package slimeknights.tconstruct.library.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;

import java.util.function.Supplier;

/** A retained capability always resolves the current master, rather than a removed tank. */
public final class ForwardingFluidHandler implements IFluidHandler {
  private final Supplier<? extends IFluidHandler> source;

  public ForwardingFluidHandler(Supplier<? extends IFluidHandler> source) {
    this.source = source;
  }

  private IFluidHandler current() {
    IFluidHandler handler = source.get();
    return handler != null ? handler : EmptyFluidHandler.INSTANCE;
  }

  @Override
  public int getTanks() {
    return current().getTanks();
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    return current().getFluidInTank(tank);
  }

  @Override
  public int getTankCapacity(int tank) {
    return current().getTankCapacity(tank);
  }

  @Override
  public boolean isFluidValid(int tank, FluidStack stack) {
    return current().isFluidValid(tank, stack);
  }

  @Override
  public int fill(FluidStack resource, FluidAction action) {
    return current().fill(resource, action);
  }

  @Override
  public FluidStack drain(FluidStack resource, FluidAction action) {
    return current().drain(resource, action);
  }

  @Override
  public FluidStack drain(int maxDrain, FluidAction action) {
    return current().drain(maxDrain, action);
  }
}
