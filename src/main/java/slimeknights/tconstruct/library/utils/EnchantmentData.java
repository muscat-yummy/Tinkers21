package slimeknights.tconstruct.library.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Signed modifier work data with immutable native enchantment component boundaries. */
public final class EnchantmentData {
  private EnchantmentData() {}

  public static int clampLevel(int level) {
    return Mth.clamp(level, 0, Enchantment.MAX_LEVEL);
  }

  public static Map<Holder<Enchantment>,Integer> mutable(ItemEnchantments original) {
    Map<Holder<Enchantment>,Integer> result = new HashMap<>();
    original.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getIntValue()));
    return result;
  }

  /** Preserve the original tooltip policy while removing nonpositive final levels. */
  public static ItemEnchantments build(ItemEnchantments original, Map<Holder<Enchantment>,Integer> levels) {
    ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(original);
    result.removeIf(enchantment -> true);
    levels.forEach((enchantment, level) -> {
      if (level != null && level > 0) result.set(enchantment, clampLevel(level));
    });
    return result.toImmutable();
  }

  /** An absent component and an explicitly empty component have different restoration behavior. */
  public record Snapshot(@Nullable ItemEnchantments component) {
    public static Snapshot capture(ItemStack stack) {
      return new Snapshot(stack.get(DataComponents.ENCHANTMENTS));
    }

    public void restore(ItemStack stack) {
      if (component == null) stack.remove(DataComponents.ENCHANTMENTS);
      else stack.set(DataComponents.ENCHANTMENTS, component);
    }
  }
}
