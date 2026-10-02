package slimeknights.tconstruct.library.fluid;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native fluid contents shared by tanks and copper cans. Amount is always per item. */
public final class FluidItemData {
  private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
    DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "tconstruct");
  public static final DeferredHolder<DataComponentType<?>,DataComponentType<StoredFluid>> CONTENTS =
    COMPONENTS.register("fluid_contents", () -> DataComponentType.<StoredFluid>builder()
      .persistent(StoredFluid.CODEC).networkSynchronized(StoredFluid.STREAM_CODEC).build());

  private FluidItemData() {}

  public static void register(IEventBus bus) {
    COMPONENTS.register(bus);
  }

  public static CompoundTag legacyData(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  public static FluidStack getTank(ItemStack stack, String tankKey) {
    StoredFluid value = stack.get(CONTENTS.get());
    return value != null ? value.toStack() : StoredFluid.readLegacyTank(legacyData(stack).getCompound(tankKey));
  }

  public static FluidStack getCan(ItemStack stack, String fluidKey, String metadataKey, int amount) {
    StoredFluid value = stack.get(CONTENTS.get());
    if (value != null) {
      return value.toStack().copyWithAmount(amount);
    }
    CompoundTag legacy = legacyData(stack);
    return StoredFluid.readLegacy(legacy.getString(fluidKey), amount, legacy.getCompound(metadataKey));
  }

  public static ItemStack set(ItemStack stack, FluidStack fluid, String... legacyKeys) {
    if (fluid.isEmpty()) {
      stack.remove(CONTENTS.get());
    } else {
      stack.set(CONTENTS.get(), StoredFluid.of(fluid));
    }
    CompoundTag legacy = legacyData(stack);
    for (String key : legacyKeys) {
      legacy.remove(key);
    }
    if (legacy.isEmpty()) {
      stack.remove(DataComponents.CUSTOM_DATA);
    } else {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));
    }
    return stack;
  }
}
