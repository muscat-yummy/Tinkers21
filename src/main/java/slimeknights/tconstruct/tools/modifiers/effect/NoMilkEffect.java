package slimeknights.tconstruct.tools.modifiers.effect;

import java.util.Set;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;
import slimeknights.tconstruct.common.TinkerEffect;

/** Effect that cannot be cured with milk; totems still clear it as they did in 1.20. */
public class NoMilkEffect extends TinkerEffect {
  public NoMilkEffect(MobEffectCategory typeIn, int color, boolean show) {
    super(typeIn, color, show);
  }

  @Override
  public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
    super.fillEffectCures(cures, instance);
    cures.remove(EffectCures.MILK);
  }
}
