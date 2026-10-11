package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.LoadableOps;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.Objects;

/**
 * Extension of the vanilla ingredient to display materials on items and support matching by materials
 */
public class MaterialIngredient extends NestedIngredient {
  private final IJsonPredicate<MaterialVariantId> material;
  public static final LoadableIngredientSerializer<MaterialIngredient> SERIALIZER = new LoadableIngredientSerializer<>(Serializer.INSTANCE);
  public static final IngredientType<MaterialIngredient> TYPE = SERIALIZER.type("match", "item", "tag", "material", "material_tag");
  protected MaterialIngredient(Ingredient nested, IJsonPredicate<MaterialVariantId> material) {
    super(nested);
    this.material = Objects.requireNonNull(material);
  }

  /** @deprecated use {@link #MaterialIngredient(Ingredient, IJsonPredicate)} */
  @Deprecated(forRemoval = true)
  protected MaterialIngredient(Ingredient nested, MaterialVariantId material, @Nullable TagKey<IMaterial> tag) {
    this(nested, makePredicate(material, tag));
  }

  /** Converts the legacy material and tag into a predicate */
  private static IJsonPredicate<MaterialVariantId> makePredicate(MaterialVariantId material, @Nullable TagKey<IMaterial> tag) {
    // UNKNOWN is the legacy way to express any material
    IJsonPredicate<MaterialVariantId> predicate = material.equals(IMaterial.UNKNOWN.getIdentifier()) ? MaterialPredicate.ANY : MaterialPredicate.variant(material);
    if (tag != null) {
      IJsonPredicate<MaterialVariantId> tagPredicate = MaterialPredicate.tag(tag);
      if (predicate == MaterialPredicate.ANY) {
        predicate = tagPredicate;
      } else {
        predicate = MaterialPredicate.and(predicate, tagPredicate);
      }
    }
    return predicate;
  }

  /** Creates an ingredient matching the given materials */
  public static Ingredient of(Ingredient ingredient, IJsonPredicate<MaterialVariantId> material) {
    return new MaterialIngredient(ingredient, material).toVanilla();
  }

  /** Creates an ingredient matching the given materials */
  public static Ingredient of(ItemLike item, IJsonPredicate<MaterialVariantId> material) {
    return of(Ingredient.of(item), material);
  }

  /** Creates an ingredient matching a specific material */
  public static Ingredient of(Ingredient ingredient) {
    return new MaterialIngredient(ingredient, MaterialPredicate.ANY).toVanilla();
  }

  /** Creates an ingredient matching a single material */
  public static Ingredient of(Ingredient ingredient, MaterialVariantId material) {
    return of(ingredient, MaterialPredicate.variant(material));
  }

  /** Creates an ingredient matching a material tag */
  public static Ingredient of(Ingredient ingredient, TagKey<IMaterial> tag) {
    return of(ingredient, MaterialPredicate.tag(tag));
  }

  /**
   * Creates a new instance from an item with a fixed material
   * @param item      Material item
   * @param material  Material ID
   * @return  Material ingredient instance
   */
  public static Ingredient of(ItemLike item, MaterialVariantId material) {
    return of(Ingredient.of(item), material);
  }

  /**
   * Creates a new instance from an item with a tagged material
   * @param item      Material item
   * @param tag   Material tag
   * @return  Material ingredient instance
   */
  public static Ingredient of(ItemLike item, TagKey<IMaterial> tag) {
    return of(Ingredient.of(item), tag);
  }

  /**
   * Creates a new ingredient matching any material from items
   * @param item  Material item
   * @return  Material ingredient instance
   */
  public static Ingredient of(ItemLike item) {
    return of(Ingredient.of(item));
  }

  /**
   * Creates a new ingredient from a tag
   * @param tag       Tag instance
   * @param material  Material value
   * @return  Material with tag
   */
  public static Ingredient of(TagKey<Item> tag, MaterialVariantId material) {
    return of(Ingredient.of(tag), material);
  }

