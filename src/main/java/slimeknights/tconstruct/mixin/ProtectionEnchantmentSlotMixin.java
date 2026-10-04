package slimeknights.tconstruct.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.utils.ProtectionSlotPolicy;

/** The native level-query path must use the same configured slots as native effects. */
@Mixin(Enchantment.class)
public abstract class ProtectionEnchantmentSlotMixin {
  @WrapOperation(
    method = "getSlotItems",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;matchingSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z"))
  private boolean tconstruct$matchProtectionSlot(Enchantment enchantment, EquipmentSlot slot, Operation<Boolean> original, LivingEntity entity) {
    return original.call(enchantment, slot) || ProtectionSlotPolicy.extendsSlots(enchantment, entity);
  }
}
