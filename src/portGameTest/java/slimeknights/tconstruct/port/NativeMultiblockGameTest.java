package slimeknights.tconstruct.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class NativeMultiblockGameTest {
  @GameTest(template = "smeltery", timeoutTicks = 1800)
  public static void actualSmelteryAlloyReloadAndReformation(GameTestHelper helper) {
    actualStructure(helper, false);
  }

  @GameTest(template = "smeltery", timeoutTicks = 1800)
  public static void actualFoundryReloadAndReformation(GameTestHelper helper) {
    actualStructure(helper, true);
  }

  private static void actualStructure(GameTestHelper helper, boolean foundry) {
    var level = helper.getLevel();
    var brick = foundry ? TinkerSmeltery.scorchedBricks.get() : TinkerSmeltery.searedBricks.get();
    for (int x = 1; x <= 3; x++) {
      for (int z = 1; z <= 3; z++) {
        helper.setBlock(new BlockPos(x, 1, z), brick);
        if (x != 2 || z != 2) helper.setBlock(new BlockPos(x, 2, z), brick);
      }
    }
    BlockPos controllerPos = helper.absolutePos(new BlockPos(2, 2, 1));
    BlockPos fuelPos = helper.absolutePos(new BlockPos(1, 2, 2));
    BlockPos drainPos = helper.absolutePos(new BlockPos(3, 2, 2));
    BlockPos wallPos = helper.absolutePos(new BlockPos(2, 2, 3));
    var block = foundry ? TinkerSmeltery.foundryController.get() : TinkerSmeltery.smelteryController.get();
    level.setBlockAndUpdate(controllerPos, block.defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    level.setBlockAndUpdate(fuelPos, (foundry ? TinkerSmeltery.scorchedTank : TinkerSmeltery.searedTank)
      .get(TankType.FUEL_TANK).defaultBlockState());
    level.setBlockAndUpdate(drainPos, (foundry ? TinkerSmeltery.scorchedDrain : TinkerSmeltery.searedDrain)
      .get().defaultBlockState());
    int[] phase = {0};
    IFluidHandler[] proxy = {null};
    helper.succeedWhen(() -> {
      var controller = (HeatingStructureBlockEntity) level.getBlockEntity(controllerPos);
      if (phase[0] == 0) {
        helper.assertTrue(level.getBlockState(controllerPos).getValue(ControllerBlock.IN_STRUCTURE),
          "Native world ticking has not formed the actual " + (foundry ? "foundry" : "smeltery"));
        var input = level.getCapability(Capabilities.ItemHandler.BLOCK, controllerPos, Direction.UP);
        var fuel = level.getCapability(Capabilities.FluidHandler.BLOCK, fuelPos, Direction.UP);
        proxy[0] = level.getCapability(Capabilities.FluidHandler.BLOCK, drainPos, Direction.UP);
        helper.assertTrue(input != null && fuel != null && proxy[0] != null && input.getSlots() == 1,
          "Actual formed structure did not expose its native capacities and IO");
        helper.assertTrue(fuel.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE) == 1000 &&
          input.insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty(),
          "Actual formed structure rejected fuel or iron input");
        phase[0] = 1;
      }
      if (phase[0] == 1) {
        helper.assertTrue(proxy[0].getFluidInTank(0).getAmount() == FluidValues.INGOT &&
          controller.getMeltingInventory().getStackInSlot(0).isEmpty(),
          "Actual native structure heating has not melted its iron input");
        var saved = controller.saveWithFullMetadata(level.registryAccess());
        var state = level.getBlockState(controllerPos);
        level.removeBlockEntity(controllerPos);
        BlockEntity loaded = BlockEntity.loadStatic(controllerPos, state, saved, level.registryAccess());
        helper.assertTrue(loaded instanceof HeatingStructureBlockEntity,
          "Native persistent controller decode did not restore its actual registered type");
        level.setBlockEntity(loaded);
        controller = (HeatingStructureBlockEntity) loaded;
        helper.assertTrue(controller.getStructure() != null &&
          controller.getMeltingInventory().getSlots() == 1 &&
          proxy[0].getFluidInTank(0).getAmount() == FluidValues.INGOT,
          "Native controller replacement lost persisted structure, inventory capacity or live drain forwarding");
        helper.assertTrue(proxy[0].drain(FluidValues.INGOT, FluidAction.EXECUTE).getAmount() == FluidValues.INGOT,
          "Retained real drain proxy did not resolve the loaded controller");
        if (!foundry) {
          helper.assertTrue(proxy[0].fill(new FluidStack(TinkerFluids.moltenCopper.get(), FluidValues.INGOT),
            FluidAction.EXECUTE) == FluidValues.INGOT &&
            proxy[0].fill(new FluidStack(TinkerFluids.moltenGold.get(), FluidValues.INGOT),
            FluidAction.EXECUTE) == FluidValues.INGOT,
            "Actual smeltery IO did not accept real alloy inputs");
        }
        phase[0] = 2;
      }
      if (phase[0] == 2) {
        if (!foundry) {
          FluidStack alloy = proxy[0].drain(new FluidStack(TinkerFluids.moltenRoseGold.get(), FluidValues.INGOT * 2),
            FluidAction.SIMULATE);
          helper.assertTrue(alloy.getAmount() == FluidValues.INGOT * 2,
            "Actual heated smeltery has not alloyed copper and gold into exact rose gold");
          helper.assertTrue(proxy[0].drain(alloy, FluidAction.EXECUTE).getAmount() == alloy.getAmount(),
            "Actual alloy output extraction changed its amount");
        }
        level.setBlockAndUpdate(wallPos, Blocks.AIR.defaultBlockState());
        phase[0] = 3;
      }
      if (phase[0] == 3) {
        helper.assertTrue(!level.getBlockState(controllerPos).getValue(ControllerBlock.IN_STRUCTURE),
          "Native servant neighbor notifications have not invalidated the broken structure");
        helper.assertTrue(proxy[0].fill(new FluidStack(TinkerFluids.moltenIron.get(), FluidValues.INGOT),
          FluidAction.EXECUTE) == 0, "Retained drain proxy accepted fluid into an invalid structure");
        level.setBlockAndUpdate(wallPos, brick.defaultBlockState());
        phase[0] = 4;
      }
      helper.assertTrue(level.getBlockState(controllerPos).getValue(ControllerBlock.IN_STRUCTURE),
        "Native ticking has not reformed the restored structure");
      helper.assertTrue(proxy[0].fill(new FluidStack(TinkerFluids.moltenIron.get(), FluidValues.INGOT),
        FluidAction.SIMULATE) == FluidValues.INGOT && proxy[0].getFluidInTank(0).isEmpty(),
        "Retained drain proxy did not recover safely after real structure reformation");
      TConstruct.LOG.info("TiC actual {} structure heating reload forwarding and reformation PASS",
        foundry ? "foundry" : "smeltery alloy");
    });
  }
}
