package slimeknights.tconstruct.tools.logic;

import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/** Applies the existing critical boost through NeoForge's native event state. */
public final class CriticalHitLogic {
  private CriticalHitLogic() {}

  public static void apply(CriticalHitEvent event, boolean airborne, double boost) {
    // Honor an earlier handler that turned off an originally vanilla critical hit.
    if (boost <= 0 || (event.isVanillaCritical() && !event.isCriticalHit())) {
      return;
    }
    if (!event.isCriticalHit() && airborne) {
      event.setCriticalHit(true);
    }
    if (event.isCriticalHit()) {
      event.setDamageMultiplier((float) (event.getDamageMultiplier() + boost));
    }
  }
}
