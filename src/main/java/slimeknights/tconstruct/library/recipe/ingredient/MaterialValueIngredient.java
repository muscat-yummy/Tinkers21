package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Ingredient matching material items with the given value. Typically, matches ingots or blocks
 */
@Getter
@RequiredArgsConstructor
public class MaterialValueIngredient implements ICustomIngredient {
  private final IJsonPredicate<MaterialVariantId> material;
  private final float minValue;
  private final float maxValue;
  public static final LoadableIngredientSerializer<MaterialValueIngredient> SERIALIZER = new LoadableIngredientSerializer<>(Serializer.INSTANCE);
  public static final IngredientType<MaterialValueIngredient> TYPE = SERIALIZER.type("material", "value");

  /** Creates an ingredient matching a range of values */
  public static Ingredient of(IJsonPredicate<MaterialVariantId> materials, float minValue, float maxValue) {
    return new MaterialValueIngredient(materials, minValue, maxValue).toVanilla();
  }

  /** Creates an ingredient matching an exact value */
  public static Ingredient of(IJsonPredicate<MaterialVariantId> materials, float value) {
    return of(materials, value, value);
  }

  /** Checks the given material recipe against our filters */
  public boolean test(MaterialRecipe material) {
    float value = material.getValue() / (float) material.getNeeded();
    return minValue <= value && value <= maxValue && this.material.matches(material.getMaterial().getVariant());
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null) {
      return false;
    }
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe);
  }

  @Override
  public Stream<ItemStack> getItems() {
    return MaterialRecipeCache.getSortedRecipes().stream()
      .filter(this::test)
      .flatMap(material -> Arrays.stream(material.getIngredient().getItems()));
  }

  @Override
  public boolean isSimple() {
    return true;
  }


  /* Helpers for ShapedMaterialRecipe */

  /** Checks if this ingredient fully contains the range of the other */
  private boolean contains(MaterialValueIngredient other) {
    return this.minValue <= other.minValue && other.maxValue <= this.maxValue;
  }

  /** Creates an ingredient that matches anything either of the two ingredients matches */
  public MaterialValueIngredient merge(MaterialValueIngredient other) {
    if (this == other) return this;

    // if we have the same predicate, we can possibly skip creating a new instance
    IJsonPredicate<MaterialVariantId> predicate = this.material;
    if (this.material.equals(other.material)) {
      if (this.contains(other)) {
        return this;
      }
      if (other.contains(this)) {
        return other;
      }
    } else {
      predicate = MaterialPredicate.or(this.material, other.material);
    }
    return new MaterialValueIngredient(predicate, Math.min(this.minValue, other.minValue), Math.max(this.maxValue, other.maxValue));
  }

  /** Gets the material matching this recipe */
  @Nullable
  public MaterialVariantId getMaterial(ItemStack stack) {
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe) ? recipe.getMaterial().getVariant() : null;
  }


  /* JSON */

  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("type", Serializer.ID.toString());
    Serializer.MATERIAL_FIELD.serialize(this, json);
    if (minValue == maxValue) {
      json.addProperty("value", minValue);
    } else {
      JsonObject value = new JsonObject();
      if (minValue > 0) {
        value.addProperty("min", minValue);
      }
      if (Float.isFinite(maxValue)) {
        value.addProperty("max", maxValue);
      }
      json.add("value", value);
    }
    return json;
  }

  @Override
  public IngredientType<MaterialValueIngredient> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object object) {
    return object == this || object instanceof MaterialValueIngredient other && material.equals(other.material)
      && Float.compare(minValue, other.minValue) == 0 && Float.compare(maxValue, other.maxValue) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(material, minValue, maxValue);
  }


  /** Serializer instance */
  public enum Serializer implements RecordLoadable<MaterialValueIngredient> {
    INSTANCE;
    public static final ResourceLocation ID = TConstruct.getResource("material_value");
    private static final LoadableField<IJsonPredicate<MaterialVariantId>, MaterialValueIngredient> MATERIAL_FIELD = new MaterialPredicateField<>("material", i -> i.material);

    @Override
    public MaterialValueIngredient deserialize(JsonObject json, TypedMap context) {
      float minValue, maxValue;
      JsonElement value = json.get("value");
      if (value == null) {
        throw new com.google.gson.JsonSyntaxException("Material value ingredient requires value");
      }
      if (value.isJsonPrimitive()) {
        minValue = maxValue = value.getAsJsonPrimitive().getAsFloat();
      } else {
        JsonObject object = GsonHelper.convertToJsonObject(value, "value");
        minValue = GsonHelper.getAsFloat(object, "min", 0);
        maxValue = GsonHelper.getAsFloat(object, "max", Float.POSITIVE_INFINITY);
      }
      return new MaterialValueIngredient(MATERIAL_FIELD.get(json, context), minValue, maxValue);
    }

    @Override
    public void serialize(MaterialValueIngredient ingredient, JsonObject json) {
      MATERIAL_FIELD.serialize(ingredient, json);
      if (ingredient.minValue == ingredient.maxValue) {
        json.addProperty("value", ingredient.minValue);
      } else {
        JsonObject range = new JsonObject();
        if (ingredient.minValue > 0) range.addProperty("min", ingredient.minValue);
        if (Float.isFinite(ingredient.maxValue)) range.addProperty("max", ingredient.maxValue);
        json.add("value", range);
      }
    }

    @Override
    public MaterialValueIngredient decode(FriendlyByteBuf buffer, TypedMap context) {
      return new MaterialValueIngredient(
        MATERIAL_FIELD.decode(buffer, context),
        buffer.readFloat(),
        buffer.readFloat()
      );
    }

    @Override
    public void encode(FriendlyByteBuf buffer, MaterialValueIngredient ingredient) {
      MATERIAL_FIELD.encode(buffer, ingredient);
      buffer.writeFloat(ingredient.minValue);
      buffer.writeFloat(ingredient.maxValue);
    }
  }
}
