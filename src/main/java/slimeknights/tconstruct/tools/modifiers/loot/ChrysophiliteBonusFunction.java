package slimeknights.tconstruct.tools.modifiers.loot;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.BinomialWithBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.Formula;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.OreDrops;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.UniformBonusCount;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import slimeknights.tconstruct.shared.TinkerAttributes;

import java.util.Set;

/** Loot modifier to boost drops based on teh chrysophilite amount */
public class ChrysophiliteBonusFunction extends LootItemConditionalFunction {
  public static final MapCodec<ChrysophiliteBonusFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
    commonFields(instance).and(instance.group(
      BonusFormulaCodec.CODEC.forGetter(value -> value.formula),
      Codec.BOOL.optionalFieldOf("include_base", true).forGetter(value -> value.includeBase)
    )).apply(instance, ChrysophiliteBonusFunction::new));
  public static final LootItemFunctionType<ChrysophiliteBonusFunction> TYPE = new LootItemFunctionType<>(CODEC);


  /** Formula to apply */
  private final Formula formula;
  /** If true, the includes the helmet in the level, if false level is just gold pieces */
  private final boolean includeBase;
  protected ChrysophiliteBonusFunction(List<LootItemCondition> conditions, Formula formula, boolean includeBase) {
    super(conditions);
    this.formula = formula;
    this.includeBase = includeBase;
  }

  /** Creates a generic builder */
  public static Builder<?> builder(Formula formula, boolean includeBase) {
    return simpleBuilder(conditions -> new ChrysophiliteBonusFunction(conditions, formula, includeBase));
  }

  /** Creates a builder for the binomial with bonus formula */
  public static Builder<?> binomialWithBonusCount(float probability, int extra, boolean includeBase) {
    return builder(new BinomialWithBonusCount(extra, probability), includeBase);
  }

  /** Creates a builder for the ore drops formula */
  public static Builder<?> oreDrops(boolean includeBase) {
    return builder(new OreDrops(), includeBase);
  }

  /** Creates a builder for the uniform bonus count */
  public static Builder<?> uniformBonusCount(int bonusMultiplier, boolean includeBase) {
    return builder(new UniformBonusCount(bonusMultiplier), includeBase);
  }

  @Override
  protected ItemStack run(ItemStack stack, LootContext context) {
    if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity entity) {
      int level = (int) entity.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(TinkerAttributes.CHRYSOPHILITE.get()));
      if (!includeBase) {
        level--;
      }
      if (level > 0) {
        stack.setCount(formula.calculateNewCount(context.getRandom(), stack.getCount(), level));
      }
    }
    return stack;
  }

  @Override
  public Set<LootContextParam<?>> getReferencedContextParams() {
    return ImmutableSet.of(LootContextParams.THIS_ENTITY);
  }

  @Override
  public LootItemFunctionType<ChrysophiliteBonusFunction> getType() {
    return TYPE;
  }

}
