package slimeknights.tconstruct.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.shared.effect.AntigravityEffect;

/** Clears movement history only after an effect was actually removed, respecting cancellation upstream. */
@Mixin(LivingEntity.class)
public abstract class EffectRemovalMixin {
  @Inject(method = "onEffectRemoved", at = @At("TAIL"))
  private void tconstruct$clearAntigravityHistory(MobEffectInstance instance, CallbackInfo ci) {
    if (instance.getEffect().value() instanceof AntigravityEffect effect) {
      effect.clearVelocityHistory((LivingEntity) (Object) this);
    }
  }
}
