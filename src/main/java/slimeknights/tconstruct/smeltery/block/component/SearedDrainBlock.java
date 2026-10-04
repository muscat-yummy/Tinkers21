package slimeknights.tconstruct.smeltery.block.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.utils.BlockInteractionUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.smeltery.block.entity.component.DrainBlockEntity;

/** Extenson to include interaction behavior */
public class SearedDrainBlock extends RetexturedOrientableSmelteryBlock {
  public SearedDrainBlock(Properties properties) {
    super(properties, DrainBlockEntity::new);
  }

  @Override
  protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
      Player player, InteractionHand hand, BlockHitResult hit) {
    return BlockInteractionUtil.withItem(stack, () -> use(state, world, pos, player, hand, hit));
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    return use(state, world, pos, player, InteractionHand.MAIN_HAND, hit);
  }

  /** Owned interaction shared by native hand-aware callbacks. */
  @Deprecated
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit.getDirection(), state.getValue(FACING).getOpposite())) {
      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
}
