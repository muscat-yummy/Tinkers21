package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** Drawable and hover tooltip using the widget contract in the pinned JEI 19 API. */
public final class DrawableTooltipWidget implements IRecipeWidget {
  private final IDrawable drawable;
  private mezz.jei.api.gui.placement.IPlaceable<?> placement;
  private ScreenPosition position = new ScreenPosition(0, 0);
  private Consumer<ITooltipBuilder> tooltip = builder -> {};

  private DrawableTooltipWidget(IDrawable drawable) {
    this.drawable = drawable;
  }

  public static DrawableTooltipWidget add(IRecipeExtrasBuilder builder, IDrawable drawable) {
    DrawableTooltipWidget widget = new DrawableTooltipWidget(drawable);
    widget.placement = builder.addDrawable(drawable);
    builder.addWidget(widget);
    return widget;
  }

  /** A tooltip-only region on JEI versions without a tooltip-area builder. */
  public static DrawableTooltipWidget area(IRecipeExtrasBuilder builder, int x, int y, int width, int height) {
    IDrawable bounds = new IDrawable() {
      @Override public int getWidth() { return width; }
      @Override public int getHeight() { return height; }
      @Override public void draw(GuiGraphics graphics, int xOffset, int yOffset) {}
    };
    return add(builder, bounds).setPosition(x, y);
  }

  public DrawableTooltipWidget setTooltip(List<Component> text) {
    List<Component> snapshot = List.copyOf(text);
    return setTooltip(builder -> builder.addAll(snapshot));
  }

  public DrawableTooltipWidget setPosition(int x, int y) {
    position = new ScreenPosition(x, y);
    placement.setPosition(x, y);
    return this;
  }

  public DrawableTooltipWidget setTooltip(Component text) {
    return setTooltip(builder -> builder.add(text));
  }

  public DrawableTooltipWidget setTooltip(Consumer<ITooltipBuilder> callback) {
    tooltip = callback;
    return this;
  }

  @Override
  public ScreenPosition getPosition() {
    return position;
  }

  @Override
  public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
    // Image is rendered once by addDrawable; this widget owns hover text only.
  }

  @Override
  public void getTooltip(ITooltipBuilder builder, double mouseX, double mouseY) {
    if (contains(mouseX, mouseY, drawable.getWidth(), drawable.getHeight())) {
      tooltip.accept(builder);
    }
  }

  /** Bounds are relative to the widget and exclude their upper edges. */
  public static boolean contains(double x, double y, int width, int height) {
    return x >= 0 && y >= 0 && x < width && y < height;
  }
}
