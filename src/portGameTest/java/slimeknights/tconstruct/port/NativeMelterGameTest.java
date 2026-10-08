package slimeknights.tconstruct.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class NativeMelterGameTest {
  @GameTest(template = "smeltery", timeoutTicks = 1600)
  public static void actualFuelMeltingAndCasting(GameTestHelper helper) {
    BlockPos melterPos = new BlockPos(1, 2, 1);
    BlockPos fuelPos = melterPos.below();
    BlockPos tablePos = new BlockPos(3, 1, 1);
    BlockPos heaterPos = new BlockPos(4, 1, 3);
    // The lower block update forms the real controller; no structure flag is forced.
    helper.setBlock(melterPos, TinkerSmeltery.searedMelter.get());
    helper.setBlock(fuelPos, TinkerSmeltery.searedTank.get(TankType.FUEL_TANK));
    helper.setBlock(tablePos, TinkerSmeltery.searedTable.get());
    helper.setBlock(heaterPos, TinkerSmeltery.searedHeater.get());
    var level = helper.getLevel();
    ItemStack coal = new ItemStack(Items.COAL, 3);
    coal.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Persistent actual heater fuel"));
    var heaterInput = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(heaterPos), Direction.UP);
    helper.assertTrue(heaterInput != null && heaterInput.insertItem(0, coal, false).isEmpty(),
      "Actual placed heater rejected real coal");
    var heater = level.getBlockEntity(helper.absolutePos(heaterPos));
    heater.loadWithComponents(heater.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
    helper.assertTrue(ItemStack.matches(coal, heaterInput.getStackInSlot(0)),
      "Actual native heater persistence lost fuel count or components");
    var metadata = new net.minecraft.nbt.CompoundTag();
    metadata.putInt("retained_metadata", 42);
    var encoded = slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.writeStack(coal, 7, metadata, level.registryAccess());
    helper.assertTrue(encoded == metadata && metadata.getInt("retained_metadata") == 42 &&
      metadata.getInt("slot") == 7 && ItemStack.matches(coal, ItemStack.parseOptional(level.registryAccess(), metadata)),
      "Actual native inventory helper lost its original compound, metadata, slot or stack components");
    var fuel = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(fuelPos), Direction.UP);
    var input = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(melterPos), Direction.UP);
    var molten = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(melterPos), Direction.DOWN);
    var casting = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(tablePos), Direction.UP);
    var castInput = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(tablePos), Direction.UP);
    helper.assertTrue(fuel != null && input != null && molten != null && casting != null && castInput != null,
      "Actual placed machines did not expose native item/fluid capabilities");
    helper.assertTrue(level.getBlockState(helper.absolutePos(melterPos)).getValue(ControllerBlock.IN_STRUCTURE),
      "Actual neighbor update did not form the melter");
    helper.assertTrue(fuel.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE) == 1000 &&
      fuel.getFluidInTank(0).isEmpty(), "Native fuel insertion simulation mutated the actual tank");
    helper.assertTrue(fuel.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE) == 1000,
      "Actual fuel tank rejected native lava");
    helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), true).isEmpty() &&
      input.getStackInSlot(0).isEmpty(), "Native melting input simulation mutated the actual inventory");
    helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty(),
      "Actual melter rejected its iron input");
    ItemStack cast = new ItemStack(TinkerSmeltery.ingotCast.asItem());
    helper.assertTrue(castInput.insertItem(0, cast, false).isEmpty(), "Actual casting table rejected its ingot cast");
    // Reload actual placed machines through native persistent data before heating begins.
    for (BlockPos pos : new BlockPos[]{melterPos, fuelPos, tablePos}) {
      var entity = level.getBlockEntity(helper.absolutePos(pos));
      var saved = entity.saveWithFullMetadata(level.registryAccess());
      entity.loadWithComponents(saved, level.registryAccess());
    }
    helper.assertTrue(fuel.getFluidInTank(0).getAmount() == 1000 &&
      input.getStackInSlot(0).getItem() == Items.IRON_INGOT &&
      castInput.getStackInSlot(0).getItem() == cast.getItem(),
      "Native machine persistence lost real fluid or inventory inputs");
    boolean[] transferred = {false};
    helper.succeedWhen(() -> {
      if (!transferred[0]) {
        helper.assertTrue(molten.getFluidInTank(0).getAmount() == FluidValues.INGOT &&
          input.getStackInSlot(0).isEmpty(), "Actual native fuel heating has not completed iron melting");
        FluidStack preview = molten.drain(FluidValues.INGOT, FluidAction.SIMULATE);
        helper.assertTrue(preview.getAmount() == FluidValues.INGOT &&
          molten.getFluidInTank(0).getAmount() == FluidValues.INGOT,
          "Native molten output simulation changed the actual amount");
        helper.assertTrue(casting.fill(preview, FluidAction.SIMULATE) == FluidValues.INGOT &&
          casting.getFluidInTank(0).isEmpty(), "Native casting simulation started or mutated actual casting");
        FluidStack drained = molten.drain(FluidValues.INGOT, FluidAction.EXECUTE);
        helper.assertTrue(casting.fill(drained, FluidAction.EXECUTE) == FluidValues.INGOT &&
          molten.getFluidInTank(0).isEmpty(), "Actual molten transfer did not preserve exact fluid amount");
        var entity = level.getBlockEntity(helper.absolutePos(tablePos));
        var saved = entity.saveWithFullMetadata(level.registryAccess());
        entity.loadWithComponents(saved, level.registryAccess());
        helper.assertTrue(casting.getFluidInTank(0).getAmount() == FluidValues.INGOT,
          "Native in-progress casting persistence lost actual molten fluid");
        transferred[0] = true;
      }
      var table = (CastingBlockEntity) level.getBlockEntity(helper.absolutePos(tablePos));
      helper.assertTrue(table.getItem(CastingBlockEntity.OUTPUT).getItem() == Items.IRON_INGOT &&
        table.getItem(CastingBlockEntity.OUTPUT).getCount() == 1,
        "Actual native casting/cooling has not produced one iron ingot");
      helper.assertTrue(table.getItem(CastingBlockEntity.INPUT).getItem() == cast.getItem() &&
        casting.getFluidInTank(0).isEmpty(), "Actual casting lost its reusable cast or retained consumed fluid");
      helper.assertTrue(fuel.getFluidInTank(0).getAmount() < 1000, "Actual melter did not consume real fuel");
      slimeknights.tconstruct.TConstruct.LOG.info("TiC actual native melter fuel casting and machine persistence PASS");
    });
  }
}
