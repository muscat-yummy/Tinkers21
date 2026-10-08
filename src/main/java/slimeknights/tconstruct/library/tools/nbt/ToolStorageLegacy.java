package slimeknights.tconstruct.library.tools.nbt;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.common.LegacyItemData;

import java.util.HashMap;
import java.util.Map;

/** Legacy nested storage is converted only inside an actual registry-aware item decoding boundary. */
public final class ToolStorageLegacy {
  private ToolStorageLegacy() {}
  public record Migration(ToolStorage storage, CompoundTag custom, boolean changed) {}

  private static DynamicOps<JsonElement> jsonOps(DynamicOps<?> ops) {
    if (!(ops instanceof RegistryOps<?> registryOps)) {
      throw new IllegalStateException("Legacy tool storage requires actual registry-aware item decoding");
    }
    return registryOps.withParent(JsonOps.INSTANCE);
  }

  public static <T> Migration read(CompoundTag custom, ToolStorage storage, DynamicOps<T> ops) {
    if (!custom.contains("tic_persistent", Tag.TAG_COMPOUND)) return new Migration(storage, custom, false);
    CompoundTag copy = custom.copy();
    CompoundTag persistent = copy.getCompound("tic_persistent");
    boolean changed = false;
    for (String name : java.util.Set.copyOf(persistent.getAllKeys())) {
      ResourceLocation key = ResourceLocation.tryParse(name);
      if (key == null) continue;
      if (persistent.contains(name, Tag.TAG_COMPOUND)) {
        CompoundTag fluid = persistent.getCompound(name);
        if (fluid.contains("FluidName", Tag.TAG_STRING) && fluid.contains("Amount", Tag.TAG_ANY_NUMERIC)) {
          if (storage.fluid(key).isEmpty()) storage = storage.withFluid(key, readFluid(fluid, ops));
          persistent.remove(name);
          changed = true;
        }
      } else if (persistent.contains(name, Tag.TAG_LIST)) {
        ListTag list = persistent.getList(name, Tag.TAG_COMPOUND);
        Map<Integer,ItemStack> inventory = storage.inventory(key);
        for (int index = 0; index < list.size(); index++) {
          CompoundTag entry = list.getCompound(index);
          if (!entry.contains("Slot", Tag.TAG_ANY_NUMERIC) || !entry.contains("id", Tag.TAG_STRING)) continue;
          int slot = entry.getInt("Slot");
          if (slot < 0) throw new IllegalArgumentException("Negative legacy inventory slot " + slot);
          ItemStack item;
          if (entry.contains("Count", Tag.TAG_ANY_NUMERIC)) {
            ResourceLocation itemId = ResourceLocation.parse(entry.getString("id"));
            var holder = BuiltInRegistries.ITEM.getHolder(itemId)
              .orElseThrow(() -> new IllegalArgumentException("Unknown legacy inventory item " + itemId));
            int count = entry.getInt("Count");
            if (count < 0) count = entry.getByte("Count") & 255;
            item = new ItemStack(holder, count);
            if (entry.contains("tag", Tag.TAG_COMPOUND)) {
              item.applyComponents(LegacyItemData.convert(item.getItem(), entry.getCompound("tag"), jsonOps(ops)));
            }
          } else {
            item = ItemStack.CODEC.parse(ops, NbtOps.INSTANCE.convertTo(ops, entry)).getOrThrow();
          }
          if (!item.isEmpty()) item = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, item).getOrThrow()).getOrThrow();
          inventory.putIfAbsent(slot, item);
          entry.remove("id");
          entry.remove("Count");
          entry.remove("count");
          entry.remove("tag");
          entry.remove("components");
          changed = true;
        }
        storage = storage.withInventory(key, inventory);
      }
    }
    ResourceLocation smashing = ResourceLocation.parse("tconstruct:smashing_fluid");
    if (persistent.contains(smashing.toString(), Tag.TAG_STRING)
        && persistent.contains("tconstruct:smashing_fluid_tag", Tag.TAG_COMPOUND)) {
      CompoundTag fluid = new CompoundTag();
      fluid.putString("FluidName", persistent.getString(smashing.toString()));
      fluid.putInt("Amount", 1);
      fluid.put("Tag", persistent.getCompound("tconstruct:smashing_fluid_tag"));
      if (storage.fluid(smashing).isEmpty()) storage = storage.withFluid(smashing, readFluid(fluid, ops));
      persistent.remove("tconstruct:smashing_fluid_tag");
      changed = true;
    } else if (persistent.contains(smashing.toString(), Tag.TAG_STRING) && storage.fluid(smashing).isEmpty()) {
      CompoundTag fluid = new CompoundTag();
      fluid.putString("FluidName", persistent.getString(smashing.toString()));
      fluid.putInt("Amount", 1);
      storage = storage.withFluid(smashing, readFluid(fluid, ops));
      changed = true;
    }
    return new Migration(storage, copy, changed);
  }

  public static FluidStack readFluid(CompoundTag data, DynamicOps<?> ops) {
    ResourceLocation id = ResourceLocation.parse(data.getString("FluidName"));
    var holder = BuiltInRegistries.FLUID.getHolder(id)
      .orElseThrow(() -> new IllegalArgumentException("Unknown legacy tool fluid " + id));
    int amount = data.getInt("Amount");
    if (amount <= 0) return FluidStack.EMPTY;
    DataComponentPatch patch = data.contains("Tag", Tag.TAG_COMPOUND)
      ? LegacyItemData.convert(Items.POTION, data.getCompound("Tag"), jsonOps(ops)) : DataComponentPatch.EMPTY;
    return new FluidStack(holder, amount, patch);
  }
}
