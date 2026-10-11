package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.Item;

/** Converts imported 1.20.1 item metadata through vanilla data fixes. */
public final class LegacyItemData {
  private LegacyItemData() {}

  public static DataComponentPatch convert(Item item, CompoundTag tag, DynamicOps<JsonElement> ops) {
    CompoundTag oldStack = new CompoundTag();
    oldStack.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
    oldStack.putByte("Count", (byte) 1);
    oldStack.put("tag", tag.copy());
    Tag fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
      new Dynamic<>(NbtOps.INSTANCE, oldStack), 3465,
      SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
    if (!(fixed instanceof CompoundTag converted)) {
      throw new IllegalArgumentException("Vanilla item data fix did not produce a compound");
    }
    Tag components = converted.get("components");
    // Keep NBT booleans in their native ops. Converting byte tags to JSON makes them numbers.
    DynamicOps<Tag> componentOps = ops instanceof RegistryOps<?> registryOps
      ? registryOps.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE;
    return components != null
      ? DataComponentPatch.CODEC.parse(componentOps, components).getOrThrow()
      : DataComponentPatch.EMPTY;
  }
}
