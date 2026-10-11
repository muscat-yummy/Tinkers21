package slimeknights.tconstruct.common.data;

import net.minecraft.data.PackOutput;
import slimeknights.mantle.recipe.data.FinishedRecipe;
import slimeknights.mantle.recipe.data.RecipeConsumer;
import slimeknights.mantle.recipe.data.MantleRecipeProvider;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import slimeknights.mantle.recipe.data.IRecipeHelper;
import slimeknights.tconstruct.TConstruct;

import java.util.function.Consumer;

/**
 * Shared logic for each module's recipe provider
 */
public abstract class BaseRecipeProvider extends MantleRecipeProvider implements IConditionBuilder, IRecipeHelper {
  public BaseRecipeProvider(PackOutput generator, java.util.concurrent.CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
    super(generator, registries);
    TConstruct.sealTinkersClass(this, "BaseRecipeProvider", "BaseRecipeProvider is trivial to recreate and directly extending can lead to addon recipes polluting our namespace.");
  }

  @Override
  protected abstract void buildRecipes(RecipeConsumer consumer);

  @Override
  public abstract String getName();

  @Override
  public String getModId() {
    return TConstruct.MOD_ID;
  }
}
