package slimeknights.tconstruct.port;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.container.IEmptyContainer;
import slimeknights.mantle.recipe.container.IRecipeContainer;
import slimeknights.mantle.recipe.container.ISingleStackContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;

public final class RecipeInputSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    ItemStack[] slots = {ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT)};
    IRecipeContainer view = new IRecipeContainer() {
      @Override public int getContainerSize() { return slots.length; }
      @Override public ItemStack getItem(int index) { return slots[index]; }
    };
    RecipeInput nativeInput = view;
    check(nativeInput.size() == 2 && nativeInput.getItem(1) == slots[1] && !nativeInput.isEmpty(), "Native input did not retain original slot view");
    slots[1] = ItemStack.EMPTY;
    check(nativeInput.isEmpty(), "Native emptiness did not follow mutable backing inventory");
    check(IEmptyContainer.EMPTY instanceof RecipeInput && IEmptyContainer.EMPTY.size() == 0 && IEmptyContainer.EMPTY.isEmpty(), "Empty view did not retain zero slots");
    ISingleStackContainer single = () -> slots[0];
    slots[0] = new ItemStack(Items.GOLD_INGOT, 3);
    check(single.size() == 1 && single.getItem(0) == slots[0] && !single.isEmpty(), "Single-stack native input changed slot semantics");
    boolean rejected = false;
    try { view.setItem(0, new ItemStack(Items.DIAMOND)); } catch (UnsupportedOperationException expected) { rejected = true; }
    check(rejected && slots[0].is(Items.GOLD_INGOT), "Native bridge made read-only input mutable");

    net.minecraft.nbt.CompoundTag oldData = new net.minecraft.nbt.CompoundTag();
    oldData.putString("fixture", "legacy");
    ICastingContainer legacy = new ICastingContainer() {
      @Override public ItemStack getStack() { return ItemStack.EMPTY; }
      @Override public net.minecraft.world.level.material.Fluid getFluid() { return Fluids.WATER; }
      @Override public net.minecraft.nbt.CompoundTag getFluidTag() { return oldData; }
    };
    FluidStack fluid = legacy.getFluidStack();
    oldData.putString("fixture", "changed");
    check(fluid.get(DataComponents.CUSTOM_DATA).copyTag().getString("fixture").equals("legacy"), "Legacy casting metadata was aliased");
    FluidStack modern = new FluidStack(Fluids.WATER, 600);
    modern.set(DataComponents.CUSTOM_NAME, Component.literal("Casting input"));
    ICastingContainer componentInput = new ICastingContainer() {
      @Override public ItemStack getStack() { return slots[0]; }
      @Override public net.minecraft.world.level.material.Fluid getFluid() { return modern.getFluid(); }
      @Override public FluidStack getFluidStack() { return modern.copy(); }
    };
    FluidStack filled = componentInput.getFluidStack().copyWithAmount(1000);
    check(filled.getAmount() == 1000 && filled.get(DataComponents.CUSTOM_NAME).equals(modern.get(DataComponents.CUSTOM_NAME)), "Native casting input lost component patches");
    check(modern.getAmount() == 600, "Casting input modified its backing fluid");
    System.out.println("Native read-only recipe inputs and casting component snapshots: PASS");
  }
}
