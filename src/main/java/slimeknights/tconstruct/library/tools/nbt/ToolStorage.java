package slimeknights.tconstruct.library.tools.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;

/** Immutable native item/fluid snapshots; no world or registry is needed to access their contents. */
public final class ToolStorage {
  public static final ToolStorage EMPTY = new ToolStorage(Map.of(), Map.of());
  private static final Codec<Map<ResourceLocation,FluidStack>> FLUID_CODEC =
    Codec.unboundedMap(ResourceLocation.CODEC, FluidStack.CODEC);
  private static final Codec<Map<Integer,ItemStack>> SLOT_CODEC =
    Codec.unboundedMap(Codec.STRING.comapFlatMap(value -> {
      try {
        int slot = Integer.parseInt(value);
        return slot < 0 ? com.mojang.serialization.DataResult.error(() -> "Negative tool slot " + value) : com.mojang.serialization.DataResult.success(slot);
      } catch (NumberFormatException exception) {
        return com.mojang.serialization.DataResult.error(() -> "Invalid tool slot " + value);
      }
    }, Object::toString), ItemStack.CODEC);
  public static final Codec<ToolStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    FLUID_CODEC.optionalFieldOf("fluids", Map.of()).forGetter(ToolStorage::fluidSnapshot),
    Codec.unboundedMap(ResourceLocation.CODEC, SLOT_CODEC).optionalFieldOf("inventories", Map.of()).forGetter(ToolStorage::inventorySnapshot)
  ).apply(instance, ToolStorage::new));
  private static final StreamCodec<RegistryFriendlyByteBuf,Map<ResourceLocation,FluidStack>> FLUID_STREAM =
    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, FluidStack.STREAM_CODEC);
  private static final StreamCodec<RegistryFriendlyByteBuf,Map<Integer,ItemStack>> SLOT_STREAM =
    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ItemStack.STREAM_CODEC);
  private static final StreamCodec<RegistryFriendlyByteBuf,Map<ResourceLocation,Map<Integer,ItemStack>>> INVENTORY_STREAM =
    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, SLOT_STREAM);
  public static final StreamCodec<RegistryFriendlyByteBuf,ToolStorage> STREAM_CODEC =
    StreamCodec.composite(FLUID_STREAM, ToolStorage::fluidSnapshot, INVENTORY_STREAM, ToolStorage::inventorySnapshot, ToolStorage::new);

  private final Map<ResourceLocation,FluidStack> fluids;
  private final Map<ResourceLocation,Map<Integer,ItemStack>> inventories;

  public ToolStorage(Map<ResourceLocation,FluidStack> fluids, Map<ResourceLocation,Map<Integer,ItemStack>> inventories) {
    Map<ResourceLocation,FluidStack> fluidCopy = new HashMap<>();
    fluids.forEach((key, value) -> { if (!value.isEmpty()) fluidCopy.put(key, value.copy()); });
    this.fluids = Map.copyOf(fluidCopy);
    Map<ResourceLocation,Map<Integer,ItemStack>> inventoryCopy = new HashMap<>();
    inventories.forEach((key, slots) -> {
      Map<Integer,ItemStack> slotCopy = copySlots(slots);
      if (!slotCopy.isEmpty()) inventoryCopy.put(key, Map.copyOf(slotCopy));
    });
    this.inventories = Map.copyOf(inventoryCopy);
  }

  private static Map<Integer,ItemStack> copySlots(Map<Integer,ItemStack> slots) {
    Map<Integer,ItemStack> copy = new HashMap<>();
    slots.forEach((slot, stack) -> {
      if (slot < 0) throw new IllegalArgumentException("Negative tool inventory slot " + slot);
      if (!stack.isEmpty()) copy.put(slot, stack.copy());
    });
    return copy;
  }

  private Map<ResourceLocation,FluidStack> fluidSnapshot() {
    Map<ResourceLocation,FluidStack> copy = new HashMap<>();
    fluids.forEach((key, fluid) -> copy.put(key, fluid.copy()));
    return copy;
  }

  private Map<ResourceLocation,Map<Integer,ItemStack>> inventorySnapshot() {
    Map<ResourceLocation,Map<Integer,ItemStack>> copy = new HashMap<>();
    inventories.forEach((key, slots) -> copy.put(key, copySlots(slots)));
    return copy;
  }

  public FluidStack fluid(ResourceLocation key) {
    FluidStack fluid = fluids.get(key);
    return fluid == null ? FluidStack.EMPTY : fluid.copy();
  }

  public Map<Integer,ItemStack> inventory(ResourceLocation key) {
    Map<Integer,ItemStack> slots = inventories.get(key);
    return slots == null ? new HashMap<>() : copySlots(slots);
  }

  public ToolStorage withFluid(ResourceLocation key, FluidStack fluid) {
    Map<ResourceLocation,FluidStack> copy = fluidSnapshot();
    if (fluid.isEmpty()) copy.remove(key);
    else copy.put(key, fluid);
    return new ToolStorage(copy, inventories);
  }

  public ToolStorage withInventory(ResourceLocation key, Map<Integer,ItemStack> slots) {
    Map<ResourceLocation,Map<Integer,ItemStack>> copy = inventorySnapshot();
    if (slots.isEmpty()) copy.remove(key);
    else copy.put(key, slots);
    return new ToolStorage(fluids, copy);
  }

  public boolean isEmpty() { return fluids.isEmpty() && inventories.isEmpty(); }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof ToolStorage storage) || !fluids.keySet().equals(storage.fluids.keySet())
        || !inventories.keySet().equals(storage.inventories.keySet())) return false;
    for (ResourceLocation key : fluids.keySet()) {
      if (!FluidStack.matches(fluids.get(key), storage.fluids.get(key))) return false;
    }
    for (ResourceLocation key : inventories.keySet()) {
      Map<Integer,ItemStack> slots = inventories.get(key), theirs = storage.inventories.get(key);
      if (!slots.keySet().equals(theirs.keySet())) return false;
      for (int slot : slots.keySet()) {
        if (!ItemStack.matches(slots.get(slot), theirs.get(slot))) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    for (var entry : fluids.entrySet()) {
      hash += entry.getKey().hashCode() ^ (31 * FluidStack.hashFluidAndComponents(entry.getValue()) + entry.getValue().getAmount());
    }
    for (var entry : inventories.entrySet()) {
      int slots = 0;
      for (var slot : entry.getValue().entrySet()) {
        slots += slot.getKey().hashCode() ^ (31 * ItemStack.hashItemAndComponents(slot.getValue()) + slot.getValue().getCount());
      }
      hash += entry.getKey().hashCode() ^ slots;
    }
    return hash;
  }
}
