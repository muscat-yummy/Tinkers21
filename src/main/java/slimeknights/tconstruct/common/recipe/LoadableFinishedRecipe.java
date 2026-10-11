package slimeknights.tconstruct.common.recipe;

import com.google.gson.JsonObject;
import slimeknights.mantle.recipe.data.FinishedRecipe;
import slimeknights.mantle.recipe.data.RecipeConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Simple recipe for serializing a recipe with a loadable.
 * TODO: move to Mantle
 */
public record LoadableFinishedRecipe<T extends Recipe<?>>(ResourceLocation id, RecordLoadable<T> loader, T recipe) implements FinishedRecipe {
  @Override
  public ResourceLocation getId() {
    return id;
  }

  @Override
  public void serializeRecipeData(JsonObject json) {
    loader.serialize(recipe, json);
  }

  @Override
  public RecipeSerializer<?> getType() {
    return recipe.getSerializer();
  }

  @Nullable
  @Override
  public JsonObject serializeAdvancement() {
    return null;
  }

  @Nullable
  @Override
  public ResourceLocation getAdvancementId() {
    return null;
  }

  /** One-liner to accept a new recipe */
  public static <T extends Recipe<?>> void save(RecipeConsumer consumer, ResourceLocation id, RecordLoadable<T> loader, T recipe) {
    consumer.accept(new LoadableFinishedRecipe<>(id, loader, recipe));
  }
}
