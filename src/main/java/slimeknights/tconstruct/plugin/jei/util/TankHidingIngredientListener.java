package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IIngredientManager.IIngredientListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import slimeknights.tconstruct.library.fluid.EmptyFluidHandlerItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Handler to remove tanks when their fluid is removed from JEI. */
public record TankHidingIngredientListener(IIngredientManager manager, List<Item> tanks) implements IIngredientListener {
  /** Gets all changed tanks from the given ingredients */
  private List<ItemStack> getTanks(Collection<? extends ITypedIngredient<?>> ingredients) {
    List<ItemStack> list = new ArrayList<>();
    for (ITypedIngredient<?> ingredient : ingredients) {
      FluidStack fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).orElse(FluidStack.EMPTY);
      if (!fluid.isEmpty()) {
        for (Item item : tanks) {
          ItemStack tank = new ItemStack(item);
          IFluidHandlerItem handler = tank.getCapability(Capabilities.FluidHandler.ITEM);
          if (handler != null && handler.getTanks() > 0 && handler.fill(fluid, FluidAction.EXECUTE) > 0) {
            list.add(handler.getContainer());
          }
        }
      }
    }
    return list;
  }

  @Override
  public <V> void onIngredientsAdded(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
    List<ItemStack> tanks = getTanks(ingredients);
    if (!tanks.isEmpty()) {
      manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, tanks);
    }
  }

  @Override
  public <V> void onIngredientsRemoved(IIngredientHelper<V> ingredientHelper, Collection<ITypedIngredient<V>> ingredients) {
    List<ItemStack> tanks = getTanks(ingredients);
    if (!tanks.isEmpty()) {
      manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, tanks);
    }
  }
}
