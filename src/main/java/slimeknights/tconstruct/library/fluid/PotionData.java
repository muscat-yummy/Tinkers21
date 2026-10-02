package slimeknights.tconstruct.library.fluid;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Shared native potion contents, including read support for pre-component fluid metadata. */
public final class PotionData {
  private PotionData() {}

  public static Optional<Holder<Potion>> potion(String id) {
    ResourceLocation location = ResourceLocation.tryParse(id);
    return location == null ? Optional.empty()
      : BuiltInRegistries.POTION.getHolder(ResourceKey.create(Registries.POTION, location)).map(holder -> holder);
  }

  public static PotionContents get(DataComponentHolder stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    CompoundTag legacy = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    // Vanilla potion items have an empty default component, which must not hide old saved metadata.
    if (contents != null && !contents.equals(PotionContents.EMPTY)) {
      return contents;
    }
    if (legacy.contains("Potion") || legacy.contains("CustomPotionEffects") || legacy.contains("CustomPotionColor")) {
      return readLegacy(legacy);
    }
    return contents == null ? PotionContents.EMPTY : contents;
  }

  public static PotionContents readLegacy(CompoundTag tag) {
    List<MobEffectInstance> effects = new ArrayList<>();
    ListTag saved = tag.getList("CustomPotionEffects", Tag.TAG_COMPOUND);
    for (int i = 0; i < saved.size(); i++) {
      MobEffectInstance effect = readEffect(saved.getCompound(i));
      if (effect != null) {
        effects.add(effect);
      }
    }
    Optional<Integer> color = tag.contains("CustomPotionColor", Tag.TAG_ANY_NUMERIC)
      ? Optional.of(tag.getInt("CustomPotionColor")) : Optional.empty();
    return new PotionContents(potion(tag.getString("Potion")), color, List.copyOf(effects));
  }

  @Nullable
  private static MobEffectInstance readEffect(CompoundTag tag) {
    // Also accept effects already saved with the native lowercase schema.
    if (tag.contains("id", Tag.TAG_STRING)) {
      return MobEffectInstance.load(tag);
    }
    ResourceLocation location = ResourceLocation.tryParse(tag.getString("Id"));
    if (location == null) {
      return null;
    }
    return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, location))
      .map(effect -> new MobEffectInstance(effect, tag.getInt("Duration"), Math.max(0, tag.getByte("Amplifier")),
        tag.getBoolean("Ambient"), !tag.contains("ShowParticles") || tag.getBoolean("ShowParticles"),
        tag.contains("ShowIcon") ? tag.getBoolean("ShowIcon") : !tag.contains("ShowParticles") || tag.getBoolean("ShowParticles"),
        tag.contains("HiddenEffect", Tag.TAG_COMPOUND) ? readEffect(tag.getCompound("HiddenEffect")) : null))
      .orElse(null);
  }

  /** Legacy recipe predicates retain their established Potion/custom-effect fields. */
  public static CompoundTag predicateTag(FluidStack fluid) {
    CompoundTag tag = fluid.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    PotionContents contents = get(fluid);
    tag.remove("Potion");
    tag.remove("CustomPotionEffects");
    tag.remove("CustomPotionColor");
    contents.potion().flatMap(Holder::unwrapKey).ifPresent(key -> tag.putString("Potion", key.location().toString()));
    contents.customColor().ifPresent(color -> tag.putInt("CustomPotionColor", color));
    if (!contents.customEffects().isEmpty()) {
      ListTag effects = new ListTag();
      for (MobEffectInstance effect : contents.customEffects()) {
        effects.add(writeEffect(effect));
      }
      tag.put("CustomPotionEffects", effects);
    }
    return tag;
  }

  private static CompoundTag writeEffect(MobEffectInstance effect) {
    CompoundTag tag = new CompoundTag();
    effect.getEffect().unwrapKey().ifPresent(key -> tag.putString("Id", key.location().toString()));
    tag.putInt("Duration", effect.getDuration());
    tag.putByte("Amplifier", (byte) effect.getAmplifier());
    tag.putBoolean("Ambient", effect.isAmbient());
    tag.putBoolean("ShowParticles", effect.isVisible());
    tag.putBoolean("ShowIcon", effect.showIcon());
    return tag;
  }

  public static FluidStack fluid(ItemStack item, Fluid type, int amount) {
    FluidStack fluid = new FluidStack(type, amount);
    fluid.applyComponents(item.getComponentsPatch());
    fluid.set(DataComponents.POTION_CONTENTS, get(item));
    clearLegacy(fluid);
    return fluid;
  }

  public static ItemStack fill(ItemStack item, FluidStack fluid) {
    item.applyComponents(fluid.getComponentsPatch());
    item.set(DataComponents.POTION_CONTENTS, get(fluid));
    clearLegacy(item);
    return item;
  }

  private static void clearLegacy(net.neoforged.neoforge.common.MutableDataComponentHolder stack) {
    CompoundTag legacy = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    legacy.remove("Potion");
    legacy.remove("CustomPotionEffects");
    legacy.remove("CustomPotionColor");
    if (legacy.isEmpty()) {
      stack.remove(DataComponents.CUSTOM_DATA);
    } else {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));
    }
  }
}
