package slimeknights.tconstruct.library.json.variable.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/** Gets the level of the mob effect on an entity */
public record EntityEffectLevelVariable(Holder<MobEffect> effect) implements EntityVariable {
  public static final RecordLoadable<EntityEffectLevelVariable> LOADER = RecordLoadable.create(Loadables.MOB_EFFECT.requiredField("effect", variable -> variable.effect().value()), EntityEffectLevelVariable::new);

  /** Compatibility constructor for callers holding a registered effect value. */
  public EntityEffectLevelVariable(MobEffect effect) {
    this(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
  }

  @Override
  public float getValue(LivingEntity entity) {
    MobEffectInstance instance = entity.getEffect(effect);
    if (instance != null) {
      return instance.getAmplifier() + 1;
    }
    return 0;
  }

  @Override
  public RecordLoadable<EntityEffectLevelVariable> getLoader() {
    return LOADER;
  }
}
