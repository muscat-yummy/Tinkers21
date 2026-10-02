package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.Optional;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.EventPriority;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Capability to allow an entity to store modifiers, used on projectiles fired from modifiable items */
public class EntityModifierCapability {
  /** Default instance to use with orElse */
  public static final EntityModifiers EMPTY = new EntityModifiers() {
    @Override
    public ModifierNBT getModifiers() {
      return ModifierNBT.EMPTY;
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {}

    @Override
    public void addModifiers(ModifierNBT nbt) {}
  };

  private EntityModifierCapability() {}

  /* Static helpers */

  /** List of predicates to check if the entity supports this capability */
  private static final List<Predicate<Entity>> ENTITY_PREDICATES = new ArrayList<>();

  /** Capability ID */
  private static final ResourceLocation ID = TConstruct.getResource("modifiers");
  /** Capability type */
  public static final EntityCapability<EntityModifiers,Void> CAPABILITY = EntityCapability.createVoid(ID, EntityModifiers.class);
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TConstruct.MOD_ID);
  private static final DeferredHolder<AttachmentType<?>,AttachmentType<Provider>> DATA =
    ATTACHMENTS.register("modifiers", () -> AttachmentType.builder(Provider::new)
      .serialize(new IAttachmentSerializer<ListTag,Provider>() {
        @Override
        public Provider read(IAttachmentHolder holder, ListTag tag, HolderLookup.Provider registries) {
          Provider provider = new Provider();
          provider.setModifiers(ModifierNBT.readFromNBT(tag.copy()));
          return provider;
        }

        @Override
        public ListTag write(Provider provider, HolderLookup.Provider registries) {
          return provider.getModifiers().serializeToNBT().copy();
        }
      }).build());

  public static Optional<EntityModifiers> getOptionalData(Entity entity) {
    return Optional.ofNullable(entity.getCapability(CAPABILITY));
  }

  /** Gets the capability for the entity or an empty instance if missing */
  public static EntityModifiers getCapability(Entity entity) {
    return getOptionalData(entity).orElse(EMPTY);
  }

  /** Gets the data or an empty instance if missing */
  public static ModifierNBT getOrEmpty(Entity entity) {
    return getOptionalData(entity).orElse(EMPTY).getModifiers();
  }

  /** Checks if the given entity supports this capability */
  public static boolean supportCapability(Entity entity) {
    for (Predicate<Entity> entityPredicate : ENTITY_PREDICATES) {
      if (entityPredicate.test(entity)) {
        return true;
      }
    }
    return false;
  }

  /** Registers a predicate of entites that need this capability */
  public static void registerEntityPredicate(Predicate<Entity> predicate) {
    ENTITY_PREDICATES.add(predicate);
  }

  /** Registers serialized projectile modifier storage and its entity lookup. */
  public static void register() {
    ATTACHMENTS.register(TConstruct.getModEventBus());
    TConstruct.getModEventBus().addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, event -> {
      for (var type : BuiltInRegistries.ENTITY_TYPE) {
        event.registerEntity(CAPABILITY, type, (entity, context) ->
          supportCapability(entity) ? entity.getData(DATA) : null);
      }
    });
  }

  private static class Provider implements EntityModifiers {
    private ModifierNBT modifiers = ModifierNBT.EMPTY;

    @Override
    public ModifierNBT getModifiers() {
      return modifiers;
    }

    @Override
    public void setModifiers(ModifierNBT modifiers) {
      this.modifiers = java.util.Objects.requireNonNull(modifiers);
    }
  }

  /** Interface for callers to use */
  public interface EntityModifiers {
    /** Gets the stored modifiers */
    ModifierNBT getModifiers();

    /** Sets the stored modifiers */
    void setModifiers(ModifierNBT nbt);

    /** Adds additional modifiers to the stored modifiers */
    default void addModifiers(ModifierNBT nbt) {
      ModifierNBT existing = getModifiers();
      if (existing.isEmpty()) {
        setModifiers(nbt);
      } else {
        setModifiers(ModifierNBT.builder().add(existing).add(nbt).build());
      }
    }
  }
}
