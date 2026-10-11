package slimeknights.tconstruct.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.SmelteryTank;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class LegacySmelteryTankGameTest {
  @GameTest(template = "bootstrap", timeoutTicks = 100)
  public static void legacyAndNativeMultifluidReload(GameTestHelper helper) {
    var registries = helper.getLevel().registryAccess();
    CompoundTag metadata = new CompoundTag();
    metadata.putString("exploration_origin", "legacy-book-structure");
    CompoundTag iron = new CompoundTag();
    iron.putString("FluidName", "tconstruct:molten_iron");
    iron.putInt("Amount", 144);
    iron.put("Tag", metadata.copy());
    FluidStack gold = new FluidStack(TinkerFluids.moltenGold.get(), 810);
    gold.set(DataComponents.CUSTOM_DATA, CustomData.of(metadata.copy()));
    ListTag fluids = new ListTag();
    fluids.add(iron);
    fluids.add(gold.save(registries));
    CompoundTag input = new CompoundTag();
    input.put("fluids", fluids);
    input.putInt("capacity", 2000);
    CompoundTag original = input.copy();
    helper.setBlock(BlockPos.ZERO, TinkerSmeltery.smelteryController.get());
    helper.setBlock(new BlockPos(1, 0, 0), TinkerSmeltery.foundryController.get());
    SmelteryTank<?>[] tanks = {
      ((SmelteryBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(BlockPos.ZERO))).getTank(),
      ((FoundryBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 0, 0)))).getTank()
    };
    for (SmelteryTank<?> tank : tanks) {
      tank.read(input, registries);
      helper.assertTrue(tank.getFluids().size() == 2 && tank.getContained() == 954 && tank.getCapacity() == 2000,
        "Legacy/native mixed tank lost fluids, amount, or capacity");
      helper.assertTrue(tank.getFluidInTank(0).getFluid() == TinkerFluids.moltenIron.get()
        && tank.getFluidInTank(0).getAmount() == 144, "Legacy book iron did not survive loading");
      helper.assertTrue(FluidStack.matches(tank.getFluidInTank(1), gold), "Native gold components changed");
      helper.assertTrue(tank.getFluidInTank(0).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().equals(metadata),
        "Legacy fluid metadata was lost");
      FluidStack expectedIron = tank.getFluidInTank(0).copy();
      CompoundTag saved = tank.write(new CompoundTag(), registries);
      helper.assertTrue(!saved.getList("fluids", 10).getCompound(0).contains("FluidName"), "Save retained old fluid format");
      tank.read(saved, registries);
      helper.assertTrue(tank.getContained() == 954 && FluidStack.matches(tank.getFluidInTank(0), expectedIron) && FluidStack.matches(tank.getFluidInTank(1), gold), "Native reload regressed");
    }
    helper.assertTrue(input.equals(original), "Loading mutated legacy input");
    TConstruct.LOG.info("TiC actual smeltery foundry mixed legacy native fluids amounts components and native save reload PASS");
    helper.succeed();
  }
}
