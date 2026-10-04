package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;
import slimeknights.mantle.data.loadable.LoadableOps;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.typed.TypedMapBuilder;

import java.util.function.Supplier;

/**
 * Recipe serializer using loadables. Recipe identity belongs to RecipeHolder, not the recipe payload.
 * @param <T>  Recipe type
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class LoadableRecipeSerializer<T extends Recipe<?>> implements LoggingRecipeSerializer<T> {
  /** Context key to use if you want the recipe serializer passed into your recipe */
  public static final ContextKey<RecipeSerializer<?>> SERIALIZER = new ContextKey<>("serializer");
  /** Context key to use if you want a type aware serializer in the recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<TypeAwareRecipeSerializer<?>> TYPED_SERIALIZER = new ContextKey<>("typed_serializer");
  /** Context key to use if you want the recipe type passed into your recipe, requires {@link #of(RecordLoadable, Supplier)} for your serializer. */
  public static final ContextKey<RecipeType<?>> TYPE = new ContextKey<>("type");
  /** Field for a group key in a recipe (common requirement) */
  public static final LoadableField<String,Recipe<?>> RECIPE_GROUP = StringLoadable.DEFAULT.defaultField("group", "", Recipe::getGroup);


  protected final RecordLoadable<T> loadable;

  /** Creates a standard serializer from a loadable */
  public static <T extends Recipe<?>> RecipeSerializer<T> of(RecordLoadable<T> loadable) {
    return new LoadableRecipeSerializer<>(loadable);
  }

  /** Creates a type aware serializer from a loadable */
  public static <T extends R, R extends Recipe<?>> TypeAwareRecipeSerializer<T> of(RecordLoadable<T> loadable, Supplier<? extends RecipeType<R>> type) {
    return new TypeAware<>(loadable, type);
  }

  /** Creates a serializer that is deprecated, logging a warning when used */
  public static <T extends Recipe<?>> RecipeSerializer<T> deprecated(RecordLoadable<T> loadable, String replacement) {
    return new Deprecated<>(loadable, replacement);
  }

  /** Builds payload context. The enclosing RecipeHolder owns the ID. */
  protected TypedMapBuilder buildContext() {
    TypedMapBuilder context = TypedMapBuilder.builder()
      .put(ContextKey.DEBUG, "Recipe serializer " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(this)).put(SERIALIZER, this);
    ResourceLocation id = RecipeDecodeContext.currentId();
    if (id != null) {
      context.put(ContextKey.ID, id);
    }
    return context;
  }

  protected T readJson(JsonObject json) {
    return loadable.deserialize(json, buildContext().build());
  }

  /** Compatibility entry point for callers that explicitly supply a real ID. */
  public T fromJson(ResourceLocation id, JsonObject json) {
    return loadable.deserialize(json, buildContext().put(ContextKey.ID, id).build());
  }

  @Override
  public MapCodec<T> codec() {
    return new MapCodec<>() {
      @Override
      public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
        return ops.getMap(ops.createMap(input.entries())).flatMap(map -> {
          try {
            JsonObject json = ops.convertTo(JsonOps.INSTANCE, ops.createMap(map.entries())).getAsJsonObject();
            return DataResult.success(LoadableOps.with(ops, () -> readJson(json)));
          } catch (RuntimeException e) {
            return DataResult.error(() -> "Unable to decode recipe: " + e.getMessage());
          }
        });
      }

      @Override
      public <O> RecordBuilder<O> encode(T recipe, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        try {
          JsonObject json = LoadableOps.with(ops, () -> loadable.serialize(recipe)).getAsJsonObject();
          for (var entry : json.entrySet()) {
            prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
          }
        } catch (RuntimeException e) {
          prefix.withErrorsFrom(DataResult.error(() -> "Unable to encode recipe: " + e.getMessage()));
        }
        return prefix;
      }

      @Override
      public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.empty();
      }
    };
  }

  @Override
  public T fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
    return loadable.decode(buffer, buildContext().build());
  }

  @Override
  public void toNetworkSafe(RegistryFriendlyByteBuf buffer, T recipe) {
    loadable.encode(buffer, recipe);
  }

  public static class TypeAware<T extends Recipe<?>> extends LoadableRecipeSerializer<T> implements TypeAwareRecipeSerializer<T> {
    private final Supplier<? extends RecipeType<?>> type;
    protected TypeAware(RecordLoadable<T> loadable, Supplier<? extends RecipeType<?>> type) {
      super(loadable);
      this.type = type;
    }

    @Override
    protected TypedMapBuilder buildContext() {
      return super.buildContext().put(TYPE, getType()).put(TYPED_SERIALIZER, this);
    }

    @Override
    public RecipeType<?> getType() {
      return type.get();
    }

  }

  /** Helper class that logs a warning on recipe parse about planned removal */
  private static class Deprecated<T extends Recipe<?>> extends LoadableRecipeSerializer<T> {
    private final String replacement;
    protected Deprecated(RecordLoadable<T> loadable, String replacement) {
      super(loadable);
      this.replacement = replacement;
    }

    @Override
    protected T readJson(JsonObject json) {
      T recipe = super.readJson(json);
      Mantle.logger.warn("Using deprecated recipe serializer {}, {}", BuiltInRegistries.RECIPE_SERIALIZER.getKey(this), replacement);
      return recipe;
    }
  }
}
