package slimeknights.tconstruct.port;

import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import slimeknights.tconstruct.tools.logic.CriticalHitLogic;

/** Tests actual event state; entity objects are unused by the boost policy. */
public final class CriticalHitSmoke {
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    var normal = new CriticalHitEvent(null, null, 1f, false);
    CriticalHitLogic.apply(normal, false, .25);
    check(!normal.isCriticalHit() && normal.getDamageMultiplier() == 1f, "Grounded attack became critical");
    var airborne = new CriticalHitEvent(null, null, 1f, false);
    CriticalHitLogic.apply(airborne, true, .25);
    check(airborne.isCriticalHit() && airborne.getDamageMultiplier() == 1.25f, "Forced critical changed additive multiplier semantics");
    var vanilla = new CriticalHitEvent(null, null, 1.5f, true);
    CriticalHitLogic.apply(vanilla, false, .25);
    check(vanilla.isCriticalHit() && vanilla.getDamageMultiplier() == 1.75f, "Vanilla critical boost");
    var suppressed = new CriticalHitEvent(null, null, 1.5f, true);
    suppressed.setCriticalHit(false);
    CriticalHitLogic.apply(suppressed, true, .25);
    check(!suppressed.isCriticalHit() && suppressed.getDamageMultiplier() == 1.5f, "Earlier suppression was overridden");
    var addon = new CriticalHitEvent(null, null, 2f, false);
    addon.setCriticalHit(true);
    CriticalHitLogic.apply(addon, false, .25);
    check(addon.getDamageMultiplier() == 2.25f, "Earlier addon multiplier was replaced");
    var zero = new CriticalHitEvent(null, null, 1f, false);
    CriticalHitLogic.apply(zero, true, 0);
    check(!zero.isCriticalHit(), "Zero boost forced critical");
    System.out.println("Native critical event boost, airborne policy and earlier handler state: PASS");
  }
}
