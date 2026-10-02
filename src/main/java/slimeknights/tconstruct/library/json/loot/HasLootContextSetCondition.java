package slimeknights.tconstruct.library.json.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * Loot condition requiring all values in the named loot context set.
 * TODO: migrate to Mantle
 */
public record HasLootContextSetCondition(LootContextParamSet set) implements LootItemCondition {
  // Vanilla's encoder throws for an unregistered set. Report invalid mod data through DataResult.
  private static final Codec<LootContextParamSet> SET_CODEC = Codec.of(new Encoder<>() {
    @Override
    public <T> DataResult<T> encode(LootContextParamSet set, DynamicOps<T> ops, T prefix) {
      try {
        return LootContextParamSets.CODEC.encode(set, ops, prefix);
      } catch (IllegalArgumentException | NullPointerException exception) {
        return DataResult.error(() -> "Unregistered LootContextParamSet");
      }
    }
  }, LootContextParamSets.CODEC);
  public static final MapCodec<HasLootContextSetCondition> CODEC =
    SET_CODEC.fieldOf("set").xmap(HasLootContextSetCondition::new, HasLootContextSetCondition::set);
  public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

  public static Builder builder(LootContextParamSet set) {
    return new Builder(set);
  }

  @Override
  public LootItemConditionType getType() {
    return TYPE;
  }

  @Override
  public boolean test(LootContext context) {
    for (LootContextParam<?> param : set.getRequired()) {
      if (!context.hasParam(param)) {
        return false;
      }
    }
    return true;
  }

  public record Builder(LootContextParamSet set) implements LootItemCondition.Builder {
    @Override
    public LootItemCondition build() {
      return new HasLootContextSetCondition(set);
    }
  }
}
