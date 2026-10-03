package slimeknights.tconstruct.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil.FoodConsumer;

/** Optional bridge to Diet's published capability/tracker contract, without loading an incompatible Forge jar. */
public class DietPlugin {
  public static void onConstruct() {
    final Method get;
    final Method ifPresent;
    final Method consumeItem;
    final Method consumeItems;
    try {
      ClassLoader loader = DietPlugin.class.getClassLoader();
      Class<?> capability = Class.forName("com.illusivesoulworks.diet.common.capability.DietCapability", true, loader);
      Class<?> tracker = Class.forName("com.illusivesoulworks.diet.api.type.IDietTracker", true, loader);
      get = capability.getMethod("get", Player.class);
      ifPresent = get.getReturnType().getMethod("ifPresent", Consumer.class);
      consumeItem = tracker.getMethod("consume", ItemStack.class, int.class, float.class);
      consumeItems = tracker.getMethod("consume", List.class, int.class, float.class);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Installed Diet does not expose its supported capability/tracker contract", e);
    }
    ModifierUtil.foodConsumer = new FoodConsumer() {
      private void consume(Player player, Method method, Object food, int hunger, float saturation) {
        Object capability = invoke(get, null, player);
        invoke(ifPresent, capability, (Consumer<Object>) tracker -> invoke(method, tracker, food, hunger, saturation));
      }

      @Override
      public void onConsume(Player player, ItemStack stack, int hunger, float saturation) {
        consume(player, consumeItem, stack, hunger, saturation);
      }

      @Override
      public void onConsume(Player player, List<ItemStack> stacks, int hunger, float saturation) {
        consume(player, consumeItems, stacks, hunger, saturation);
      }
    };
  }

  private static Object invoke(Method method, Object receiver, Object... arguments) {
    try {
      return method.invoke(receiver, arguments);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException("Diet food consumption failed", e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to call Diet's capability/tracker contract", e);
    }
  }
}
