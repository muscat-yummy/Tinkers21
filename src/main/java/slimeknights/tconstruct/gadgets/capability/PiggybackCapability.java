package slimeknights.tconstruct.gadgets.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.TConstruct;

import java.util.Optional;

/** Transient player passenger tracking exposed through a native entity capability. */
public class PiggybackCapability {
  private static final ResourceLocation ID = TConstruct.getResource("piggyback");
  public static final EntityCapability<PiggybackHandler,Void> PIGGYBACK =
    EntityCapability.createVoid(ID, PiggybackHandler.class);
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);
  private static final DeferredHolder<AttachmentType<?>,AttachmentType<PiggybackHandler>> DATA =
    ATTACHMENTS.register("piggyback", () -> AttachmentType.builder(owner -> {
      if (!(owner instanceof Player player)) {
        throw new IllegalArgumentException("Piggyback attachment requires a player");
      }
      return new PiggybackHandler(player);
    }).build());

  private PiggybackCapability() {}

  /** No serialization or death copying: the world owns passengers and logout dismounts them. */
  public static void register() {
    ATTACHMENTS.register(TConstruct.getModEventBus());
    TConstruct.getModEventBus().addListener(PiggybackCapability::register);
  }

  private static void register(RegisterCapabilitiesEvent event) {
    event.registerEntity(PIGGYBACK, EntityType.PLAYER, (player, context) -> player.getData(DATA));
  }

  public static Optional<PiggybackHandler> getOptionalData(Entity entity) {
    return Optional.ofNullable(entity.getCapability(PIGGYBACK));
  }
}
