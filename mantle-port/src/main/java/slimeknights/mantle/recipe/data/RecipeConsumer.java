package slimeknights.mantle.recipe.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.conditions.ICondition;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Bridges Mantle raw recipe builders and native recipe outputs. */
public interface RecipeConsumer extends Consumer<FinishedRecipe>, RecipeOutput {
  HolderLookup.Provider registryLookup();

  @Override
  default Advancement.Builder advancement() {
    return Advancement.Builder.recipeAdvancement().parent(ResourceLocation.parse("recipes/root"));
  }

  @Override
  default void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
    HolderLookup.Provider registries = registryLookup();
    JsonObject json;
    try {
      json = Recipe.CODEC.encodeStart(JsonOps.INSTANCE, recipe).getOrThrow().getAsJsonObject();
    } catch (RuntimeException failure) {
      var output = recipe.getResultItem(registries);
      var key = BuiltInRegistries.ITEM.getResourceKey(output.getItem()).orElseThrow();
      var itemLookup = registries.lookup(Registries.ITEM);
      boolean providerHasResult = itemLookup.flatMap(lookup -> lookup.get(key)).isPresent();
      boolean providerOwnsResult = itemLookup.flatMap(lookup -> lookup.get(key))
        .map(holder -> holder.value() == output.getItem()).orElse(false);
      throw new IllegalStateException("Failed to encode actual recipe " + id + ": native item " + key.location() +
        ", datagen item registry available=" + itemLookup.isPresent() +
        ", result present=" + providerHasResult + ", same registered item=" + providerOwnsResult, failure);
    }
    ICondition.writeConditions(registries, json, conditions);
    final JsonObject advancementJson;
    if (advancement != null) {
      try {
        advancementJson = Advancement.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries),
          withEventItemHolders(advancement.value(), registries)).getOrThrow().getAsJsonObject();
      } catch (RuntimeException failure) {
        var result = recipe.getResultItem(registries);
        var key = BuiltInRegistries.ITEM.getResourceKey(result.getItem()).orElseThrow();
        var lookup = registries.lookup(Registries.ITEM);
        boolean included = lookup.flatMap(items -> items.get(key)).isPresent();
        boolean sameItem = lookup.flatMap(items -> items.get(key))
          .map(holder -> holder.value() == result.getItem()).orElse(false);
        throw new IllegalStateException("Failed actual advancement " + advancement.id() + " for recipe " + id +
          ": output " + key.location() + ", actual event item registry available=" + lookup.isPresent() +
          ", result included=" + included + ", same item=" + sameItem, failure);
      }
    }
    else { advancementJson = null; }
    if (advancementJson != null) { ICondition.writeConditions(registries, advancementJson, conditions); }
    accept(new FinishedRecipe() {
      @Override public ResourceLocation getId() { return id; }
      @Override public RecipeSerializer<?> getType() { return recipe.getSerializer(); }
      @Override public ResourceLocation getAdvancementId() { return advancement == null ? null : advancement.id(); }
      @Override public void serializeRecipeData(JsonObject output) {
        json.entrySet().stream().filter(entry -> !entry.getKey().equals("type"))
            .forEach(entry -> output.add(entry.getKey(), entry.getValue().deepCopy()));
      }
      @Override public JsonObject serializeAdvancement(HolderLookup.Provider lookup) {
        return advancementJson == null ? null : advancementJson.deepCopy();
      }
    });
  }

  /**
   * Native recipe builders capture built-in Item holders before GatherDataEvent supplies
   * its registry lookup. Copy only inventory item predicates into that actual lookup,
   * retaining criterion names, count/components, rewards and requirement groups.
   */
  private static Advancement withEventItemHolders(Advancement original, HolderLookup.Provider provider) {
    var items = provider.lookupOrThrow(Registries.ITEM);
    Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    original.criteria().forEach((name, criterion) -> {
      if (criterion.triggerInstance() instanceof InventoryChangeTrigger.TriggerInstance inventory) {
        var predicates = inventory.items().stream().map(predicate -> {
          Optional<HolderSet<net.minecraft.world.item.Item>> set = predicate.items().map(source -> {
            if (source instanceof HolderSet.Named<net.minecraft.world.item.Item> tag) {
              return items.getOrThrow(tag.key());
            }
            return HolderSet.direct(source.stream().map(holder -> {
              var key = holder.unwrapKey().orElseThrow(() ->
                new IllegalStateException("Inventory criterion has unregistered item holder: " + name));
              Holder.Reference<net.minecraft.world.item.Item> registered = items.getOrThrow(key);
              if (registered.value() != holder.value()) {
                throw new IllegalStateException("Inventory criterion item differs from event registry: " + key.location());
              }
              return (Holder<net.minecraft.world.item.Item>) registered;
            }).toList());
          });
          return new ItemPredicate(set, predicate.count(), predicate.components(), predicate.subPredicates());
        }).toList();
        @SuppressWarnings("unchecked")
        Criterion<InventoryChangeTrigger.TriggerInstance> typed =
          (Criterion<InventoryChangeTrigger.TriggerInstance>) criterion;
        criteria.put(name, new Criterion<>(typed.trigger(),
          new InventoryChangeTrigger.TriggerInstance(inventory.player(), inventory.slots(), predicates)));
      } else {
        criteria.put(name, criterion);
      }
    });
    return new Advancement(original.parent(), original.display(), original.rewards(),
      criteria, original.requirements(), original.sendsTelemetryEvent(), original.name());
  }

  /** Wraps raw output while retaining the registry context of the original consumer. */
  static RecipeConsumer wrap(RecipeConsumer original, Consumer<FinishedRecipe> consumer) {
    return new RecipeConsumer() {
      @Override public HolderLookup.Provider registryLookup() { return original.registryLookup(); }
      @Override public void accept(FinishedRecipe recipe) { consumer.accept(recipe); }
    };
  }
}
