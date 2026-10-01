package slimeknights.mantle.recipe.data;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Builds a recipe consumer wrapper, which adds some extra properties to wrap the result of another recipe
 */
@SuppressWarnings("unused")  // API
public class ConsumerWrapperBuilder {
  private final List<ICondition> conditions = new ArrayList<>();
  @Nullable
  private final RecipeSerializer<?> override;
  @Nullable
  private final ResourceLocation overrideName;

  private ConsumerWrapperBuilder(@Nullable RecipeSerializer<?> override, @Nullable ResourceLocation overrideName) {
    this.override = override;
    this.overrideName = overrideName;
  }

  /**
   * Creates a wrapper builder with the default serializer
   * @return Default serializer builder
   */
  public static ConsumerWrapperBuilder wrap() {
    return new ConsumerWrapperBuilder(null, null);
  }

  /**
   * Creates a wrapper builder with a serializer override
   * @param override Serializer override
   * @return Default serializer builder
   */
  public static ConsumerWrapperBuilder wrap(RecipeSerializer<?> override) {
    return new ConsumerWrapperBuilder(override, null);
  }

  /**
   * Creates a wrapper builder with a serializer name override
   * @param override Serializer override
   * @return Default serializer builder
   */
  public static ConsumerWrapperBuilder wrap(ResourceLocation override) {
    return new ConsumerWrapperBuilder(null, override);
  }

  /**
   * Adds a conditional to the consumer
   * @param condition Condition to add
   * @return Added condition
   */
  @CanIgnoreReturnValue
  public ConsumerWrapperBuilder addCondition(ICondition condition) {
    conditions.add(condition);
    return this;
  }

  /**
   * Builds the consumer for the wrapper builder
   * @param consumer Base consumer
   * @return Built wrapper consumer
   */
  public RecipeConsumer build(RecipeConsumer consumer) {
    return RecipeConsumer.wrap(consumer, recipe -> consumer.accept(new Wrapped(recipe, List.copyOf(conditions), override, overrideName)));
  }

  private static class Wrapped implements FinishedRecipe {
    private final FinishedRecipe original;
    private final List<ICondition> conditions;
    @Nullable
    private final RecipeSerializer<?> override;
    @Nullable
    private final ResourceLocation overrideName;

    private Wrapped(FinishedRecipe original, List<ICondition> conditions, @Nullable RecipeSerializer<?> override, @Nullable ResourceLocation overrideName) {
      // if wrapping another wrapper result, merge the two together
      if (original instanceof Wrapped toMerge) {
        this.original = toMerge.original;
        this.conditions = Stream.concat(toMerge.conditions.stream(), conditions.stream()).toList();
        // consumer wrappers are processed inside out, so the innermost wrapped recipe is the one with the most recent serializer override
        if (toMerge.override != null || toMerge.overrideName != null) {
          this.override = toMerge.override;
          this.overrideName = toMerge.overrideName;
        } else {
          this.override = override;
          this.overrideName = overrideName;
        }
      } else {
        this.original = original;
        this.conditions = List.copyOf(conditions);
        this.override = override;
        this.overrideName = overrideName;
      }
    }

    @Override
    public JsonObject serializeRecipe() {
      JsonObject json = new JsonObject();
      if (overrideName != null) {
        json.addProperty("type", overrideName.toString());
      } else {
        json.addProperty("type", Objects.requireNonNull(BuiltInRegistries.RECIPE_SERIALIZER.getKey(getType())).toString());
      }
      this.serializeRecipeData(json);
      return json;
    }

    @Override
    public void serializeRecipeData(JsonObject json) {
      // add conditions on top
      if (!conditions.isEmpty()) {
        JsonArray conditionsArray = new JsonArray();
        for (ICondition condition : conditions) {
          conditionsArray.add(ICondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).getOrThrow());
        }
        json.add("neoforge:conditions", conditionsArray);
      }
      // serialize the normal recipe
      original.serializeRecipeData(json);
    }

    @Override
    public JsonObject serializeRecipe(HolderLookup.Provider registries) {
      JsonObject json = original.serializeRecipe(registries).deepCopy();
      if (overrideName != null) {
        json.addProperty("type", overrideName.toString());
      } else if (override != null) {
        json.addProperty("type", Objects.requireNonNull(BuiltInRegistries.RECIPE_SERIALIZER.getKey(override)).toString());
      }
      addConditions(registries, json);
      return json;
    }
    private void addConditions(HolderLookup.Provider registries, JsonObject json) {
      JsonArray previous = json.has("neoforge:conditions") ? json.getAsJsonArray("neoforge:conditions").deepCopy() : new JsonArray();
      JsonObject extra = new JsonObject();
      ICondition.writeConditions(registries, extra, conditions);
      if (extra.has("neoforge:conditions")) { extra.getAsJsonArray("neoforge:conditions").forEach(previous::add); }
      if (!previous.isEmpty()) { json.add("neoforge:conditions", previous); }
    }
    @Nullable
    @Override
    public JsonObject serializeAdvancement(HolderLookup.Provider registries) {
      JsonObject json = original.serializeAdvancement(registries);
      if (json != null) { json = json.deepCopy(); addConditions(registries, json); }
      return json;
    }

    @Override
    public ResourceLocation getId() {
      return original.getId();
    }

    @Override
    public RecipeSerializer<?> getType() {
      if (override != null) {
        return override;
      }
      return original.getType();
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement() {
      return original.serializeAdvancement();
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId() {
      return original.getAdvancementId();
    }
  }
}
