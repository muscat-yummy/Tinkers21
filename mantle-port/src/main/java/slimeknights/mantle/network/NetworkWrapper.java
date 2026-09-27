package slimeknights.mantle.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.ISimplePacket;

/**
 * Bridges the existing Mantle packet format to NeoForge play payloads.
 * Packet IDs and codecs are collected during mod construction, before the payload event.
 */
public class NetworkWrapper {
  private final ResourceLocation channelName;
  private final String version;
  private final List<Registration<?>> registrations = new ArrayList<>();
  private final Map<Class<?>, Registration<?>> byClass = new HashMap<>();
  private final CustomPacketPayload.Type<LegacyPayload> toClient;
  private final CustomPacketPayload.Type<LegacyPayload> toServer;
  public final LegacyChannel network = new LegacyChannel();

  @Deprecated
  public NetworkWrapper(ResourceLocation channelName) {
    this(channelName, "1");
  }

  public NetworkWrapper(ResourceLocation channelName, String version) {
    this.channelName = Objects.requireNonNull(channelName);
    this.version = Objects.requireNonNull(version);
    this.toClient = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(channelName.getNamespace(), channelName.getPath() + "/to_client"));
    this.toServer = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(channelName.getNamespace(), channelName.getPath() + "/to_server"));
  }

  public <MSG extends ISimplePacket> void registerPacket(
      Class<MSG> clazz, Function<FriendlyByteBuf, MSG> decoder, @Nullable NetworkDirection direction) {
    registerPacket(clazz, ISimplePacket::encode, decoder, ISimplePacket::handle, direction);
  }

  public <MSG> void registerPacket(
      Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder,
      Function<FriendlyByteBuf, MSG> decoder,
      BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer,
      @Nullable NetworkDirection direction) {
    registerPacketNoLogger(clazz, (message, buffer) -> {
      try {
        encoder.accept(message, buffer);
      } catch (RuntimeException exception) {
        Mantle.logger.error("Failed to encode packet {}", clazz.getName(), exception);
        throw exception;
      }
    }, buffer -> {
      try {
        return decoder.apply(buffer);
      } catch (RuntimeException exception) {
        Mantle.logger.error("Failed to decode packet {}", clazz.getName(), exception);
        throw exception;
      }
    }, consumer, direction);
  }

  public <MSG> void registerPacketNoLogger(
      Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder,
      Function<FriendlyByteBuf, MSG> decoder,
      BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer,
      @Nullable NetworkDirection direction) {
    if (byClass.containsKey(clazz)) {
      throw new IllegalArgumentException("Packet registered twice: " + clazz.getName());
    }
    Registration<MSG> registration =
        new Registration<>(registrations.size(), clazz, encoder, decoder, consumer, direction);
    registrations.add(registration);
    byClass.put(clazz, registration);
  }

  /** Called on the mod event bus after all packet classes have registered. */
  public void registerPayloads(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(version);
    registrar.playToClient(toClient, codec(toClient, NetworkDirection.PLAY_TO_CLIENT),
        (payload, context) -> dispatch(payload, context));
    registrar.playToServer(toServer, codec(toServer, NetworkDirection.PLAY_TO_SERVER),
        (payload, context) -> dispatch(payload, context));
  }

  private StreamCodec<RegistryFriendlyByteBuf, LegacyPayload> codec(
      CustomPacketPayload.Type<LegacyPayload> type, NetworkDirection direction) {
    return StreamCodec.of((buffer, payload) -> {
      Registration<?> registration = registration(payload.id);
      checkDirection(registration, direction);
      buffer.writeVarInt(payload.id);
      registration.encode(payload.message, buffer);
    }, buffer -> {
      int id = buffer.readVarInt();
      Registration<?> registration = registration(id);
      checkDirection(registration, direction);
      return new LegacyPayload(type, id, registration.decoder.apply(buffer));
    });
  }

  private Registration<?> registration(int id) {
    if (id < 0 || id >= registrations.size()) {
      throw new IllegalArgumentException("Unknown packet ID " + id + " on " + channelName);
    }
    return registrations.get(id);
  }

  private static void checkDirection(Registration<?> registration, NetworkDirection direction) {
    if (registration.direction != null && registration.direction != direction) {
      throw new IllegalArgumentException("Wrong packet direction for " + registration.clazz.getName());
    }
  }

  private void dispatch(LegacyPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
    Registration<?> registration = registration(payload.id);
    registration.handle(payload.message, () -> new NetworkEvent.Context(context));
  }

  private LegacyPayload payloadFor(Object message, NetworkDirection direction) {
    Registration<?> registration = byClass.get(message.getClass());
    if (registration == null) {
      throw new IllegalArgumentException("Unregistered packet " + message.getClass().getName());
    }
    checkDirection(registration, direction);
    return new LegacyPayload(direction == NetworkDirection.PLAY_TO_CLIENT ? toClient : toServer,
        registration.id, message);
  }

  public void sendToServer(Object message) {
    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
        payloadFor(message, NetworkDirection.PLAY_TO_SERVER));
  }

  public void send(PacketDistributor.PacketTarget target, Object message) {
    target.send(payloadFor(message, NetworkDirection.PLAY_TO_CLIENT));
  }

  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(packet);
    }
  }

  public void sendTo(Object message, Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      sendTo(message, serverPlayer);
    }
  }

  public void sendTo(Object message, ServerPlayer player) {
    if (!(player instanceof FakePlayer)) {
      net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
          player, payloadFor(message, NetworkDirection.PLAY_TO_CLIENT));
    }
  }

  public void sendToClientsAround(Object message, ServerLevel level, BlockPos position) {
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
        level, level.getChunkAt(position).getPos(),
        payloadFor(message, NetworkDirection.PLAY_TO_CLIENT));
  }

  public void sendToTrackingAndSelf(Object message, Entity entity) {
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(
        entity, payloadFor(message, NetworkDirection.PLAY_TO_CLIENT));
  }

  public void sendToTracking(Object message, Entity entity) {
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(
        entity, payloadFor(message, NetworkDirection.PLAY_TO_CLIENT));
  }

  public final class LegacyChannel {
    public void send(PacketDistributor.PacketTarget target, Object message) {
      NetworkWrapper.this.send(target, message);
    }

    public void sendToServer(Object message) {
      NetworkWrapper.this.sendToServer(message);
    }

    public void sendTo(Object message, Connection connection, NetworkDirection direction) {
      if (direction != NetworkDirection.PLAY_TO_CLIENT) {
        throw new IllegalArgumentException("Use sendToServer for serverbound packets");
      }
      connection.send(new ClientboundCustomPayloadPacket(payloadFor(message, direction)));
    }
  }

  private record LegacyPayload(CustomPacketPayload.Type<LegacyPayload> packetType, int id, Object message)
      implements CustomPacketPayload {
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return packetType;
    }
  }

  private record Registration<MSG>(int id, Class<MSG> clazz,
      BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
      BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer,
      @Nullable NetworkDirection direction) {
    void encode(Object message, FriendlyByteBuf buffer) {
      encoder.accept(clazz.cast(message), buffer);
    }

    void handle(Object message, Supplier<NetworkEvent.Context> context) {
      consumer.accept(clazz.cast(message), context);
    }
  }
}
