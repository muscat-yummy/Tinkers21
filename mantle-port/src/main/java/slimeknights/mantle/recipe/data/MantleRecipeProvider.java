package slimeknights.mantle.recipe.data;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Provider for native recipes and raw optional-mod recipe data. */
public abstract class MantleRecipeProvider extends RecipeProvider {
  private final PackOutput.PathProvider recipes;
  private final PackOutput.PathProvider advancements;

  protected MantleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(output, registries);
    recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    advancements = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
  }
  protected abstract void buildRecipes(RecipeConsumer consumer);

  @Override
  protected CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider registries) {
    Set<ResourceLocation> recipeIds = new HashSet<>();
    Set<ResourceLocation> advancementIds = new HashSet<>();
    List<CompletableFuture<?>> writes = new ArrayList<>();
    RecipeConsumer consumer = new RecipeConsumer() {
      @Override public HolderLookup.Provider registryLookup() { return registries; }
      @Override public void accept(FinishedRecipe recipe) {
        if (!recipeIds.add(recipe.getId())) { throw new IllegalStateException("Duplicate recipe " + recipe.getId()); }
        JsonObject json = DatagenIngredient.normalize(recipe.serializeRecipe(registries)).getAsJsonObject();
        writes.add(DataProvider.saveStable(output, json, recipes.json(recipe.getId())));
        JsonObject advancement = recipe.serializeAdvancement(registries);
        if (advancement != null) {
          ResourceLocation id = recipe.getAdvancementId();
          if (id == null || !advancementIds.add(id)) {
            throw new IllegalStateException("Missing or duplicate advancement ID for " + recipe.getId());
          }
          writes.add(DataProvider.saveStable(output, DatagenIngredient.normalize(advancement), advancements.json(id)));
        }
      }
    };
    buildRecipes(consumer);
    return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
  }
}
