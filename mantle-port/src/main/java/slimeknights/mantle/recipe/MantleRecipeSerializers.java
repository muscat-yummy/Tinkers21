package slimeknights.mantle.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.Mantle;


/** @deprecated use {@link MantleRecipes} */
@Deprecated(forRemoval = true)
public class MantleRecipeSerializers {
  private MantleRecipeSerializers() {}

  /** Populates legacy direct fields once deferred recipe registration has completed. */
  public static void bind() {
    CRAFTING_SHAPED_FALLBACK = MantleRecipes.CRAFTING_SHAPED_FALLBACK.get();
    CRAFTING_SHAPED_RETEXTURED = MantleRecipes.CRAFTING_SHAPED_RETEXTURED.get();
  }

  /** @deprecated use {@link MantleRecipes#CRAFTING_SHAPED_FALLBACK} */
  @Deprecated(forRemoval = true)
  public static RecipeSerializer<?> CRAFTING_SHAPED_FALLBACK;
  /** @deprecated use {@link MantleRecipes#CRAFTING_SHAPED_RETEXTURED} */
  @Deprecated(forRemoval = true)
  public static RecipeSerializer<?> CRAFTING_SHAPED_RETEXTURED;
}
