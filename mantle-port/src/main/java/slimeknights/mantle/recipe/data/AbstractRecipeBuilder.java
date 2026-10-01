package slimeknights.mantle.recipe.data;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Common logic to create a recipe builder class
 * @param <T>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AbstractRecipeBuilder<T extends AbstractRecipeBuilder<T>> {
  /** Advancement builder for this class */
  protected Advancement.Builder advancementBuilder = Advancement.Builder.recipeAdvancement();
  private final Map<String,Criterion<?>> criteria = new LinkedHashMap<>();
  /** Group for this recipe */
  @Nonnull
  protected String group = "";

  /**
   * Adds a criteria to the recipe
   * @param name      Criteria name
   * @param criteria  Criteria instance
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T unlockedBy(String name, Criterion<?> criterion) {
    this.criteria.put(name, criterion);
    return (T)this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe group
   * @return  Builder
   */
  @SuppressWarnings("unchecked")
  public T group(String group) {
    this.group = group;
    return (T)this;
  }

  /**
   * Sets the group for this recipe
   * @param group  Recipe resource location group
   * @return  Builder
   */
  public T group(ResourceLocation group) {
    // if minecraft, no namepsace. Groups are technically not namespaced so this is for consistency with vanilla
    if ("minecraft".equals(group.getNamespace())) {
      return group(group.getPath());
    }
    return group(group.toString());
  }

  /**
   * Builds the recipe with a default recipe ID, typically based on the output
   * @param consumerIn  Recipe consumer
   */
  public abstract void save(RecipeConsumer consumerIn);

  /**
   * Builds the recipe
   * @param consumerIn  Recipe consumer
   * @param id          Recipe ID
   */
  public abstract void save(RecipeConsumer consumerIn, ResourceLocation id);

  /**
   * Base logic for advancement building
   * @param id      Recipe ID
   * @param folder  Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return Advancement ID
   */
  private ResourceLocation buildAdvancementInternal(ResourceLocation id, String folder) {
    this.advancementBuilder = Advancement.Builder.recipeAdvancement();
    criteria.forEach(this.advancementBuilder::addCriterion);
    this.advancementBuilder
        .parent(ResourceLocation.parse("recipes/root"))
        .rewards(AdvancementRewards.Builder.recipe(id))
        .requirements(AdvancementRequirements.Strategy.OR);
    if (!criteria.containsKey("has_the_recipe")) {
      this.advancementBuilder.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id));
    }
    return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipes/" + folder + "/" + id.getPath());
  }

  /**
   * Builds and validates the advancement, intended to be called in {@link #save(Consumer, ResourceLocation)}
   * @param id      Recipe ID
   * @param folder  Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return Advancement ID
   */
  protected ResourceLocation buildAdvancement(ResourceLocation id, String folder) {
    if (this.criteria.isEmpty()) {
      throw new IllegalStateException("No way of obtaining recipe " + id);
    }
    return buildAdvancementInternal(id, folder);
  }

  /**
   * Builds an optional advancement, intended to be called in {@link #save(Consumer, ResourceLocation)}
   * @param id        Recipe ID
   * @param folder    Group folder for saving recipes. Vanilla typically uses item groups, but for mods might as well base on the recipe
   * @return Advancement ID, or null if the advancement was not defined
   */
  @SuppressWarnings("SameParameterValue")  // API
  @Nullable
  protected ResourceLocation buildOptionalAdvancement(ResourceLocation id, String folder) {
    if (this.criteria.isEmpty()) {
      return null;
    }
    return buildAdvancementInternal(id, folder);
  }

  /** Class to implement basic finished recipe methods */
  @Getter
  protected abstract class AbstractFinishedRecipe implements FinishedRecipe {
    private final ResourceLocation id;
    @Nullable
    private final ResourceLocation advancementId;
    @Nullable
    private final AdvancementHolder advancement;

    protected AbstractFinishedRecipe(ResourceLocation id, @Nullable ResourceLocation advancementId) {
      this.id = id;
      this.advancementId = advancementId;
      this.advancement = advancementId == null ? null : advancementBuilder.build(advancementId);
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement(HolderLookup.Provider registries) {
      if (advancementId == null) {
        return null;
      }
      return Advancement.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), advancement.value()).getOrThrow().getAsJsonObject();
    }
  }

  /** Finished recipe using a loadable */
  protected class LoadableFinishedRecipe<R extends Recipe<?>> extends AbstractFinishedRecipe {
    private final R recipe;
    private final RecordLoadable<R> loadable;
    public LoadableFinishedRecipe(ResourceLocation id, R recipe, RecordLoadable<R> loadable, @Nullable ResourceLocation advancementId) {
      super(id, advancementId);
      this.recipe = recipe;
      this.loadable = loadable;
    }

    @Override
    public void serializeRecipeData(JsonObject json) {
      loadable.serialize(recipe, json);
    }

    @Override
    public RecipeSerializer<?> getType() {
      return recipe.getSerializer();
    }
  }
}
