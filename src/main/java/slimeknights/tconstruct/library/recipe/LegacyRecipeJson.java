package slimeknights.tconstruct.library.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.mantle.data.loadable.LoadableOps;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Imports retained 1.20 recipe JSON into the native codec using the real reload context. */
public final class LegacyRecipeJson {
  private LegacyRecipeJson() {}

  private static final Set<String> RENAMED_TYPES = Set.of(
    "true", "false", "mod_loaded", "item_exists", "tag_empty", "not", "and", "or",
    "difference", "intersection", "compound");
  private static final Set<String> NATIVE_STACK_RESULTS = Set.of(
    "minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:stonecutting",
    "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking",
    "mantle:crafting_shaped_retextured",
    "tconstruct:crafting_shaped_material", "tconstruct:crafting_shaped_materials",
    "tconstruct:crafting_shapeless_materials");

  public static JsonElement migrate(JsonElement input, DynamicOps<JsonElement> ops) {
    JsonObject json = input.getAsJsonObject().deepCopy();
    renameTypes(json);
    return recipe(json, ops);
  }

  private static void renameTypes(JsonElement element) {
    if (element.isJsonArray()) {
      for (JsonElement child : element.getAsJsonArray()) renameTypes(child);
    } else if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("type") && object.get("type").isJsonPrimitive()) {
        String type = object.get("type").getAsString();
        if (type.startsWith("forge:") && RENAMED_TYPES.contains(type.substring(6))) {
          object.addProperty("type", "neoforge:" + type.substring(6));
        }
      }
      for (var entry : object.entrySet()) renameTypes(entry.getValue());
    }
  }

  private static JsonArray conditions(JsonObject json) {
    JsonArray result = new JsonArray();
    if (json.has("conditions")) result.addAll(json.getAsJsonArray("conditions"));
    if (json.has(ConditionalOps.DEFAULT_CONDITIONS_KEY)) {
      result.addAll(json.getAsJsonArray(ConditionalOps.DEFAULT_CONDITIONS_KEY));
    }
    return result;
  }

  private static boolean matches(JsonArray conditions, DynamicOps<JsonElement> ops, ICondition.IContext context) {
    for (ICondition condition : ICondition.LIST_CODEC.parse(ops, conditions).getOrThrow(JsonSyntaxException::new)) {
      if (!condition.test(context)) return false;
    }
    return true;
  }

  private static JsonObject recipe(JsonObject json, DynamicOps<JsonElement> ops) {
    JsonArray conditions = conditions(json);
    if (json.has("type") && json.get("type").getAsString().equals("forge:conditional")) {
      if (!(ops instanceof ConditionalOps<?>)) {
        throw new JsonSyntaxException("Legacy conditional recipe requires actual reload condition context");
      }
      ICondition.IContext context = ConditionalOps.retrieveContext().codec()
        .parse(ops, ops.emptyMap()).getOrThrow(JsonSyntaxException::new);
      if (matches(conditions, ops, context)) {
        for (JsonElement child : json.getAsJsonArray("recipes")) {
          JsonObject branch = child.getAsJsonObject();
          JsonArray branchConditions = conditions(branch);
          if (matches(branchConditions, ops, context)) {
            JsonObject selected = recipe(branch.getAsJsonObject("recipe"), ops);
            conditions.addAll(branchConditions);
            conditions.addAll(conditions(selected));
            selected.remove("conditions");
            selected.add(ConditionalOps.DEFAULT_CONDITIONS_KEY, conditions);
            return selected;
          }
        }
      }
      // The native conditional codec skips this resource before decoding its unselected payload.
      JsonObject disabled = new JsonObject();
      disabled.addProperty("type", "neoforge:false");
      JsonArray falseConditions = new JsonArray();
      falseConditions.add(disabled);
      json.add(ConditionalOps.DEFAULT_CONDITIONS_KEY, falseConditions);
      json.remove("conditions");
      return json;
    }
    json.remove("conditions");
    if (!conditions.isEmpty()) json.add(ConditionalOps.DEFAULT_CONDITIONS_KEY, conditions);
    if (!conditions.isEmpty() && ops instanceof ConditionalOps<?>) {
      ICondition.IContext context = ConditionalOps.retrieveContext().codec()
        .parse(ops, ops.emptyMap()).getOrThrow(JsonSyntaxException::new);
      if (!matches(conditions, ops, context)) return json;
    }
    String type = json.has("type") ? json.get("type").getAsString() : "";
    if (NATIVE_STACK_RESULTS.contains(type) && json.has("result")) {
      JsonElement result = json.get("result");
      if (result.isJsonPrimitive() || result.isJsonObject() && result.getAsJsonObject().has("item")) {
        if (result.isJsonPrimitive() && json.has("count")) {
          JsonObject counted = new JsonObject();
          counted.add("item", result);
          counted.add("count", json.get("count"));
          result = counted;
        }
        JsonElement legacy = result;
        ItemStack stack = LoadableOps.with(ops, () ->
          ItemStackLoadable.REQUIRED_STACK_NBT.convert(legacy, "result", TypedMap.EMPTY));
        json.add("result", ItemStack.CODEC.encodeStart(ops, stack).getOrThrow(JsonSyntaxException::new));
      }
    }
    return json;
  }
}
