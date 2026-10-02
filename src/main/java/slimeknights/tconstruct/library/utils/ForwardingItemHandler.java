package slimeknights.tconstruct.library.utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

import java.util.function.Supplier;

/** A retained capability forwards operations to the current master inventory. */
public final class ForwardingItemHandler implements IItemHandler {
  private final Supplier<? extends IItemHandler> source;

  public ForwardingItemHandler(Supplier<? extends IItemHandler> source) {
    this.source = source;
  }

  private IItemHandler current() {
    IItemHandler handler = source.get();
    return handler != null ? handler : EmptyItemHandler.INSTANCE;
  }

  @Override
  public int getSlots() {
    return current().getSlots();
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    return current().getStackInSlot(slot);
  }

  @Override
  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    return current().insertItem(slot, stack, simulate);
  }

  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    return current().extractItem(slot, amount, simulate);
  }

  @Override
  public int getSlotLimit(int slot) {
    return current().getSlotLimit(slot);
  }

  @Override
  public boolean isItemValid(int slot, ItemStack stack) {
    return current().isItemValid(slot, stack);
  }
}
