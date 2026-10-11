package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.library.fluid.FluidItemData;
import slimeknights.tconstruct.library.fluid.StoredFluid;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluid container holding 1 ingot of fluid
 */
public class CopperCanItem extends Item {
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FLUID_TAG = "fluid_tag";

  public CopperCanItem(Properties properties) {
    super(properties);
  }

  @Override
  public boolean hasCraftingRemainingItem(ItemStack stack) {
    return getFluid(stack) != Fluids.EMPTY;
  }

  @Override
  public ItemStack getCraftingRemainingItem(ItemStack stack) {
    if (hasCraftingRemainingItem(stack)) {
      return new ItemStack(this);
    }
    return ItemStack.EMPTY;
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    Fluid fluid = getFluid(stack);
    if (fluid != Fluids.EMPTY) {
      MutableComponent text = getFluidStack(stack).getHoverName().plainCopy();
      tooltip.add(Component.translatable(this.getDescriptionId() + ".contents", text).withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        tooltip.add(Component.translatable(TankItem.FLUID_ID, Loadables.FLUID.getKey(fluid)).withStyle(ChatFormatting.DARK_GRAY));
      }
    } else {
      tooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
  }

  /** Removes the fluid from the given stack */
  public static void removeFluid(ItemStack stack) {
    FluidItemData.set(stack, FluidStack.EMPTY, TAG_FLUID, TAG_FLUID_TAG);
  }

  /** Sets the fluid on the given stack whether or not its valiid */
  private static void setFluidInternal(ItemStack stack, ResourceLocation fluid, @Nullable CompoundTag fluidTag) {
    FluidStack contents = StoredFluid.readLegacy(fluid.toString(), FluidValues.INGOT, fluidTag == null ? new CompoundTag() : fluidTag);
    if (!contents.isEmpty()) {
      setFluid(stack, contents);
    } else {
      // Retain unresolved IDs in legacy custom data, matching the old unvalidated setter.
      CompoundTag legacy = FluidItemData.legacyData(stack);
      legacy.putString(TAG_FLUID, fluid.toString());
      if (fluidTag != null) {
        legacy.put(TAG_FLUID_TAG, fluidTag.copy());
      } else {
        legacy.remove(TAG_FLUID_TAG);
      }
      stack.remove(FluidItemData.CONTENTS.get());
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));
    }
  }


  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, ResourceLocation fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid.equals(BuiltInRegistries.FLUID.getDefaultKey())) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, fluid, fluidTag);
    }
    return stack;
  }
  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, Fluid fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid == Fluids.EMPTY) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, BuiltInRegistries.FLUID.getKey(fluid), fluidTag);
    }
    return stack;
  }

  /** Sets the fluid on the given stack */
  public static ItemStack setFluid(ItemStack stack, FluidStack fluid) {
    return FluidItemData.set(stack, fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(FluidValues.INGOT), TAG_FLUID, TAG_FLUID_TAG);
  }

  /** Gets the fluid from the given stack */
  public static Fluid getFluid(ItemStack stack) {
    return getFluidStack(stack).getFluid();
  }

  /** Fresh per-item fluid stack, including its complete component patch. */
  public static FluidStack getFluidStack(ItemStack stack) {
    return FluidItemData.getCan(stack, TAG_FLUID, TAG_FLUID_TAG, FluidValues.INGOT);
  }

  /** Adds filled variants of the copper can to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    BuiltInRegistries.FLUID.holders().filter(holder -> {
      Fluid fluid = holder.value();
      return fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS);
    }).forEachOrdered(holder -> {
      output.accept(CopperCanItem.setFluid(new ItemStack(TinkerSmeltery.copperCan), holder.key().location(), null));
    });
  }

  /** Gets the fluid NBT from the given stack */
  @Nullable
  public static CompoundTag getFluidTag(ItemStack stack) {
    CustomData metadata = getFluidStack(stack).get(DataComponents.CUSTOM_DATA);
    return metadata == null || metadata.isEmpty() ? null : metadata.copyTag();
  }

  /**
   * Gets a string variant name for the given stack
   * @param stack  Stack instance to check
   * @return  String variant name
   */
  public static String getSubtype(ItemStack stack) {
    Fluid fluid = getFluid(stack);
    return fluid == Fluids.EMPTY ? FluidItemData.legacyData(stack).getString(TAG_FLUID) : BuiltInRegistries.FLUID.getKey(fluid).toString();
  }
}
