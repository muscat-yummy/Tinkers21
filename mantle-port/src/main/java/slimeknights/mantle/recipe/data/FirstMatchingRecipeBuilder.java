package slimeknights.mantle.recipe.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.conditions.AndCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import slimeknights.mantle.recipe.MantleRecipes;

/** Datagen for ordered native conditional alternatives with one stable recipe ID. */
public final class FirstMatchingRecipeBuilder {
  private record Entry(List<ICondition> conditions, Consumer<RecipeConsumer> recipe) {}
  private final List<Entry> entries = new ArrayList<>();
  private final List<ICondition> pending = new ArrayList<>();
  private boolean advancement;

  public static FirstMatchingRecipeBuilder builder() {
    return new FirstMatchingRecipeBuilder();
  }

  public FirstMatchingRecipeBuilder addCondition(ICondition condition) {
    pending.add(condition);
    return this;
  }

  public FirstMatchingRecipeBuilder addRecipe(Consumer<RecipeConsumer> recipe) {
    if (pending.isEmpty()) {
      throw new IllegalStateException("A conditional alternative needs a condition");
    }
    entries.add(new Entry(List.copyOf(pending), recipe));
    pending.clear();
    return this;
  }

  public FirstMatchingRecipeBuilder generateAdvancement() {
    advancement = true;
    return this;
  }

  public void build(RecipeConsumer consumer, ResourceLocation id) {
    if (!pending.isEmpty() || entries.isEmpty()) {
      throw new IllegalStateException("Conditional recipes need complete, nonempty alternatives");
    }
    HolderLookup.Provider lookup = consumer.registryLookup();
    JsonArray recipes = new JsonArray();
    List<ICondition> any = new ArrayList<>();
    JsonObject sharedAdvancement = null;
    for (Entry entry : entries) {
      List<FinishedRecipe> captured = new ArrayList<>();
      entry.recipe().accept(RecipeConsumer.wrap(consumer, captured::add));
      if (captured.size() != 1) {
        throw new IllegalStateException("Each conditional alternative must produce exactly one recipe");
      }
      FinishedRecipe recipe = captured.getFirst();
      JsonObject json = recipe.serializeRecipe(lookup).deepCopy();
      List<ICondition> conditions = new ArrayList<>(entry.conditions());
      if (json.has("neoforge:conditions")) {
        conditions.addAll(ICondition.LIST_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, lookup),
            json.get("neoforge:conditions")).getOrThrow());
      }
      ICondition.writeConditions(lookup, json, conditions);
      recipes.add(json);
      any.add(new AndCondition(conditions));
      if (advancement) {
        JsonObject candidate = recipe.serializeAdvancement(lookup);
        if (candidate == null) {
          throw new IllegalStateException("Generated conditional advancement is missing");
        }
        candidate = candidate.deepCopy();
        candidate.remove("neoforge:conditions");
        // Current callers share one unlock advancement across all alternatives.
        // Reject differing unlocks instead of silently selecting the wrong advancement.
        if (sharedAdvancement != null && !sharedAdvancement.equals(candidate)) {
          throw new IllegalStateException("Conditional alternatives have different unlock advancements");
        }
        sharedAdvancement = candidate;
      }
    }
    JsonObject data = new JsonObject();
    data.add("recipes", recipes);
    ICondition enabled = new OrCondition(any);
    ICondition.writeConditions(lookup, data, enabled);
    if (sharedAdvancement != null) {
      ICondition.writeConditions(lookup, sharedAdvancement, enabled);
    }
    JsonObject advancementJson = sharedAdvancement;
    consumer.accept(new FinishedRecipe() {
      @Override public ResourceLocation getId() { return id; }
      @Override public RecipeSerializer<?> getType() { return MantleRecipes.CONDITIONAL.get(); }
      @Override public ResourceLocation getAdvancementId() {
        return advancementJson == null ? null : id.withPrefix("recipes/");
      }
      @Override public void serializeRecipeData(JsonObject output) {
        data.entrySet().forEach(entry -> output.add(entry.getKey(), entry.getValue().deepCopy()));
      }
      @Override public JsonObject serializeAdvancement(HolderLookup.Provider registries) {
        return advancementJson == null ? null : advancementJson.deepCopy();
      }
    });
  }
}