  /**
   * Creates a new ingredient matching any material from a tag
   * @param tag       Tag instance
   * @return  Material with tag
   */
  public static Ingredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    // check super first, should be faster
    if (stack == null || stack.isEmpty() || !super.test(stack)) {
      return false;
    }
    // no need to read material NBT if the material is the any predicate
    if (material != MaterialPredicate.ANY) {
      return material.matches(IMaterialItem.getMaterialFromStack(stack));
    }
    return true;
  }

  @Override
  public Stream<ItemStack> getItems() {
    if (!MaterialRegistry.isFullyLoaded()) {
      return Arrays.stream(nested.getItems());
    }
    return Arrays.stream(nested.getItems()).flatMap(stack -> MaterialRecipeCache.getAllVariants().stream()
      .filter(material::matches)
      .map(mat -> IMaterialItem.withMaterial(stack, mat))
      .filter(value -> !IMaterialItem.getMaterialFromStack(value).equals(IMaterial.UNKNOWN.getIdentifier())));
  }

  public JsonElement toJson() {
    return SERIALIZER.serialize(this);
  }

  @Override
  public boolean isSimple() {
    return material == MaterialPredicate.ANY && nested.isSimple();
  }

  @Override
  public IngredientType<MaterialIngredient> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object object) {
    return object == this || object instanceof MaterialIngredient other && nested.equals(other.nested) && material.equals(other.material);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nested, material);
  }

  /** Serializer instance */
  public enum Serializer implements RecordLoadable<MaterialIngredient> {
    INSTANCE;
    public static final ResourceLocation ID = TConstruct.getResource("material");
    private static final LoadableField<IJsonPredicate<MaterialVariantId>,MaterialIngredient> MATERIAL_FIELD = new MaterialPredicateField<>("material", i -> i.material);

    @Override
    public MaterialIngredient deserialize(JsonObject json, TypedMap context) {
      // if we have match, parse as a nested object. Without match, just parse the object as vanilla
      Ingredient ingredient;
      if (json.has("match")) {
        ingredient = Ingredient.CODEC_NONEMPTY.parse(LoadableOps.current(), json.get("match")).getOrThrow();
      } else {
        ingredient = Ingredient.Value.CODEC.parse(LoadableOps.current(), json).map(value -> Ingredient.fromValues(Stream.of(value))).getOrThrow();
      }
      IJsonPredicate<MaterialVariantId> material = MATERIAL_FIELD.get(json, context);
      // deprecated tag field
      if (json.has("material_tag")) {
        TConstruct.LOG.warn("Using deprecated tag field on material ingredient");
        IJsonPredicate<MaterialVariantId> tagPredicate = MaterialPredicate.tag(TinkerLoadables.MATERIAL_TAGS.getIfPresent(json, "material_tag"));
        if (material == MaterialPredicate.ANY) {
          material = tagPredicate;
        } else {
          material = MaterialPredicate.and(material, tagPredicate);
        }
      }
      return new MaterialIngredient(ingredient, material);
    }

    @Override
    public void serialize(MaterialIngredient ingredient, JsonObject json) {
      json.add("match", Ingredient.CODEC_NONEMPTY.encodeStart(LoadableOps.current(), ingredient.nested).getOrThrow());
      MATERIAL_FIELD.serialize(ingredient, json);
    }

    @Override
    public MaterialIngredient decode(FriendlyByteBuf buffer, TypedMap context) {
      if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
        throw new IllegalArgumentException("Material ingredient requires a registry-aware network buffer");
      }
      return new MaterialIngredient(
        Ingredient.CONTENTS_STREAM_CODEC.decode(registryBuffer),
        MATERIAL_FIELD.decode(buffer, context)
      );
    }

    @Override
    public void encode(FriendlyByteBuf buffer, MaterialIngredient ingredient) {
      if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
        throw new IllegalArgumentException("Material ingredient requires a registry-aware network buffer");
      }
      Ingredient.CONTENTS_STREAM_CODEC.encode(registryBuffer, ingredient.nested);
      MATERIAL_FIELD.encode(buffer, ingredient);
    }
  }
}
