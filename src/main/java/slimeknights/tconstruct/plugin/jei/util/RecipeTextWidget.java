package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.gui.placement.IPlaceable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ITextWidget;
import net.minecraft.network.chat.FormattedText;

/** Position text through the stable placement ABI also implemented by EMI's JEI bridge. */
public final class RecipeTextWidget {
  private RecipeTextWidget() {}
  public static ITextWidget add(IRecipeExtrasBuilder builder, FormattedText text, int width, int height, int x, int y) {
    ITextWidget widget = builder.addText(text, width, height);
    ((IPlaceable<?>) widget).setPosition(x, y);
    return widget;
  }
}
