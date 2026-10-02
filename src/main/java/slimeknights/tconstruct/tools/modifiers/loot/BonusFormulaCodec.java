package slimeknights.tconstruct.tools.modifiers.loot;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;

import java.util.stream.Stream;

/** Vanilla formula codecs with the legacy allowance for absent parameters. */
public final class BonusFormulaCodec {
  private BonusFormulaCodec() {}

  public static final MapCodec<ApplyBonusCount.Formula> CODEC = new MapCodec<>() {
    @Override
    public <T> DataResult<ApplyBonusCount.Formula> decode(DynamicOps<T> ops, MapLike<T> input) {
      if (input.get("parameters") != null) {
        return ApplyBonusCount.FORMULA_CODEC.decode(ops, input);
      }
      T parameters = ops.emptyMap();
      return ApplyBonusCount.FORMULA_CODEC.decode(ops, new MapLike<>() {
        @Override
        public T get(T key) {
          return ops.getStringValue(key).result().filter("parameters"::equals).isPresent() ? parameters : input.get(key);
        }

        @Override
        public T get(String key) {
          return key.equals("parameters") ? parameters : input.get(key);
        }

        @Override
        public Stream<Pair<T,T>> entries() {
          return Stream.concat(input.entries(), Stream.of(Pair.of(ops.createString("parameters"), parameters)));
        }
      });
    }

    @Override
    public <T> RecordBuilder<T> encode(ApplyBonusCount.Formula value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      return ApplyBonusCount.FORMULA_CODEC.encode(value, ops, prefix);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
      return ApplyBonusCount.FORMULA_CODEC.keys(ops);
    }
  };
}
