package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Retains loadable ingredient JSON and stream formats behind a NeoForge IngredientType. */
public record LoadableIngredientSerializer<T extends ICustomIngredient>(RecordLoadable<T> loadable) {
  public T parse(FriendlyByteBuf buffer) {
    return loadable.decode(buffer);
  }

  public T parse(JsonObject json) {
    return loadable.deserialize(json);
  }

  public void write(FriendlyByteBuf buffer, T ingredient) {
    loadable.encode(buffer, ingredient);
  }

  /** Supply the JSON field names so this map codec also supports compressed operations. */
  public MapCodec<T> mapCodec(String... fieldNames) {
    String[] keys = fieldNames.clone();
    return new MapCodec<>() {
      @Override
      public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
        try {
          JsonObject json = new JsonObject();
          input.entries().forEach(entry -> json.add(
            ops.getStringValue(entry.getFirst()).getOrThrow(),
            ops.convertTo(JsonOps.INSTANCE, entry.getSecond())));
          return DataResult.success(loadable.deserialize(json));
        } catch (RuntimeException exception) {
          return DataResult.error(() -> "Invalid ingredient: " + exception.getMessage());
        }
      }

      @Override
      public <O> RecordBuilder<O> encode(T ingredient, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        try {
          loadable.serialize(ingredient).getAsJsonObject().entrySet().forEach(entry ->
            prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue())));
          return prefix;
        } catch (RuntimeException exception) {
          return prefix.withErrorsFrom(DataResult.error(() -> "Invalid ingredient: " + exception.getMessage()));
        }
      }

      @Override
      public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Arrays.stream(keys).map(ops::createString);
      }
    };
  }

  public IngredientType<T> type(String... fieldNames) {
    StreamCodec<RegistryFriendlyByteBuf,T> stream = StreamCodec.of(this::write, this::parse);
    return new IngredientType<>(mapCodec(fieldNames), stream);
  }

  public JsonObject serialize(T ingredient) {
    JsonObject json = new JsonObject();
    json.addProperty("type", Objects.requireNonNull(NeoForgeRegistries.INGREDIENT_TYPES.getKey(ingredient.getType())).toString());
    loadable.serialize(ingredient, json);
    return json;
  }
}
