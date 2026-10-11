package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.SyncPersistentDataPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import java.util.Optional;

/**
 * Persistent modifier data. Player data survives death and is synced on login,
 * respawn and dimension changes, preserving the previous capability contract.
 */
public final class PersistentDataCapability {
  private PersistentDataCapability() {}

  private static final ResourceLocation ID = TConstruct.getResource("persistent_data");
  public static final EntityCapability<ModDataNBT,Void> CAPABILITY = EntityCapability.createVoid(ID, ModDataNBT.class);
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);
  private static final DeferredHolder<AttachmentType<?>,AttachmentType<ModDataNBT>> DATA =
    ATTACHMENTS.register("persistent_data", () -> AttachmentType.builder(ModDataNBT::new)
      .serialize(new IAttachmentSerializer<CompoundTag,ModDataNBT>() {
        @Override
        public ModDataNBT read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider registries) {
          return ModDataNBT.readFromNBT(tag.copy());
        }

        @Override
        public CompoundTag write(ModDataNBT data, HolderLookup.Provider registries) {
          return data.getCopy();
        }
      })
      .copyOnDeath()
      .build());

  public static Optional<ModDataNBT> getOptionalData(Entity entity) {
    return Optional.ofNullable(entity.getCapability(CAPABILITY));
  }

  public static ModDataNBT getOrWarn(Entity entity) {
    Optional<ModDataNBT> data = getOptionalData(entity);
    if (data.isEmpty()) {
      TConstruct.LOG.warn("Missing Tinkers NBT on entity {}, this should not happen", entity.getType());
      return new ModDataNBT();
    }
    return data.get();
  }

  public static void register() {
    ATTACHMENTS.register(TConstruct.getModEventBus());
    TConstruct.getModEventBus().addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, PersistentDataCapability::register);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerRespawnEvent.class, e -> sync(e.getEntity()));
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerChangedDimensionEvent.class, e -> sync(e.getEntity()));
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedInEvent.class, e -> sync(e.getEntity()));
  }

  private static void register(RegisterCapabilitiesEvent event) {
    for (var type : BuiltInRegistries.ENTITY_TYPE) {
      event.registerEntity(CAPABILITY, type, (entity, context) ->
        entity instanceof LivingEntity || EntityModifierCapability.supportCapability(entity) ? entity.getData(DATA) : null);
    }
  }

  private static void sync(Player player) {
    getOptionalData(player).ifPresent(data ->
      TinkerNetwork.getInstance().sendTo(new SyncPersistentDataPacket(data.getCopy()), player));
  }
}
