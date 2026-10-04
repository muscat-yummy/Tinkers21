package slimeknights.tconstruct.library.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.DynamicOps;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Evaluates old custom-data condition arrays and the native condition key before payload decoding. */
public final class JsonConditionHelper {
  private JsonConditionHelper() {}

  public static boolean matches(JsonObject json, DynamicOps<JsonElement> ops, ICondition.IContext context) {
    return matches(json, "conditions", ops, context)
      && matches(json, ConditionalOps.DEFAULT_CONDITIONS_KEY, ops, context);
  }

  private static boolean matches(JsonObject json, String key, DynamicOps<JsonElement> ops, ICondition.IContext context) {
    if (!json.has(key)) return true;
    for (ICondition condition : ICondition.LIST_CODEC.parse(ops, json.get(key)).getOrThrow(JsonSyntaxException::new)) {
      if (!condition.test(context)) return false;
    }
    return true;
  }
}
