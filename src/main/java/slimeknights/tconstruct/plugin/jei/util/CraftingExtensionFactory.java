package slimeknights.tconstruct.plugin.jei.util;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

/** Register one native JEI extension while retaining computed material displays per recipe holder. */
public final class CraftingExtensionFactory<R extends CraftingRecipe> implements ICraftingCategoryExtension<R> {
  private final Function<RecipeHolder<R>, ? extends ICraftingCategoryExtension<R>> factory;
  // Both references are weak: a renderer may refer back to its holder, and must not retain old worlds.
  private final ReferenceQueue<RecipeHolder<R>> discarded = new ReferenceQueue<>();
  private final Map<HolderIdentity<R>, WeakReference<ICraftingCategoryExtension<R>>> extensions = new HashMap<>();

  /** RecipeHolder equality uses its ID; reloads and display variants require reference identity instead. */
  private static final class HolderIdentity<R extends CraftingRecipe> extends WeakReference<RecipeHolder<R>> {
    private final int hash;
    private HolderIdentity(RecipeHolder<R> holder, ReferenceQueue<RecipeHolder<R>> queue) {
      super(holder, queue);
      hash = System.identityHashCode(holder);
    }
    @Override public int hashCode() { return hash; }
    @Override public boolean equals(Object other) {
      if (this == other) return true;
      RecipeHolder<R> holder = get();
      return holder != null && other instanceof HolderIdentity<?> identity && holder == identity.get();
    }
  }

  public CraftingExtensionFactory(Function<RecipeHolder<R>, ? extends ICraftingCategoryExtension<R>> factory) {
    this.factory = factory;
  }

  @Nullable
  private ICraftingCategoryExtension<R> get(RecipeHolder<R> holder) {
    Reference<? extends RecipeHolder<R>> stale;
    while ((stale = discarded.poll()) != null) extensions.remove(stale);
    WeakReference<ICraftingCategoryExtension<R>> reference = extensions.get(new HolderIdentity<>(holder, null));
    ICraftingCategoryExtension<R> extension = reference == null ? null : reference.get();
    if (extension == null) {
      extension = factory.apply(holder);
      if (extension != null) {
        extensions.put(new HolderIdentity<>(holder, discarded), new WeakReference<>(extension));
      }
    }
    return extension;
  }

  @Override
  public boolean isHandled(RecipeHolder<R> holder) {
    ICraftingCategoryExtension<R> extension = get(holder);
    return extension != null && extension.isHandled(holder);
  }

  @Override
  public void setRecipe(RecipeHolder<R> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
    ICraftingCategoryExtension<R> extension = get(holder);
    if (extension != null) extension.setRecipe(holder, builder, grid, focuses);
  }

  @Override
  public int getWidth(RecipeHolder<R> holder) {
    ICraftingCategoryExtension<R> extension = get(holder);
    return extension == null ? 0 : extension.getWidth(holder);
  }

  @Override
  public int getHeight(RecipeHolder<R> holder) {
    ICraftingCategoryExtension<R> extension = get(holder);
    return extension == null ? 0 : extension.getHeight(holder);
  }

  @Override
  public void onDisplayedIngredientsUpdate(RecipeHolder<R> holder, List<IRecipeSlotDrawable> slots, IFocusGroup focuses) {
    ICraftingCategoryExtension<R> extension = get(holder);
    if (extension != null) extension.onDisplayedIngredientsUpdate(holder, slots, focuses);
  }

  @Override
  public void createRecipeExtras(RecipeHolder<R> holder, IRecipeExtrasBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
    ICraftingCategoryExtension<R> extension = get(holder);
    if (extension != null) extension.createRecipeExtras(holder, builder, grid, focuses);
  }

  @Override
  public void drawInfo(RecipeHolder<R> holder, int width, int height, GuiGraphics graphics, double mouseX, double mouseY) {
    ICraftingCategoryExtension<R> extension = get(holder);
    if (extension != null) extension.drawInfo(holder, width, height, graphics, mouseX, mouseY);
  }

  @Override
  public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<R> holder, double mouseX, double mouseY) {
    ICraftingCategoryExtension<R> extension = get(holder);
    if (extension != null) extension.getTooltip(tooltip, holder, mouseX, mouseY);
  }

  @Override
  public boolean handleInput(RecipeHolder<R> holder, double mouseX, double mouseY, InputConstants.Key input) {
    ICraftingCategoryExtension<R> extension = get(holder);
    return extension != null && extension.handleInput(holder, mouseX, mouseY, input);
  }
}
