package slimeknights.tconstruct.port;

import java.util.concurrent.atomic.AtomicInteger;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction;
import slimeknights.tconstruct.tools.logic.DamageStageLogic;

/** Exercises production hook placement using the real native DamageContainer reduction sequence. */
public final class DamageStageSmoke {
  private static DamageContainer damage(float amount) { return new DamageContainer(null, amount); }
  private static void near(float actual, float expected, String message) {
    if (Math.abs(actual - expected) > 0.0001F) throw new AssertionError(message + ": " + actual + " != " + expected);
  }
  public static void main(String[] args) {
    DamageContainer normal = damage(20);
    var plan = DamageStageLogic.prepareProtection(normal, 5, 10, 20);
    DamageStageLogic.afterAbsorption(normal, remaining -> remaining * 0.5);
    normal.setReduction(Reduction.ARMOR, 4);
    normal.setReduction(Reduction.MOB_EFFECTS, 4);
    normal.setReduction(Reduction.ENCHANTMENTS, 2.4F);
    DamageStageLogic.finishProtection(normal, plan);
    near(normal.getNewDamage(), 7.2F, "Protection follows armor and resistance");
    DamageStageLogic.finishProtection(normal, plan);
    near(normal.getNewDamage(), 7.2F, "Protection applies once");
    normal.setReduction(Reduction.ABSORPTION, 3);
    near(normal.getNewDamage(), 2.1F, "Final hook receives damage after absorption");
    near(normal.getReduction(Reduction.ABSORPTION), 3, "Final hook must not consume extra absorption");
    near(normal.getReduction(Reduction.ARMOR), 4, "Armor reduction remains native");

    DamageContainer noEnchantments = damage(20);
    var zero = DamageStageLogic.prepareProtection(noEnchantments, 0, 10, 20);
    noEnchantments.setReduction(Reduction.ARMOR, 4);
    noEnchantments.setReduction(Reduction.MOB_EFFECTS, 4);
    // Vanilla skips the enchantment stage entirely when its protection is zero.
    DamageStageLogic.finishProtection(noEnchantments, zero);
    near(noEnchantments.getNewDamage(), 7.2F, "Protection works without a vanilla enchantment callback");

    DamageContainer addon = damage(20);
    addon.addModifier(Reduction.ENCHANTMENTS, (container, reduction) -> reduction + 2);
    var custom = DamageStageLogic.prepareProtection(addon, 5, 10, 20);
    addon.setReduction(Reduction.ENCHANTMENTS, 4);
    DamageStageLogic.finishProtection(addon, custom);
    near(addon.getNewDamage(), 10, "Earlier addon reduction is preserved");

    DamageContainer cursed = damage(20);
    var negative = DamageStageLogic.prepareProtection(cursed, 0, -5, 20);
    DamageStageLogic.finishProtection(cursed, negative);
    near(cursed.getNewDamage(), 24, "Negative protection increases damage");

    DamageContainer capped = damage(20);
    var highCap = DamageStageLogic.prepareProtection(capped, 0, 40, 30);
    DamageStageLogic.finishProtection(capped, highCap);
    near(capped.getNewDamage(), 0, "Custom high protection cap cannot heal");

    DamageContainer absorbed = damage(8);
    AtomicInteger hits = new AtomicInteger();
    DamageStageLogic.afterAbsorption(absorbed, remaining -> {
      near((float) remaining, 0, "Fully absorbed hook input");
      hits.incrementAndGet();
      return remaining;
    });
    absorbed.setReduction(Reduction.ABSORPTION, 8);
    near(absorbed.getNewDamage(), 0, "Fully absorbed health damage");
    near(absorbed.getReduction(Reduction.ABSORPTION), 8, "Fully absorbed hearts");
    if (hits.get() != 1) throw new AssertionError("Post-hit hook must run for an absorbed hit");

    DamageContainer canceled = damage(10);
    DamageStageLogic.afterAbsorption(canceled, remaining -> -1);
    canceled.setReduction(Reduction.ABSORPTION, 3);
    near(canceled.getNewDamage(), 0, "Final damage cancellation");
    near(canceled.getReduction(Reduction.ABSORPTION), 3, "Cancellation keeps native absorption consumption");

    DamageContainer noAbsorption = damage(7);
    DamageStageLogic.afterAbsorption(noAbsorption, remaining -> remaining + 2);
    noAbsorption.setReduction(Reduction.ABSORPTION, 0);
    near(noAbsorption.getNewDamage(), 9, "Final damage hook also runs without absorption hearts");

    System.out.println("Native damage stages, zero/bypassed enchantment fallback, addon reductions, custom caps and exact absorption consumption: PASS");
  }
}
