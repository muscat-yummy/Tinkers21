package slimeknights.tconstruct.smeltery.block;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.utils.BlockInteractionUtil;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.tconstruct.shared.block.TableBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

import javax.annotation.Nullable;

public abstract class AbstractCastingBlock extends TableBlock {
  /** Property for when the casting block has an item inside */
  public static final BooleanProperty HAS_ITEM = BooleanProperty.create("has_item");

  @Getter
  private final boolean requireCast;
  protected AbstractCastingBlock(Properties builder, boolean requireCast) {
    super(builder);
    this.requireCast = requireCast;
    registerDefaultState(defaultBlockState().setValue(HAS_ITEM, false));
  }

  @Override
  protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(HAS_ITEM);
  }

  @Override
  @Deprecated
  @Nullable
  public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
    return null;
  }

  @Deprecated
  @Override
  public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    return BlockInteractionUtil.withItem(stack, () -> use(state, level, pos, player, hand, hit));
  }

  @Override
  public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    return use(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
  }

  /** Owned interaction callback retained for subclasses; native entry points delegate here. */
  @Deprecated
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult) {
    if (player.isShiftKeyDown()) {
      return InteractionResult.PASS;
    }
    BlockEntity te = world.getBlockEntity(pos);
    if (te instanceof CastingBlockEntity) {
      ((CastingBlockEntity) te).interact(player, hand);
      return InteractionResult.SUCCESS;
    }
    return super.useWithoutItem(state, world, pos, player, rayTraceResult);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
    if (!world.isClientSide && world.getBlockEntity(pos) instanceof CastingBlockEntity casting) {
      casting.handleRedstone(world.hasNeighborSignal(pos));
    }
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
    if (world.getBlockEntity(pos) instanceof CastingBlockEntity casting) {
      casting.swap();
    }
  }

  @Override
  protected boolean openGui(Player playerEntity, Level world, BlockPos blockPos) {
    return false;
  }

  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @Override
  public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
    if (world.getBlockEntity(pos) instanceof CastingBlockEntity casting) {
      return casting.getAnalogSignal();
    }
    return 0;
  }
}
