package slimeknights.tconstruct.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;

/** Preserve native crossbow criteria for arrows carrying an actual TiC crossbow weapon. */
@Mixin(AbstractArrow.class)
public abstract class CrossbowArrowMixin {
  @Inject(method = "shotFromCrossbow", at = @At("RETURN"), cancellable = true)
  private void tconstruct$recognizeCrossbow(CallbackInfoReturnable<Boolean> cir) {
    if (!cir.getReturnValue()) {
      ItemStack weapon = ((AbstractArrow) (Object) this).getWeaponItem();
      if (weapon != null && weapon.getItem() instanceof ModifiableCrossbowItem) {
        cir.setReturnValue(true);
      }
    }
  }
}
