package slimeknights.tconstruct.port;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.network.chat.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import slimeknights.tconstruct.plugin.jei.util.DrawableTooltipWidget;

/** Exercises the production widget through the pinned JEI registration and tooltip contract. */
public final class RecipeWidgetSmoke {
  public static void main(String[] args) {
    AtomicReference<IRecipeWidget> registered = new AtomicReference<>();
    IRecipeExtrasBuilder builder = (IRecipeExtrasBuilder) Proxy.newProxyInstance(
      IRecipeExtrasBuilder.class.getClassLoader(), new Class<?>[] {IRecipeExtrasBuilder.class},
      (proxy, method, arguments) -> {
        if (method.getName().equals("addWidget")) {
          registered.set((IRecipeWidget) arguments[0]);
          return null;
        }
        throw new AssertionError("Unexpected builder call " + method.getName());
      });
    IDrawable drawable = new IDrawable() {
      public int getWidth() { return 24; }
      public int getHeight() { return 17; }
      public void draw(GuiGraphics graphics, int x, int y) {}
    };
    AtomicInteger value = new AtomicInteger(4);
    AtomicInteger seen = new AtomicInteger(-1);
    DrawableTooltipWidget widget = DrawableTooltipWidget.add(builder, drawable)
      .setPosition(58, 18).setTooltip(tooltip -> seen.set(value.get()));
    if (registered.get() != widget || widget.getPosition().x() != 58 || widget.getPosition().y() != 18) {
      throw new AssertionError("Widget registration/position");
    }
    widget.getTooltip(null, 0, 0);
    if (seen.get() != 4) throw new AssertionError("Minimum edge must hover");
    value.set(12);
    widget.getTooltip(null, 23.99, 16.99);
    if (seen.get() != 12) throw new AssertionError("Dynamic tooltip must use current displayed value");
    for (double[] point : new double[][] {{-0.01, 0}, {0, -0.01}, {24, 0}, {0, 17}, {58, 18}, {Double.NaN, 0}}) {
      seen.set(-1);
      widget.getTooltip(null, point[0], point[1]);
      if (seen.get() != -1) throw new AssertionError("Outside widget tooltip");
    }
    List<Component> text = new ArrayList<>(List.of(Component.literal("first"), Component.literal("second")));
    AtomicReference<List<?>> tooltipText = new AtomicReference<>();
    ITooltipBuilder tooltip = (ITooltipBuilder) Proxy.newProxyInstance(
      ITooltipBuilder.class.getClassLoader(), new Class<?>[] {ITooltipBuilder.class},
      (proxy, method, arguments) -> {
        if (method.getName().equals("addAll")) {
          tooltipText.set(List.copyOf((List<?>) arguments[0]));
          return method.getReturnType().isInstance(proxy) ? proxy : null;
        }
        throw new AssertionError("Unexpected tooltip call " + method.getName());
      });
    DrawableTooltipWidget area = DrawableTooltipWidget.area(builder, 3, 2, 118, 9).setTooltip(text);
    text.clear();
    area.drawWidget(null, 0, 0);
    area.getTooltip(tooltip, 117.99, 8.99);
    if (registered.get() != area || tooltipText.get() == null || tooltipText.get().size() != 2
        || area.getPosition().x() != 3 || area.getPosition().y() != 2) {
      throw new AssertionError("Tooltip area must preserve position and text snapshot");
    }
    tooltipText.set(null);
    area.getTooltip(tooltip, 118, 0);
    if (tooltipText.get() != null) throw new AssertionError("Tooltip area upper edge must be exclusive");
    System.out.println("Pinned JEI widget registration, relative/exclusive hover bounds and dynamic tooltip: PASS");
  }
}
