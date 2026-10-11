package slimeknights.tconstruct.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.library.tools.helper.ModifierLootingHandler;

/** Retains native loot arithmetic and randomness while providing TiC's contextual looting level. */
@Mixin(EnchantedCountIncreaseFunction.class)
public abstract class LootCountMixin {
  @Shadow @Final private Holder<Enchantment> enchantment;

  @Redirect(method = "run", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/level/storage/loot/LootContext;getParamOrNull(Lnet/minecraft/world/level/storage/loot/parameters/LootContextParam;)Ljava/lang/Object;"))
  private <T> T tconstruct$queryEntity(LootContext context, LootContextParam<T> param) {
    return ModifierLootingHandler.getLootingQueryEntity(context, param, enchantment);
  }

  @Redirect(method = "run", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I"))
  private int tconstruct$looting(Holder<Enchantment> enchantment, LivingEntity entity, ItemStack stack, LootContext context) {
    return ModifierLootingHandler.getEnchantmentLooting(enchantment, entity, context);
  }
}
