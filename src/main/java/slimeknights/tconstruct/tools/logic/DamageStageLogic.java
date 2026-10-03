package slimeknights.tconstruct.tools.logic;

import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction;

/** TiC damage hooks placed at the actual native armor, enchantment and absorption stages. */
public final class DamageStageLogic {
  private DamageStageLogic() {}

  public static final class Protection {
    private final float value;
    private final float cap;
    private boolean applied;
    private Protection(float value, float cap) {
      this.value = value;
      this.cap = cap;
    }
  }

  /** The existing TiC protection formula, including negative protection and custom caps. */
  public static float afterProtection(float damage, float protection, float cap) {
    if (protection == 0 || damage <= 0) return damage;
    return damage * (1 - Mth.clamp(protection, -20, cap) / 25);
  }

  public static Protection prepareProtection(DamageContainer container, float vanillaProtection, float protection, float cap) {
    Protection plan = new Protection(protection, cap);
    container.addModifier(Reduction.ENCHANTMENTS, (damage, reduction) -> {
      plan.applied = true;
      float before = damage.getNewDamage();
      float vanillaDamage = before * (1 - Mth.clamp(vanillaProtection, 0, 20) / 25);
      float desiredDamage = Math.max(0, afterProtection(before, protection, cap));
      // Preserve additive changes made by earlier native reduction callbacks.
      return reduction + vanillaDamage - desiredDamage;
    });
    return plan;
  }

  /** Native callbacks do not run when vanilla protection is zero or bypassed. */
  public static void finishProtection(DamageContainer container, Protection plan) {
    if (!plan.applied) {
      container.setNewDamage(Math.max(0, afterProtection(container.getNewDamage(), plan.value, plan.cap)));
      plan.applied = true;
    } else if (container.getNewDamage() < 0) {
      container.setNewDamage(0);
    }
  }

  public static void afterAbsorption(DamageContainer container, DoubleUnaryOperator modifier) {
    container.addModifier(Reduction.ABSORPTION, (damage, absorbed) -> {
      float current = Math.max(0, damage.getNewDamage() - absorbed);
      float result = Math.max(0, (float) modifier.applyAsDouble(current));
      // Changing the reduction would also consume extra absorption hearts. Keep the
      // native absorbed amount and adjust only the damage that remains after it.
      damage.setNewDamage(result + absorbed);
      return absorbed;
    });
  }
}
