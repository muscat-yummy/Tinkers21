package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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
    JsonObject fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
      new Dynamic<>(NbtOps.INSTANCE, oldStack), 3465,
      SharedConstants.getCurrentVersion().getDataVersion().getVersion())
      .convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
    return fixed.has("components")
      ? DataComponentPatch.CODEC.parse(ops, fixed.get("components")).getOrThrow()
      : DataComponentPatch.EMPTY;
  }
}
