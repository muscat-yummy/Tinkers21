package slimeknights.tconstruct.library.fluid;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/** Immutable item-component snapshot; mutable FluidStacks are recreated on access. */
public record StoredFluid(Holder<Fluid> fluid, int amount, DataComponentPatch components) {
  public static final Codec<StoredFluid> CODEC = FluidStack.CODEC.xmap(StoredFluid::of, StoredFluid::toStack);
  public static final StreamCodec<RegistryFriendlyByteBuf,StoredFluid> STREAM_CODEC =
    FluidStack.STREAM_CODEC.map(StoredFluid::of, StoredFluid::toStack);

  public StoredFluid {
    Objects.requireNonNull(fluid);
    Objects.requireNonNull(components);
    if (fluid.value() == Fluids.EMPTY || amount <= 0) {
      throw new IllegalArgumentException("Stored fluid must be nonempty");
    }
  }

  public static StoredFluid of(FluidStack stack) {
    return new StoredFluid(stack.getFluidHolder(), stack.getAmount(), stack.getComponentsPatch());
  }

  public FluidStack toStack() {
    return new FluidStack(fluid, amount, components);
  }

  /** Read the Forge 1.20 flat format without treating arbitrary legacy NBT as native components. */
  public static FluidStack readLegacy(String id, int amount, CompoundTag metadata) {
    ResourceLocation key = ResourceLocation.tryParse(id);
    if (key == null || amount <= 0 || !BuiltInRegistries.FLUID.containsKey(key)) {
      return FluidStack.EMPTY;
    }
    Fluid value = BuiltInRegistries.FLUID.get(key);
    if (value == Fluids.EMPTY) {
      return FluidStack.EMPTY;
    }
    FluidStack result = new FluidStack(value, amount);
    if (!metadata.isEmpty()) {
      result.set(DataComponents.CUSTOM_DATA, CustomData.of(metadata.copy()));
    }
    return result;
  }

  public static FluidStack readLegacyTank(CompoundTag tag) {
    return readLegacy(tag.getString("FluidName"), tag.getInt("Amount"), tag.getCompound("Tag"));
  }

  /** Accept legacy flat tanks and native provider-aware tank/stack formats. */
  public static FluidStack readTank(CompoundTag tag, HolderLookup.Provider registries) {
    if (tag.contains("FluidName", Tag.TAG_STRING)) {
      return readLegacyTank(tag);
    }
    return FluidStack.parseOptional(registries, tag.contains("Fluid", Tag.TAG_COMPOUND) ? tag.getCompound("Fluid") : tag);
  }
}
