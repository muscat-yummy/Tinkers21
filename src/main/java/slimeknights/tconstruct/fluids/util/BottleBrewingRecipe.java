package slimeknights.tconstruct.fluids.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;

/** Recipe for transforming a bottle, depending on a vanilla brewing recipe to get the ingredient */
public class BottleBrewingRecipe extends BrewingRecipe {
  private final Item from;
  private final Item to;
  private final PotionBrewing.Builder brewing;
  public BottleBrewingRecipe(Ingredient input, Item from, Item to, ItemStack output, PotionBrewing.Builder brewing) {
    super(input, Ingredient.EMPTY, output);
    this.from = from;
    this.to = to;
    this.brewing = java.util.Objects.requireNonNull(brewing);
  }

  @Override
  public boolean isIngredient(ItemStack stack) {
    for (PotionBrewing.Mix<Item> recipe : brewing.containerMixes) {
      if (recipe.from().value() == from && recipe.to().value() == to) {
        return recipe.ingredient().test(stack);
      }
    }
    return false;
  }

  @Override
  public Ingredient getIngredient() {
    for (PotionBrewing.Mix<Item> recipe : brewing.containerMixes) {
      if (recipe.from().value() == from && recipe.to().value() == to) {
        return recipe.ingredient();
      }
    }
    return Ingredient.EMPTY;
  }
}
