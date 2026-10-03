package slimeknights.tconstruct.port;

import java.lang.reflect.Proxy;
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
    System.out.println("Pinned JEI widget registration, relative/exclusive hover bounds and dynamic tooltip: PASS");
  }
}
