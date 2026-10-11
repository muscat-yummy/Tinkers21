package slimeknights.mantle.network;

import java.util.function.Supplier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Bridges the old target selection API to NeoForge payload distribution. */
public final class PacketDistributor {
  private PacketDistributor() {}

  @FunctionalInterface
  public interface PacketTarget {
    void send(CustomPacketPayload payload);
  }

  @FunctionalInterface
  public interface Target<T> {
    PacketTarget with(Supplier<T> target);
  }

  public static final Target<ServerPlayer> PLAYER =
      target -> payload -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(target.get(), payload);
  public static final Target<Entity> TRACKING_ENTITY =
      target -> payload -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(target.get(), payload);
  public static final Target<Entity> TRACKING_ENTITY_AND_SELF =
      target -> payload -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(target.get(), payload);
  public static final Target<LevelChunk> TRACKING_CHUNK = target -> payload -> {
    LevelChunk chunk = target.get();
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
        (ServerLevel) chunk.getLevel(), chunk.getPos(), payload);
  };
}
