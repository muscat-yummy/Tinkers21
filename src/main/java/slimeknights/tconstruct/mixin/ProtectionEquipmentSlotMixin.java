package slimeknights.tconstruct.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.utils.ProtectionSlotPolicy;

/** Extend only the configured real enchantments while native effects iterate equipment. */
@Mixin(EnchantmentHelper.class)
public abstract class ProtectionEquipmentSlotMixin {
  @WrapOperation(
    method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;matchingSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z"))
  private static boolean tconstruct$matchProtectionSlot(Enchantment enchantment, EquipmentSlot slot, Operation<Boolean> original, @Local Holder<Enchantment> holder) {
    return original.call(enchantment, slot) || ProtectionSlotPolicy.extendsSlots(holder);
  }
}
