package slimeknights.tconstruct.port;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import slimeknights.tconstruct.library.fluid.ForwardingFluidHandler;
import slimeknights.tconstruct.library.utils.ForwardingItemHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ForwardingCapabilitySmoke {
  private static void check(boolean result, String message) {
    if (!result) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    var original = new FluidTank(1000);
    var replacement = new FluidTank(2000);
    var source = new AtomicReference<FluidTank>(original);
    var retained = new ForwardingFluidHandler(source::get);
    FluidStack fluid = new FluidStack(Fluids.WATER, 600);
    fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Component water"));
    check(retained.fill(fluid, FluidAction.SIMULATE) == 600 && original.isEmpty(), "Simulation changed the tank");
    check(retained.fill(fluid, FluidAction.EXECUTE) == 600, "Initial forwarding fill failed");
    source.set(replacement);
    check(retained.getTankCapacity(0) == 2000 && retained.getFluidInTank(0).isEmpty(), "Retained capability used old master");
    check(retained.fill(fluid, FluidAction.EXECUTE) == 600 && original.getFluidAmount() == 600, "Fill reached old master");
    FluidStack drained = retained.drain(200, FluidAction.SIMULATE);
    check(drained.getAmount() == 200 && replacement.getFluidAmount() == 600, "Drain simulation consumed fluid");
    check(FluidStack.isSameFluidSameComponents(drained, fluid), "Forwarding lost fluid components");
    check(retained.drain(fluid.copyWithAmount(200), FluidAction.EXECUTE).getAmount() == 200
      && replacement.getFluidAmount() == 400, "Typed drain reached old master");
    source.set(null);
    check(retained.fill(fluid, FluidAction.EXECUTE) == 0 && retained.drain(100, FluidAction.EXECUTE).isEmpty(),
      "Absent master still accepted transfers");
    check(original.getFluidAmount() == 600 && replacement.getFluidAmount() == 400, "Detached proxy changed a tank");
    source.set(original);
    check(retained.drain(100, FluidAction.EXECUTE).getAmount() == 100 && original.getFluidAmount() == 500,
      "Master reload did not recover");

    var firstItems = new ItemStackHandler(1);
    var nextItems = new ItemStackHandler(1);
    var itemSource = new AtomicReference<ItemStackHandler>(firstItems);
    var itemProxy = new ForwardingItemHandler(itemSource::get);
    ItemStack stack = new ItemStack(Items.STICK, 7);
    stack.set(DataComponents.CUSTOM_NAME, Component.literal("Component sticks"));
    check(itemProxy.insertItem(0, stack, true).isEmpty() && firstItems.getStackInSlot(0).isEmpty(), "Item simulation changed inventory");
    check(itemProxy.insertItem(0, stack, false).isEmpty(), "Item insertion failed");
    itemSource.set(nextItems);
    check(itemProxy.getStackInSlot(0).isEmpty(), "Item proxy retained old inventory");
    itemProxy.insertItem(0, stack, false);
    ItemStack extracted = itemProxy.extractItem(0, 2, true);
    check(extracted.getCount() == 2 && nextItems.getStackInSlot(0).getCount() == 7, "Extraction simulation consumed items");
    check(ItemStack.isSameItemSameComponents(extracted, stack), "Forwarding lost item components");
    check(itemProxy.extractItem(0, 2, false).getCount() == 2 && firstItems.getStackInSlot(0).getCount() == 7,
      "Extraction reached old master");
    itemSource.set(null);
    check(itemProxy.insertItem(0, stack, false).getCount() == 7 && itemProxy.extractItem(0, 1, false).isEmpty(),
      "Absent item master still accepted transfers");
    itemSource.set(firstItems);
    check(itemProxy.getSlots() == 1 && itemProxy.extractItem(0, 1, false).getCount() == 1, "Inventory reload did not recover");
    System.out.println("Retained fluid/item capabilities follow replacement, removal and reload; simulations and components: PASS");
  }
}
