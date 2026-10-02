package slimeknights.tconstruct.port;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import slimeknights.tconstruct.common.json.BlockOrEntityCondition;
import slimeknights.tconstruct.library.json.loot.HasLootContextSetCondition;

/** Checks legacy JSON fields and validation through the production condition codecs. */
public final class LootConditionSmoke {
  public static void main(String[] args) {
    if (BlockOrEntityCondition.CODEC.codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow() != BlockOrEntityCondition.INSTANCE) {
      throw new AssertionError("Unit condition did not retain singleton");
    }
    if (BlockOrEntityCondition.INSTANCE.getType() != BlockOrEntityCondition.TYPE) {
      throw new AssertionError("Condition type identity changed");
    }
    String[] names = {"empty", "chest", "block", "entity"};
    LootContextParamSet[] sets = {LootContextParamSets.EMPTY, LootContextParamSets.CHEST, LootContextParamSets.BLOCK, LootContextParamSets.ENTITY};
    for (int index = 0; index < names.length; index++) {
      String name = names[index];
      ResourceLocation id = ResourceLocation.withDefaultNamespace(name);
      LootContextParamSet set = sets[index];
      if (set == null) {
        throw new AssertionError("Missing vanilla parameter set " + id);
      }
      JsonObject input = new JsonObject();
      input.addProperty("set", id.toString());
      HasLootContextSetCondition value = HasLootContextSetCondition.CODEC.codec().parse(JsonOps.INSTANCE, input).getOrThrow();
      if (value.set() != set || value.getType() != HasLootContextSetCondition.TYPE) {
        throw new AssertionError("Parameter set/type identity changed");
      }
      JsonObject output = HasLootContextSetCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, value).getOrThrow().getAsJsonObject();
      if (!output.equals(input)) {
        throw new AssertionError("Legacy set field changed: " + output);
      }
    }
    JsonObject unknown = new JsonObject();
    unknown.addProperty("set", "tconstruct:missing_parameter_set");
    if (HasLootContextSetCondition.CODEC.codec().parse(JsonOps.INSTANCE, unknown).error().isEmpty()
        || HasLootContextSetCondition.CODEC.codec().parse(JsonOps.INSTANCE, new JsonObject()).error().isEmpty()) {
      throw new AssertionError("Unknown/missing set accepted");
    }
    LootContextParamSet unregistered = new LootContextParamSet.Builder().build();
    if (HasLootContextSetCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, new HasLootContextSetCondition(unregistered)).error().isEmpty()) {
      throw new AssertionError("Unregistered set accepted");
    }
    System.out.println("Loot condition legacy JSON, identities and invalid-set validation: PASS");
  }
}
