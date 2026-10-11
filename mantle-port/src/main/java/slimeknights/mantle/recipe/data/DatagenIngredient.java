package slimeknights.mantle.recipe.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.Mantle;
import java.util.stream.Stream;

/** Encode-only marker for optional-mod ingredient IDs, removed before writing finished data. */
public record DatagenIngredient(JsonElement payload) implements ICustomIngredient {
  public static final ResourceLocation ID = Mantle.getResource("datagen_ingredient");
  public static final MapCodec<DatagenIngredient> CODEC = new MapCodec<>() {
    @Override public <T> DataResult<DatagenIngredient> decode(DynamicOps<T> ops, MapLike<T> input) {
      return DataResult.error(() -> "Datagen ingredient marker must be normalized before loading");
    }
    @Override public <T> RecordBuilder<T> encode(DatagenIngredient value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      return prefix.add("ingredient", JsonOps.INSTANCE.convertTo(ops, value.payload));
    }
    @Override public <T> Stream<T> keys(DynamicOps<T> ops) { return Stream.of(ops.createString("ingredient")); }
  };
  public static final IngredientType<DatagenIngredient> TYPE = new IngredientType<>(CODEC);
  public DatagenIngredient { payload = payload.deepCopy(); }
  @Override public boolean test(ItemStack stack) {
    throw new UnsupportedOperationException("Datagen ingredient cannot match runtime stacks");
  }
  @Override public Stream<ItemStack> getItems() {
    throw new UnsupportedOperationException("Optional-mod IDs have no datagen item instances");
  }
  @Override public boolean isSimple() { return false; }
  @Override public IngredientType<?> getType() { return TYPE; }

  /** Removes markers from both native-codec and custom-builder JSON. */
  public static JsonElement normalize(JsonElement json) {
    if (json.isJsonObject()) {
      JsonObject object = json.getAsJsonObject();
      if (object.has("type") && object.get("type").isJsonPrimitive() && ID.toString().equals(object.get("type").getAsString())) {
        JsonElement payload = object.get("ingredient");
        if (payload == null) { throw new IllegalStateException("Datagen ingredient has no payload"); }
        return normalize(payload);
      }
      JsonObject result = new JsonObject();
      object.entrySet().forEach(entry -> result.add(entry.getKey(), normalize(entry.getValue())));
      return result;
    }
    if (json.isJsonArray()) {
      JsonArray result = new JsonArray();
      json.getAsJsonArray().forEach(value -> result.add(normalize(value)));
      return result;
    }
    return json.deepCopy();
  }
}
