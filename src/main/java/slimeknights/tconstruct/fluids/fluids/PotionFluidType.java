package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.fluid.texture.ClientTextureFluidType;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.fluid.PotionData;

import java.util.Objects;
import java.util.function.Consumer;

public class PotionFluidType extends FluidType {
  public PotionFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public String getDescriptionId(FluidStack stack) {
    return Potion.getName(PotionData.get(stack).potion(), "item.minecraft.potion.effect.");
  }

  @Override
  public ItemStack getBucket(FluidStack fluidStack) {
    return PotionData.fill(new ItemStack(fluidStack.getFluid().getBucket()), fluidStack);
  }

  @Override
  public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
    consumer.accept(new ClientTextureFluidType(this) {
      @Override
      public int getTintColor(FluidStack stack) {
        PotionContents contents = PotionData.get(stack);
        if (contents.potion().isEmpty() && !contents.hasEffects() && contents.customColor().isEmpty()) {
          return getTintColor();
        }
        return contents.getColor() | 0xFF000000;
      }
    });
  }

  public static FluidStack potionFluid(ResourceKey<Potion> potion, int size) {
    return potionFluid(BuiltInRegistries.POTION.getHolderOrThrow(potion), size);
  }

  public static FluidStack potionFluid(Holder<Potion> potion, int size) {
    FluidStack stack = new FluidStack(TinkerFluids.potion.get(), size);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Compatibility for callers holding a registry value rather than its holder. */
  public static FluidStack potionFluid(Potion potion, int size) {
    return potionFluid(BuiltInRegistries.POTION.wrapAsHolder(potion), size);
  }

  public static FluidOutput potionResult(Holder<Potion> potion, int size) {
    DataComponentPatch components = DataComponentPatch.builder()
      .set(DataComponents.POTION_CONTENTS, new PotionContents(potion)).build();
    return FluidOutput.fromTagWithComponents(Objects.requireNonNull(TinkerFluids.potion.getCommonTag()), size, components);
  }

  public static FluidOutput potionResult(Potion potion, int size) {
    return potionResult(BuiltInRegistries.POTION.wrapAsHolder(potion), size);
  }

  public static ItemStack potionBucket(ResourceKey<Potion> potion) {
    return potionBucket(BuiltInRegistries.POTION.getHolderOrThrow(potion));
  }

  public static ItemStack potionBucket(Holder<Potion> potion) {
    return PotionContents.createItemStack(TinkerFluids.potion.asItem(), potion);
  }

  public static ItemStack potionBucket(Potion potion) {
    return potionBucket(BuiltInRegistries.POTION.wrapAsHolder(potion));
  }
}
