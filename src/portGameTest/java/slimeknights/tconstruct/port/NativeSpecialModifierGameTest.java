package slimeknights.tconstruct.port;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.modules.capacity.OverslimeModule;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class NativeSpecialModifierGameTest {
  private static ToolStack ironPickaxe() {
    var material = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("tconstruct:pickaxe"));
    return ToolStack.createTool(item, ((IModifiable) item).getToolDefinition(),
      MaterialNBT.of(material, material, material));
  }

  private static ItemStack reload(GameTestHelper helper, ItemStack stack) {
    var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
    return ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, stack).getOrThrow()).getOrThrow();
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualOverslimeDamageAndPersistence(GameTestHelper helper) {
    var tool = ironPickaxe();
    tool.addModifier(new ModifierId("tconstruct:overslime"), 1);
    helper.assertTrue(OverslimeModule.getCapacity(tool) >= 30,
      "Actual overslime modifier did not create its material tool shield capacity");
    OverslimeModule.INSTANCE.addAmount(tool, 30);
    ToolDamageUtil.damage(tool, 7, null, null);
    helper.assertTrue(tool.getDamage() == 0 && OverslimeModule.INSTANCE.getAmount(tool) == 23,
      "Actual overslime hook did not absorb tool damage exactly once");
    var restored = ToolStack.from(reload(helper, tool.createStack()));
    helper.assertTrue(OverslimeModule.INSTANCE.getAmount(restored) == 23,
      "Native item reload lost the special modifier shield");
    ToolDamageUtil.damage(restored, 28, null, null);
    helper.assertTrue(OverslimeModule.INSTANCE.getAmount(restored) == 0 && restored.getDamage() == 5,
      "Actual shield exhaustion did not apply only the remaining durability damage");
    TConstruct.LOG.info("TiC actual overslime absorption exhaustion and persistence PASS");
    helper.succeed();
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualSpillingTankCapabilityAndPersistence(GameTestHelper helper) {
    var tool = ironPickaxe();
    tool.addModifier(new ModifierId("tconstruct:spilling"), 1);
    ItemStack stack = tool.createStack();
    var tank = stack.getCapability(Capabilities.FluidHandler.ITEM);
    helper.assertTrue(tank != null && tank.getTankCapacity(0) == 1000,
      "Actual spilling modifier did not expose its native fluid item capability");
    helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 750), FluidAction.SIMULATE) == 750 &&
      tank.getFluidInTank(0).isEmpty(), "Simulated special modifier filling changed storage");
    helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 750), FluidAction.EXECUTE) == 750,
      "Actual special modifier tank did not commit filling");
    ItemStack loaded = reload(helper, tank.getContainer());
    var restored = loaded.getCapability(Capabilities.FluidHandler.ITEM);
    helper.assertTrue(restored != null && restored.getFluidInTank(0).getAmount() == 750 &&
      restored.getFluidInTank(0).getFluid() == Fluids.WATER,
      "Native special modifier item reload lost fluid contents");
    helper.assertTrue(restored.drain(200, FluidAction.SIMULATE).getAmount() == 200 &&
      restored.getFluidInTank(0).getAmount() == 750, "Simulated special modifier draining changed storage");
    helper.assertTrue(restored.drain(200, FluidAction.EXECUTE).getAmount() == 200 &&
      restored.getFluidInTank(0).getAmount() == 550, "Actual special modifier draining lost the remainder");
    ToolStack.from(loaded).removeModifier(new ModifierId("tconstruct:spilling"), 1);
    helper.assertTrue(loaded.getCapability(Capabilities.FluidHandler.ITEM) == null,
      "Removing the last tank modifier retained a stale native capability");
    TConstruct.LOG.info("TiC actual spilling fluid capability simulation persistence and removal PASS");
    helper.succeed();
  }
}
