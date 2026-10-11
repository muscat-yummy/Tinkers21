package slimeknights.tconstruct.world.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.common.util.TriState;
import slimeknights.tconstruct.world.TinkerWorld;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SlimeDirtBlock extends Block {

  public SlimeDirtBlock(Properties properties) {
    super(properties);
  }

  @Override
  public TriState canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, Direction facing, BlockState plant) {
    // can sustain both slimeplants and normal plants
    return plant.is(TinkerWorld.SLIME_PLANT_TYPE) || plant.is(TinkerWorld.PLAINS_PLANT_TYPE) ? TriState.TRUE : TriState.FALSE;
  }
}
