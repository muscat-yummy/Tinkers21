package slimeknights.tconstruct.library.recipe.material;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shaped recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the material of the result.
 * @deprecated use {@link ShapedMaterialsRecipe}, which requires specifying the ingredients for each part.
 */
@Deprecated
public class ShapedMaterialRecipe extends ShapedRecipe {
  private MaterialValueIngredient material;
  private final List<MaterialVariantId> extraMaterials;
  public ShapedMaterialRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification, List<MaterialVariantId> extraMaterials) {
    super(group, category, MaterialRecipePattern.create(width, height, ingredients), result, showNotification);
    this.extraMaterials = extraMaterials;
  }

  public ShapedMaterialRecipe(ShapedRecipe recipe, List<MaterialVariantId> extraMaterials) {
    super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.result, recipe.showNotification());
    this.extraMaterials = List.copyOf(extraMaterials);
  }

  /** @deprecated use {@link #ShapedMaterialRecipe(ResourceLocation,String,CraftingBookCategory,int,int,NonNullList,ItemStack,boolean,List)} */
  @Deprecated(forRemoval = true)
  public ShapedMaterialRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification) {
    this(id, group, category, width, height, ingredients, result, showNotification, List.of());
  }

  /** @deprecated use {@link #ShapedMaterialRecipe(ShapedRecipe,List)} */
  @Deprecated(forRemoval = true)
  public ShapedMaterialRecipe(ShapedRecipe recipe) {
    this(recipe, List.of());
  }

  /** Gets the material to match */
  @Nullable
  public MaterialValueIngredient getMaterial() {
    if (material == null) {
      // assume all material ingredients match the same stat type
      for (Ingredient ingredient : getIngredients()) {
        // collect all ingredients that match
        if (ingredient.getCustomIngredient() instanceof MaterialValueIngredient materialValue) {
          if (material == null) {
            material = materialValue;
          } else {
            // ensure the stat type matches, and expand the range
            material = material.merge(materialValue);
          }
        }
      }
      // if we found no materials, that is also an issue
      if (material == null) {
        TConstruct.LOG.error("No material ingredient found for shaped material recipe in group {}, this indicates a broken recipe", getGroup());
      }
    }
    return material;
  }

  @Nullable
  private MaterialVariantId findMaterial(CraftingInput inventory) {
    MaterialValueIngredient material = getMaterial();
    if (material == null) {
      return null;
    }
    // ensure same material in all slots
    MaterialVariantId firstMaterial = null;
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        // ignore anything that does not meet our requirements
        MaterialVariantId matchedMaterial = material.getMaterial(stack);
        if (matchedMaterial != null) {
          // first match is set
          if (firstMaterial == null) {
            firstMaterial = matchedMaterial;
          } else if (!firstMaterial.matchesVariant(matchedMaterial)) {
            // if same material but different variants, just discard the variant
            if (firstMaterial.getId().equals(matchedMaterial.getId())) {
              firstMaterial = firstMaterial.getId();
            } else {
              // if different materials, no match
              return null;
            }
          }
        }
      }
    }
    return firstMaterial;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    if (!super.matches(inventory, level)) {
      return false;
    }

    // must have a material to match, no mixing
    return findMaterial(inventory) != null;
  }

  /** Sets the material for the given stack */
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
    ItemStack stack = super.assemble(inventory, registryAccess);
    MaterialVariantId material = findMaterial(inventory);
    if (material != null) {
      setMaterial(stack, material);
    }
    return stack;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapedMaterialRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapedMaterialRecipe> {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS;
    static final LoadableField<List<MaterialVariantId>,ShapedMaterialRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);
    private static final ShapedRecipe.Serializer BASE = new ShapedRecipe.Serializer();
    private static final MapCodec<ShapedMaterialRecipe> CODEC = RecordCodecBuilder.<ShapedMaterialRecipe>mapCodec(instance -> instance.group(
      BASE.codec().forGetter(recipe -> recipe),
      ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS_CODEC.optionalFieldOf("extra_materials", List.of()).forGetter(recipe -> recipe.extraMaterials)
    ).apply(instance, ShapedMaterialRecipe::new)).flatXmap(recipe ->
      recipe.getMaterial() == null ? DataResult.error(() -> "Invalid material ingredients for shaped material recipe") : DataResult.success(recipe),
      DataResult::success);

    @Override
    public MapCodec<ShapedMaterialRecipe> codec() {
      return CODEC;
    }

    @Override
    public ShapedMaterialRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      return new ShapedMaterialRecipe(BASE.streamCodec().decode(buffer), EXTRA_MATERIALS.decode(buffer));
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapedMaterialRecipe recipe) {
      BASE.streamCodec().encode(buffer, recipe);
      EXTRA_MATERIALS.encode(buffer, recipe.extraMaterials);
    }
  }
}
