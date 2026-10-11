package slimeknights.tconstruct.smeltery.block.entity.inventory;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import javax.annotation.Nullable;

/**
 * Provides read only access to the input of a casting table. Prevents extra data from leaking
 */
@RequiredArgsConstructor
public class CastingContainerWrapper implements ICastingContainer {
  private final CastingBlockEntity tile;
  @Setter
  private FluidStack fluid = FluidStack.EMPTY;
  private boolean switchSlots = false;

  @Override
  public ItemStack getStack() {
    ItemStack stack = tile.getItem(switchSlots ? CastingBlockEntity.OUTPUT : CastingBlockEntity.INPUT);
    if (stack.is(tile.getEmptyCastTag())) {
      return ItemStack.EMPTY;
    }
    return stack;
  }

  @Override
  public Fluid getFluid() {
    return fluid.getFluid();
  }

  @Nullable
  @Override
  public CompoundTag getFluidTag() {
    CustomData data = fluid.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : data.copyTag();
  }

  @Override
  public FluidStack getFluidStack() {
    return fluid.copy();
  }

  /** Uses the input for input (default) */
  public void useInput() {
    switchSlots = false;
  }

  /** Uses the output for input (for multistep casting) */
  public void useOutput() {
    switchSlots = true;
  }
}
