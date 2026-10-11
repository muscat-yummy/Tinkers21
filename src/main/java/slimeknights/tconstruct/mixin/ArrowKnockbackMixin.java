package slimeknights.tconstruct.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.modifiers.entity.ProjectileWithKnockback;

/** Adds TiC projectile knockback to native, world-resolved enchantment knockback. */
@Mixin(AbstractArrow.class)
public abstract class ArrowKnockbackMixin implements ProjectileWithKnockback {
  @Unique
  private float tconstruct$knockback;

  @Override
  public void addKnockback(float amount) {
    tconstruct$knockback += amount;
  }

  @ModifyVariable(method = "doKnockback", at = @At("STORE"), ordinal = 0)
  private double tconstruct$addKnockback(double nativeKnockback) {
    return nativeKnockback + tconstruct$knockback;
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void tconstruct$saveKnockback(CompoundTag tag, CallbackInfo ci) {
    if (tconstruct$knockback != 0) {
      tag.putFloat("tconstruct:knockback", tconstruct$knockback);
    }
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void tconstruct$loadKnockback(CompoundTag tag, CallbackInfo ci) {
    tconstruct$knockback = tag.getFloat("tconstruct:knockback");
  }
}
