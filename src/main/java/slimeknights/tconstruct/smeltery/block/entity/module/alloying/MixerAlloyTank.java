package slimeknights.tconstruct.smeltery.block.entity.module.alloying;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.capabilities.Capabilities;
import slimeknights.tconstruct.library.utils.BlockCapabilitySource;
import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.alloying.IMutableAlloyTank;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Alloy tank that takes inputs from neighboring blocks
 */
@RequiredArgsConstructor
public class MixerAlloyTank implements IMutableAlloyTank {
  // parameters
  /** Handler parent */
  private final MantleBlockEntity parent;
  /** Tank for outputs */
  private final IFluidHandler outputTank;

  /** Current temperature. Provided as a getter and setter as there are a few contexts with different source for temperature */
  @Getter
  @Setter
  private int temperature = 0;

  // side tank cache
  /** Cache of tanks for each of the sides */
  private final Map<Direction,BlockCapabilitySource<IFluidHandler>> inputs = new EnumMap<>(Direction.class);
  /** Map of tank index to tank on the side */
  @Nullable
  private IFluidHandler[] indexedList = null;

  // state
  /** Number of currently held tanks */
  private int currentTanks = 0;

  @Override
  public int getTanks() {
    checkTanks();
    return currentTanks;
  }

  /** Gets the map of index to direction */
  private IFluidHandler[] indexTanks() {
    checkTanks();
    return indexedList;
  }

  /** Gets the fluid handler for the given tank index */
  public IFluidHandler getFluidHandler(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return EmptyFluidHandler.INSTANCE;
    }
    return indexTanks()[tank];
  }

  @Override
  public FluidStack getFluidInTank(int tank) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    // get the first fluid from the proper tank, we do not support multiple fluids on a side
    return indexTanks()[tank].getFluidInTank(0);
  }

  @Override
  public FluidStack drain(int tank, FluidStack fluidStack) {
    checkTanks();
    // invalid index, nothing
    if (tank >= currentTanks || tank < 0) {
      return FluidStack.EMPTY;
    }
    return indexTanks()[tank].drain(fluidStack, FluidAction.EXECUTE);
  }

  @Override
  public boolean canFit(FluidStack fluid, int removed) {
    checkTanks();
    return outputTank.fill(fluid, FluidAction.SIMULATE) == fluid.getAmount();
  }

  @Override
  public int fill(FluidStack fluidStack) {
    return outputTank.fill(fluidStack, FluidAction.EXECUTE);
  }

  /**
   * Refreshes the cached tanks if needed
   * After calling this method, all five tank sides will have been fetched
   */
  private void checkTanks() {
    Level world = parent.getLevel();
    if (world == null || parent.isRemoved()) {
      inputs.clear();
      indexedList = new IFluidHandler[0];
      currentTanks = 0;
      return;
    }
    var tanks = new ArrayList<IFluidHandler>(5);
    for (Direction direction : Direction.values()) {
      if (direction == Direction.DOWN) {
        continue;
      }
      BlockPos target = parent.getBlockPos().relative(direction);
      // Retain the datapack tag restriction and avoid loading neighboring chunks.
      if (world.hasChunkAt(target) && world.getBlockState(target).is(TinkerTags.Blocks.ALLOYER_TANKS)) {
        var source = inputs.computeIfAbsent(direction,
          side -> new BlockCapabilitySource<>(parent, Capabilities.FluidHandler.BLOCK, target, side.getOpposite()));
        IFluidHandler handler = source.get();
        if (handler != null && handler.getTanks() > 0) {
          tanks.add(handler);
        }
      }
    }
    // Resolve every native cache before using an indexed list so absent/replaced handlers recover too.
    boolean changed = indexedList == null || tanks.size() != currentTanks;
    for (int index = 0; !changed && index < tanks.size(); index++) {
      changed = indexedList[index] != tanks.get(index);
    }
    if (changed) {
      indexedList = tanks.toArray(IFluidHandler[]::new);
      currentTanks = indexedList.length;
    }
  }

  /** Discard the lookup for a neighbor change; native sources handle capability invalidation. */
  public void refresh(Direction direction, boolean checkInput) {
    if (direction != Direction.DOWN) {
      inputs.remove(direction);
      indexedList = null;
    }
  }

}
