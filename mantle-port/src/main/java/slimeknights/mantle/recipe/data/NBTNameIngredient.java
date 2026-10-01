package slimeknights.mantle.recipe.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.crafting.Ingredient;
import javax.annotation.Nullable;

/** Converts 1.20.1 strict stack metadata into component ingredient data during datagen. */
public final class NBTNameIngredient {
  private static final int SOURCE_DATA_VERSION = 3465;
  private NBTNameIngredient() {}
  public static Ingredient from(ResourceLocation name, @Nullable CompoundTag nbt) {
    CompoundTag oldStack = new CompoundTag();
    oldStack.putString("id", name.toString());
    oldStack.putByte("Count", (byte)1);
    if (nbt != null) { oldStack.put("tag", nbt.copy()); }
    JsonElement fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
      new Dynamic<>(NbtOps.INSTANCE, oldStack), SOURCE_DATA_VERSION,
      SharedConstants.getCurrentVersion().getDataVersion().getVersion()).convert(JsonOps.INSTANCE).getValue();
    JsonObject ingredient = new JsonObject();
    ingredient.addProperty("type", "neoforge:components");
    ingredient.addProperty("items", name.toString());
    JsonObject stack = fixed.getAsJsonObject();
    ingredient.add("components", stack.has("components") ? stack.get("components").deepCopy() : new JsonObject());
    ingredient.addProperty("strict", true);
    return new DatagenIngredient(ingredient).toVanilla();
  }
  public static Ingredient from(ResourceLocation name) { return from(name, null); }
}
