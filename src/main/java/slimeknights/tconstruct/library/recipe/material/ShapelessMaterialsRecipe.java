package slimeknights.tconstruct.library.recipe.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shapeless recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapelessMaterialsRecipe extends ShapelessRecipe implements MaterialsCraftingTableRecipe {
  /** Number of parts to match */
  @Getter
  private final int partCount;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;

  public ShapelessMaterialsRecipe(ResourceLocation id, String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients, int partCount, List<MaterialVariantId> extraMaterials) {
    super(group, category, result, ingredients);
    this.partCount = partCount;
    this.extraMaterials = extraMaterials;
  }

  public ShapelessMaterialsRecipe(ShapelessRecipe recipe, int partCount, List<MaterialVariantId> extraMaterials) {
    super(recipe.getGroup(), recipe.category(), recipe.result, recipe.getIngredients());
    this.partCount = partCount;
    this.extraMaterials = List.copyOf(extraMaterials);
  }

  @Override
  public List<Ingredient> getParts() {
    return getIngredients();
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    ShapedMaterialsRecipe.setMaterial(stack, material, extraMaterials);
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
    return ShapedMaterialsRecipe.assemble(super.assemble(inventory, registryAccess), inventory, getIngredients(), partCount, false, extraMaterials);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapelessMaterialsRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapelessMaterialsRecipe> {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS;
    static final LoadableField<List<MaterialVariantId>,ShapelessMaterialsRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);
    private static final ShapelessRecipe.Serializer BASE = new ShapelessRecipe.Serializer();
    private static final MapCodec<ShapelessMaterialsRecipe> CODEC = RecordCodecBuilder.<ShapelessMaterialsRecipe>mapCodec(instance -> instance.group(
      BASE.codec().forGetter(recipe -> recipe),
      Codec.INT.fieldOf("parts").forGetter(recipe -> recipe.partCount),
      ShapedMaterialsRecipe.Serializer.EXTRA_MATERIALS_CODEC.optionalFieldOf("extra_materials", List.of()).forGetter(recipe -> recipe.extraMaterials)
    ).apply(instance, ShapelessMaterialsRecipe::new)).flatXmap(Serializer::validate, Serializer::validate);

    private static DataResult<ShapelessMaterialsRecipe> validate(ShapelessMaterialsRecipe recipe) {
      return recipe.partCount < 1 || recipe.partCount > recipe.getIngredients().size()
        ? DataResult.error(() -> "Parts must be between 1 and the number of ingredients")
        : DataResult.success(recipe);
    }

    @Override
    public MapCodec<ShapelessMaterialsRecipe> codec() {
      return CODEC;
    }

    @Override
    public ShapelessMaterialsRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      ShapelessMaterialsRecipe recipe = new ShapelessMaterialsRecipe(BASE.streamCodec().decode(buffer), buffer.readVarInt(), EXTRA_MATERIALS.decode(buffer));
      return validate(recipe).getOrThrow();
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapelessMaterialsRecipe recipe) {
      validate(recipe).getOrThrow();
      BASE.streamCodec().encode(buffer, recipe);
      buffer.writeVarInt(recipe.partCount);
      EXTRA_MATERIALS.encode(buffer, recipe.extraMaterials);
    }
  }
}
