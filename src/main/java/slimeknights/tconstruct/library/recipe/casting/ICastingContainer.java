package slimeknights.tconstruct.library.recipe.casting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.container.ISingleStackContainer;

import javax.annotation.Nullable;

/**
 * Inventory containing a single item and a fluid
 */
public interface ICastingContainer extends ISingleStackContainer {
  /**
   * Gets the contained fluid in this inventory
   * @return  Contained fluid
   */
  Fluid getFluid();

  /** Component-preserving fluid input for native casting recipes. */
  default FluidStack getFluidStack() {
    FluidStack result = new FluidStack(getFluid(), 1);
    CompoundTag legacy = getFluidTag();
    if (!result.isEmpty() && legacy != null && !legacy.isEmpty()) {
      result.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy.copy()));
    }
    return result;
  }

  /**
   * Gets the NBT for the contained fluid
   * @return  Fluid's NBT
   */
  @Nullable
  default CompoundTag getFluidTag() {
    return null;
  }
}
