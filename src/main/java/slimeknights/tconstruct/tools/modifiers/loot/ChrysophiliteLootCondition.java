package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import slimeknights.tconstruct.shared.TinkerAttributes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Set;

/** Condition to check if the enemy has the chrysophilite modifier */
public class ChrysophiliteLootCondition implements LootItemCondition {
  public static final ChrysophiliteLootCondition INSTANCE = new ChrysophiliteLootCondition();
  public static final MapCodec<ChrysophiliteLootCondition> CODEC = MapCodec.unit(INSTANCE);
  public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

  private ChrysophiliteLootCondition() {}

  @Override
  public boolean test(LootContext context) {
    return context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity living && living.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(TinkerAttributes.CHRYSOPHILITE.get())) >= 1;
  }

  @Override
  public Set<LootContextParam<?>> getReferencedContextParams() {
    return ImmutableSet.of(LootContextParams.THIS_ENTITY);
  }

  @Override
  public LootItemConditionType getType() {
    return TYPE;
  }

}
