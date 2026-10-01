package slimeknights.mantle.recipe.data;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import javax.annotation.Nullable;
import java.util.Objects;

/** Raw recipe output for custom builders and optional-mod IDs during datagen. */
public interface FinishedRecipe {
  void serializeRecipeData(JsonObject json);
  ResourceLocation getId();
  RecipeSerializer<?> getType();
  @Nullable ResourceLocation getAdvancementId();

  default JsonObject serializeRecipe() {
    JsonObject json = new JsonObject();
    json.addProperty("type", Objects.requireNonNull(BuiltInRegistries.RECIPE_SERIALIZER.getKey(getType())).toString());
    serializeRecipeData(json);
    return json;
  }
  default JsonObject serializeRecipe(HolderLookup.Provider registries) { return serializeRecipe(); }

  @Nullable
  default JsonObject serializeAdvancement() {
    if (getAdvancementId() != null) {
      throw new IllegalStateException("This advancement requires registry-aware serialization");
    }
    return null;
  }
  @Nullable
  default JsonObject serializeAdvancement(HolderLookup.Provider registries) { return serializeAdvancement(); }
}
