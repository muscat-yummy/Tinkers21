package slimeknights.mantle.recipe.crafting;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Builder for a shaped recipe with fallbacks. */
@SuppressWarnings("unused")
public class ShapedFallbackRecipeBuilder {
  private final ShapedRecipeBuilder base;
  private final List<ResourceLocation> alternatives = new ArrayList<>();

  private ShapedFallbackRecipeBuilder(ShapedRecipeBuilder base) {
    this.base = base;
  }

  public static ShapedFallbackRecipeBuilder fallback(ShapedRecipeBuilder base) {
    return new ShapedFallbackRecipeBuilder(base);
  }

  public ShapedFallbackRecipeBuilder addAlternative(ResourceLocation location) {
    alternatives.add(location);
    return this;
  }

  public ShapedFallbackRecipeBuilder addAlternatives(Collection<ResourceLocation> locations) {
    alternatives.addAll(locations);
    return this;
  }

  private RecipeOutput wrap(RecipeOutput output) {
    List<ResourceLocation> snapshot = List.copyOf(alternatives);
    return new RecipeOutput() {
      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }

      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
        if (!(recipe instanceof ShapedRecipe shaped)) {
          throw new IllegalArgumentException("Fallback recipe requires a shaped base");
        }
        output.accept(id, new ShapedFallbackRecipe(shaped, snapshot), advancement, conditions);
      }
    };
  }

  public void build(RecipeOutput output) {
    base.save(wrap(output));
  }

  public void build(RecipeOutput output, ResourceLocation id) {
    base.save(wrap(output), id);
  }
}
