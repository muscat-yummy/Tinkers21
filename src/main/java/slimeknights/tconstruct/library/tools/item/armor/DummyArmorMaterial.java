package slimeknights.tconstruct.library.tools.item.armor;

import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import slimeknights.mantle.registration.object.IdAwareObject;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registered native armor material whose stats are supplied by the tool definition.
 * Addons must attach their namespace using {@link #register(String, IEventBus)}
 * during mod construction, before the armor material registry event.
 */
@Getter
public class DummyArmorMaterial implements IdAwareObject {
  private static final Map<String,DeferredRegister<ArmorMaterial>> REGISTERS = new HashMap<>();

  private final ResourceLocation id;
  private final SoundEvent equipSound;
  private final DeferredHolder<ArmorMaterial,ArmorMaterial> holder;

  public DummyArmorMaterial(ResourceLocation id, SoundEvent equipSound) {
    this.id = id;
    this.equipSound = equipSound;
    synchronized (REGISTERS) {
      holder = getRegister(id.getNamespace()).register(id.getPath(), () -> {
        Map<Type,Integer> defense = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
          defense.put(type, 0);
        }
        // Reference the real sound registry; the sound may bind after this material.
        return new ArmorMaterial(defense, 0,
          DeferredHolder.create(Registries.SOUND_EVENT, equipSound.getLocation()),
          () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(id)), 0, 0);
      });
    }
  }

  private static DeferredRegister<ArmorMaterial> getRegister(String namespace) {
    return REGISTERS.computeIfAbsent(namespace, name -> DeferredRegister.create(Registries.ARMOR_MATERIAL, name));
  }

  /** Attach only this mod's materials to its own event bus. */
  public static void register(String namespace, IEventBus bus) {
    synchronized (REGISTERS) {
      getRegister(namespace).register(bus);
    }
  }

  public String getName() {
    return id.toString();
  }

  /** Tool definitions own durability and all armor stats. */
  @Deprecated
  public int getDurabilityForType(Type type) {
    return 0;
  }

  @Deprecated
  public int getDefenseForType(Type type) {
    return 0;
  }

  public int getEnchantmentValue() {
    return 0;
  }

  @Deprecated
  public Ingredient getRepairIngredient() {
    return Ingredient.EMPTY;
  }

  @Deprecated
  public float getToughness() {
    return 0;
  }

  @Deprecated
  public float getKnockbackResistance() {
    return 0;
  }
}
