package slimeknights.tconstruct.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class NativeBasinGameTest {
  @GameTest(template = "smeltery", timeoutTicks = 1200)
  public static void actualFaucetIronGoldBlocksAndEmptyPlatform(GameTestHelper helper) {
    var level = helper.getLevel();
    BlockPos[] basins = new BlockPos[4];
    for (int i = 0; i < 4; i++) {
      int x = (i % 2) * 3;
      int z = 1 + (i / 2) * 2;
      boolean scorched = i >= 2;
      boolean gold = i % 2 == 1;
      BlockPos source = new BlockPos(x, 2, z);
      BlockPos faucetPos = source.east();
      basins[i] = faucetPos.below();
      helper.setBlock(source, TinkerSmeltery.searedTank.get(TankType.FUEL_TANK));
      helper.setBlock(faucetPos, TinkerSmeltery.searedFaucet.get().defaultBlockState().setValue(FaucetBlock.FACING, Direction.EAST));
      helper.setBlock(basins[i], scorched ? TinkerSmeltery.scorchedBasin.get() : TinkerSmeltery.searedBasin.get());
      FluidStack fluid = new FluidStack(gold ? TinkerFluids.moltenGold.get() : TinkerFluids.moltenIron.get(), FluidValues.METAL_BLOCK);
      var input = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(source), Direction.EAST);
      var output = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(basins[i]), Direction.UP);
      helper.assertTrue(input != null && output != null, "Actual basin/source fluid capabilities missing");
      if (scorched) {
        helper.assertTrue(output.fill(fluid, FluidAction.SIMULATE) == 0, "Empty scorched basin must still require a cast/platform");
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(basins[i]), Direction.UP);
        helper.assertTrue(inventory != null && inventory.insertItem(0, new ItemStack(TinkerCommons.goldPlatform.asItem()), false).isEmpty(),
          "Actual scorched basin rejected its reusable gold platform");
      }
      helper.assertTrue(output.fill(fluid, FluidAction.SIMULATE) == FluidValues.METAL_BLOCK && output.getFluidInTank(0).isEmpty(),
        "Fluid-only native basin recipe missing or simulation mutated tank");
      helper.assertTrue(input.fill(fluid, FluidAction.EXECUTE) == FluidValues.METAL_BLOCK, "Source tank rejected molten metal");
      ((FaucetBlockEntity)level.getBlockEntity(helper.absolutePos(faucetPos))).activate();
    }
    helper.succeedWhen(() -> {
      for (int i = 0; i < basins.length; i++) {
        var basin = (CastingBlockEntity)level.getBlockEntity(helper.absolutePos(basins[i]));
        ItemStack output = basin.getItem(CastingBlockEntity.OUTPUT);
        helper.assertTrue(output.is(i % 2 == 1 ? Items.GOLD_BLOCK : Items.IRON_BLOCK) && output.getCount() == 1,
          "Actual faucet/basin has not produced exact iron/gold block in lane " + i);
        helper.assertTrue(basin.getTank().getFluidInTank(0).isEmpty(), "Casting retained consumed metal");
        if (i >= 2) helper.assertTrue(basin.getItem(CastingBlockEntity.INPUT).is(TinkerCommons.goldPlatform.asItem()), "Casting consumed reusable gold platform");
      }
      TConstruct.LOG.info("TiC actual faucet seared scorched iron gold block casting and reusable empty platform PASS");
    });
  }
}
