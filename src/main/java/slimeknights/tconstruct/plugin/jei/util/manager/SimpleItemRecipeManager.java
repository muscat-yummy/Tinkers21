package slimeknights.tconstruct.plugin.jei.util.manager;

import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.library.recipe.display.FilteredItemRecipe;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayCraftingTinkering;

import java.util.List;

/** Recipe manager handling filtering recipes that use just items for inputs and outputs. Used notably in {@link slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringCategory}. */
public class SimpleItemRecipeManager<T extends FilteredItemRecipe> implements ISimpleRecipeManagerPlugin<T> {
  @Getter
  private final List<T> allRecipes;
  private final ItemRecipeCache<T> inputItemCache, outputItemCache;

  public SimpleItemRecipeManager(IIngredientManager ingredientManager, List<T> recipes) {
    this.allRecipes = FilteredRecipe.alwaysVisible(recipes);
    IIngredientHelper<ItemStack> itemHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
    inputItemCache = new ItemRecipeCache<>(itemHelper, recipes, false);
    outputItemCache = new ItemRecipeCache<>(itemHelper, recipes, true);
  }

  @Override
  public boolean isHandledInput(ITypedIngredient<?> input) {
    return input.getType() == VanillaTypes.ITEM_STACK;
  }

  @Override
  public boolean isHandledOutput(ITypedIngredient<?> output) {
    return output.getType() == VanillaTypes.ITEM_STACK;
  }

  @Override
  public List<T> getRecipesForInput(ITypedIngredient<?> input) {
    ITypedIngredient<ItemStack> item = input.cast(VanillaTypes.ITEM_STACK);
    if (item != null) {
      return inputItemCache.filterRecipes(item.getIngredient());
    }
    return List.of();
  }

  @Override
  public List<T> getRecipesForOutput(ITypedIngredient<?> output) {
    ITypedIngredient<ItemStack> item = output.cast(VanillaTypes.ITEM_STACK);
    if (item != null) {
      return outputItemCache.filterRecipes(item.getIngredient());
    }
    return List.of();
  }

  /** Adapt display variants using their actual source crafting IDs carried by native recipe holders. */
  public static ISimpleRecipeManagerPlugin<RecipeHolder<CraftingRecipe>> createCrafting(
      IIngredientManager ingredientManager, List<? extends IDisplayCraftingTinkering> recipes) {
    var delegate = new SimpleItemRecipeManager<IDisplayCraftingTinkering>(
      ingredientManager, List.copyOf(recipes));
    return new ISimpleRecipeManagerPlugin<>() {
      @Override
      public boolean isHandledInput(ITypedIngredient<?> input) {
        return delegate.isHandledInput(input);
      }
      @Override
      public boolean isHandledOutput(ITypedIngredient<?> output) {
        return delegate.isHandledOutput(output);
      }
      @Override
      public List<RecipeHolder<CraftingRecipe>> getAllRecipes() {
        return holders(delegate.getAllRecipes());
      }
      @Override
      public List<RecipeHolder<CraftingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
        return holders(delegate.getRecipesForInput(input));
      }
      @Override
      public List<RecipeHolder<CraftingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
        return holders(delegate.getRecipesForOutput(output));
      }
    };
  }

  private static List<RecipeHolder<CraftingRecipe>> holders(
      List<IDisplayCraftingTinkering> recipes) {
    return recipes.stream().map(recipe -> new RecipeHolder<CraftingRecipe>(
      recipe.getId(), recipe)).toList();
  }
}
