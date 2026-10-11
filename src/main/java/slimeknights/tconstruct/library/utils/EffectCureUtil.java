package slimeknights.tconstruct.library.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;

import java.util.List;

/** Bridges item-based modifier cures to persistent, synchronized native effect cures. */
public final class EffectCureUtil {
  private static final String ITEM_PREFIX = "tconstruct:item/";
  private EffectCureUtil() {}

  public static EffectCure forItem(Item item) {
    if (item == Items.MILK_BUCKET) {
      return EffectCures.MILK;
    }
    if (item == Items.HONEY_BOTTLE) {
      return EffectCures.HONEY;
    }
    return EffectCure.get(ITEM_PREFIX + BuiltInRegistries.ITEM.getKey(item));
  }

  /** Identifies item cures separately from totem and other non-item cures. */
  public static boolean isItemCure(EffectCure cure) {
    return cure == EffectCures.MILK || cure == EffectCures.HONEY || cure.name().startsWith(ITEM_PREFIX);
  }

  public static boolean hasItemCures(MobEffectInstance instance) {
    return instance.getCures().stream().anyMatch(EffectCureUtil::isItemCure);
  }

  /** Replaces item cures while retaining native non-item cures, including totem protection. */
  public static void setItemCures(MobEffectInstance instance, List<Item> items) {
    instance.getCures().removeIf(EffectCureUtil::isItemCure);
    for (Item item : items) {
      instance.getCures().add(forItem(item));
    }
  }

  public static boolean cure(LivingEntity entity, ItemStack item) {
    return !item.isEmpty() && entity.removeEffectsCuredBy(forItem(item.getItem()));
  }
}
