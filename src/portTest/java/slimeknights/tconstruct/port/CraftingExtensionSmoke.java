package slimeknights.tconstruct.port;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.BookCloningRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.plugin.jei.util.CraftingExtensionFactory;

/** Exercise the production singleton adapter with actual native recipe holders and JEI callbacks. */
public final class CraftingExtensionSmoke {
  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
    net.minecraft.SharedConstants.tryDetectVersion();
    net.minecraft.server.Bootstrap.bootStrap();
    RecipeHolder<BookCloningRecipe> first = new RecipeHolder<>(
      ResourceLocation.fromNamespaceAndPath("tconstruct", "repair"), new BookCloningRecipe(CraftingBookCategory.MISC));
    RecipeHolder<BookCloningRecipe> reloaded = new RecipeHolder<>(first.id(), new BookCloningRecipe(CraftingBookCategory.MISC));
    RecipeHolder<BookCloningRecipe> hidden = new RecipeHolder<>(
      ResourceLocation.fromNamespaceAndPath("tconstruct", "hidden"), first.value());
    AtomicInteger creations = new AtomicInteger();
    AtomicReference<RecipeHolder<BookCloningRecipe>> seen = new AtomicReference<>();
    AtomicInteger layout = new AtomicInteger(), updates = new AtomicInteger(), extras = new AtomicInteger();
    // Keep renderers alive during this test, so cache reuse does not depend on garbage collection timing.
    var live = new java.util.ArrayList<ICraftingCategoryExtension<BookCloningRecipe>>();
    CraftingExtensionFactory<BookCloningRecipe> adapter = new CraftingExtensionFactory<>(holder -> {
      if (holder.id().equals(hidden.id())) return null;
      creations.incrementAndGet();
      ICraftingCategoryExtension<BookCloningRecipe> extension = new ICraftingCategoryExtension<>() {
        @Override public int getWidth(RecipeHolder<BookCloningRecipe> actual) { seen.set(actual); return 3; }
        @Override public int getHeight(RecipeHolder<BookCloningRecipe> actual) { seen.set(actual); return 2; }
        @Override public void setRecipe(RecipeHolder<BookCloningRecipe> actual, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
          seen.set(actual); layout.incrementAndGet();
        }
        @Override public void onDisplayedIngredientsUpdate(RecipeHolder<BookCloningRecipe> actual, List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
          seen.set(actual); updates.incrementAndGet();
        }
        @Override public void createRecipeExtras(RecipeHolder<BookCloningRecipe> actual, IRecipeExtrasBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
          seen.set(actual); extras.incrementAndGet();
        }
      };
      live.add(extension);
      return extension;
    });
    if (!adapter.isHandled(first) || adapter.getWidth(first) != 3 || adapter.getHeight(first) != 2) {
      throw new AssertionError("Native shaped dimensions");
    }
    adapter.setRecipe(first, null, null, null);
    adapter.onDisplayedIngredientsUpdate(first, List.of(), null);
    adapter.createRecipeExtras(first, null, null, null);
    if (seen.get() != first || layout.get() != 1 || updates.get() != 1 || extras.get() != 1 || creations.get() != 1) {
      throw new AssertionError("Every display callback must receive the exact source holder and reuse its renderer");
    }
    if (adapter.isHandled(hidden)) throw new AssertionError("Invalid material display must remain unhandled");
    adapter.setRecipe(hidden, null, null, null);
    if (layout.get() != 1) throw new AssertionError("Unhandled display must not use another recipe renderer");
    adapter.setRecipe(reloaded, null, null, null);
    if (seen.get() != reloaded || creations.get() != 2 || layout.get() != 2) {
      throw new AssertionError("A reloaded recipe with the same ID must have its own renderer");
    }
    System.out.println("Native crafting holder identity, dimensions, callbacks, invalid displays and reload cache: PASS");
  }
}
