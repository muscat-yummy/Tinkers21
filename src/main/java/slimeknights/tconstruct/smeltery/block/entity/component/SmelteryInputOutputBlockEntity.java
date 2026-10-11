package slimeknights.tconstruct.smeltery.block.entity.component;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import slimeknights.tconstruct.library.fluid.ForwardingFluidHandler;
import slimeknights.tconstruct.library.utils.ForwardingItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.inventory.EmptyItemHandler;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.common.multiblock.IMasterLogic;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import static slimeknights.mantle.util.RetexturedHelper.TAG_TEXTURE;

/**
 * Shared logic between drains and ducts
 */
public abstract class SmelteryInputOutputBlockEntity<T> extends SmelteryComponentBlockEntity implements IRetexturedBlockEntity {
  /** Capability this TE watches */
  private final BlockCapability<T,Direction> capability;
  /** Empty capability for in case the valid capability becomes invalid without invalidating */
  protected final T emptyInstance;
  /* Retexturing */
  @Nonnull
  @Getter
  private Block texture = Blocks.AIR;

  protected SmelteryInputOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockCapability<T,Direction> capability, T emptyInstance) {
    super(type, pos, state);
    this.capability = capability;
    this.emptyInstance = emptyInstance;
  }

  /** Notify native consumers when the IO block changes its master. */
  private void clearHandler() {
    if (level != null) {
      level.invalidateCapabilities(worldPosition);
    }
  }

  @Override
  public void onMasterLoad(IMasterLogic master) {
    clearHandler();
  }

  @Override
  protected void setMaster(@Nullable BlockPos master, @Nullable Block block) {
    assert level != null;

    // if we have a new master, invalidate handlers
    boolean masterChanged = !Objects.equals(getMasterPos(), master);
    super.setMaster(master, block);
    // notify neighbors of the change (state change skips the notify flag)
    if (masterChanged) {
      clearHandler();
      level.blockUpdated(worldPosition, getBlockState().getBlock());
    }
  }

  /** Get the current master's native capability; fluid IO overrides its internal tank lookup. */
  @Nullable
  protected T getCapability(BlockEntity parent) {
    return level != null ? level.getCapability(capability, parent.getBlockPos(), null) : null;
  }

  /** Resolve each operation against the loaded, valid master, avoiding stale capabilities. */
  @Nullable
  public T getHandler() {
    BlockPos master = getMasterPos();
    if (isRemoved() || level == null || master == null || !level.hasChunkAt(master) || !validateMaster()) {
      return null;
    }
    BlockEntity parent = level.getBlockEntity(master);
    return parent != null && !parent.isRemoved() ? getCapability(parent) : null;
  }

  /* Retexturing */

  @Override
  @Nonnull
  public ModelData getModelData() {
    return RetexturedHelper.getModelData(getTexture());
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /* NBT */

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void saveSynced(CompoundTag tags) {
    super.saveSynced(tags);
    if (texture != Blocks.AIR) {
      tags.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  protected void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    if (tags.contains(TAG_TEXTURE, Tag.TAG_STRING)) {
      texture = RetexturedHelper.getBlock(tags.getString(TAG_TEXTURE));
      RetexturedHelper.onTextureUpdated(this);
    }
  }


  /** Fluid implementation of smeltery IO */
  public static abstract class SmelteryFluidIO extends SmelteryInputOutputBlockEntity<IFluidHandler> {
    protected SmelteryFluidIO(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, Capabilities.FluidHandler.BLOCK, EmptyFluidHandler.INSTANCE);
    }

    private final IFluidHandler forwardingHandler = new ForwardingFluidHandler(this::getHandler);

    /** Native callers retain a forwarding proxy whose operations resolve the current master. */
    @Nullable
    public IFluidHandler getFluidHandler() {
      return getHandler() != null ? forwardingHandler : null;
    }

    protected IFluidHandler makeWrapper(IFluidHandler capability) {
      return capability;
    }

    @Nullable
    @Override
    protected IFluidHandler getCapability(BlockEntity parent) {
      if (parent instanceof ISmelteryTankHandler tankHandler) {
        IFluidHandler capability = tankHandler.getFluidCapability();
        return capability != null ? makeWrapper(capability) : null;
      }
      return null;
    }
  }

  /** Item implementation of smeltery IO */
  public static class ChuteBlockEntity extends SmelteryInputOutputBlockEntity<IItemHandler> {
    public ChuteBlockEntity(BlockPos pos, BlockState state) {
      this(TinkerSmeltery.chute.get(), pos, state);
    }

    protected ChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state, Capabilities.ItemHandler.BLOCK, EmptyItemHandler.INSTANCE);
    }

    private final IItemHandler forwardingHandler = new ForwardingItemHandler(this::getHandler);

    @Nullable
    public IItemHandler getItemHandler() {
      return getHandler() != null ? forwardingHandler : null;
    }
  }

}
