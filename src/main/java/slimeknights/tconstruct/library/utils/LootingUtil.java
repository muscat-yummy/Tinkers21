package slimeknights.tconstruct.library.utils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Resolves looting from the actual loot world's enchantment registry and attacking entity. */
public final class LootingUtil {
  private LootingUtil() {}

  public static int getLooting(LootContext context) {
    Entity attacker = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
    if (attacker instanceof LivingEntity living) {
      var looting = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
      return EnchantmentHelper.getEnchantmentLevel(looting, living);
    }
    return 0;
  }
}
