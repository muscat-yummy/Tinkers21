package slimeknights.tconstruct.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.mantle.datagen.MantleTags;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.casting.TipClearingCastingRecipe;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

/** Native tags and actual water casting, not just a display-list assertion. */
@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class MantleFluidTagGameTest {
  @GameTest(template="smeltery",timeoutTicks=300)
  public static void actualNativeWaterLavaTagsAndTipClearing(GameTestHelper helper) {
    var level=helper.getLevel();
    helper.assertTrue(Fluids.WATER.is(MantleTags.Fluids.WATER) && Fluids.FLOWING_WATER.is(MantleTags.Fluids.WATER),"Mantle native water tag missing");
    helper.assertTrue(Fluids.LAVA.is(MantleTags.Fluids.LAVA) && Fluids.FLOWING_LAVA.is(MantleTags.Fluids.LAVA),"Mantle native lava tag missing");
    String[] paths={"tools/modifiers/slotless/ammo_tip_clearing","tools/modifiers/slotless/fishing_rod_tip_clearing"};
    TipClearingCastingRecipe[] recipes=new TipClearingCastingRecipe[2];
    for(int i=0;i<2;i++) {
      recipes[i]=(TipClearingCastingRecipe)level.getRecipeManager().byKey(TConstruct.getResource(paths[i])).orElseThrow().value();
      var displays=recipes[i].getRecipes(level.registryAccess());
      helper.assertTrue(!displays.isEmpty(),"Tip-clearing recipe has no native fluid display");
      ItemStack cast=displays.get(0).getCastItems(ItemStack.EMPTY,false).get(0).copy();
      helper.assertTrue(!ModifierUtil.getPersistentString(cast,recipes[i].getModifier()).isEmpty(),"Test tool has no potion");
      BlockPos pos=new BlockPos(i*2,1,1);
      helper.setBlock(pos,TinkerSmeltery.searedTable.get());
      var inventory=level.getCapability(Capabilities.ItemHandler.BLOCK,helper.absolutePos(pos),Direction.UP);
      var tank=level.getCapability(Capabilities.FluidHandler.BLOCK,helper.absolutePos(pos),Direction.UP);
      helper.assertTrue(inventory!=null && tank!=null && inventory.insertItem(0,cast,false).isEmpty(),"Actual table rejected tipped tool");
      int amount=i==0?50:250;
      FluidStack water=new FluidStack(Fluids.WATER,amount);
      helper.assertTrue(tank.fill(water,FluidAction.SIMULATE)==amount && tank.getFluidInTank(0).isEmpty(),"Water recipe missing or simulation mutated table");
      helper.assertTrue(tank.fill(water,FluidAction.EXECUTE)==amount,"Actual casting table rejected water");
    }
    helper.succeedWhen(()->{
      for(int i=0;i<2;i++) {
        var table=(CastingBlockEntity)level.getBlockEntity(helper.absolutePos(new BlockPos(i*2,1,1)));
        ItemStack result=table.getItem(CastingBlockEntity.OUTPUT);
        helper.assertTrue(!result.isEmpty() && ModifierUtil.getPersistentString(result,recipes[i].getModifier()).isEmpty(),"Water casting did not remove the actual potion effect");
        helper.assertTrue(table.getTank().getFluidInTank(0).isEmpty(),"Water was not consumed");
      }
      TConstruct.LOG.info("TiC actual native Mantle water lava tags and ammo fishing rod water tip clearing PASS");
    });
  }
}
