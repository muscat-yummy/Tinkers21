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
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shaped recipe with a number of {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialIngredient} and
 * {@link slimeknights.tconstruct.library.recipe.ingredient.MaterialValueIngredient} to set the materials of the result.
 */
public class ShapedMaterialsRecipe extends ShapedRecipe implements MaterialsCraftingTableRecipe {
  /** List of tool parts to search for in the final recipe */
  @Getter
  private final List<Ingredient> parts;
  /**
   * If true, a part may show up multiple times in the inputs, and all copies should match.
   * If false, only the first instance of a part is checked for each input, allowing a tool with the same part multiple times.
   */
  private final boolean checkRepeats;
  private final String partPattern;
  /** List of additional materials to add beyond the parts */
  @Getter
  private final List<MaterialVariantId> extraMaterials;
  public ShapedMaterialsRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    this(group, category, width, height, ingredients, result, showNotification, parts, extraMaterials);
  }

  /** Native recipe payload; the enclosing RecipeHolder owns its resource ID. */
  public ShapedMaterialsRecipe(String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification, List<Ingredient> parts, List<MaterialVariantId> extraMaterials) {
    super(group, category, MaterialRecipePattern.create(width, height, ingredients), result, showNotification);
    this.parts = List.copyOf(parts);
    this.partPattern = MaterialRecipePattern.symbols(pattern, parts);
    this.checkRepeats = parts.stream().unordered().distinct().count() == parts.size();
    this.extraMaterials = extraMaterials;
  }

  public ShapedMaterialsRecipe(ShapedRecipe recipe, String partPattern, List<MaterialVariantId> extraMaterials) {
    super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.result, recipe.showNotification());
    this.partPattern = partPattern;
    this.parts = MaterialRecipePattern.parts(pattern, partPattern);
    this.checkRepeats = parts.stream().unordered().distinct().count() == parts.size();
    this.extraMaterials = List.copyOf(extraMaterials);
  }

  @Override
  public int getPartCount() {
    return parts.size();
  }

  /**
   * Finds materials for each of the parts
   * @return Array of all matched materials. Array will have no null entries, though the array may be null if no match was found.
   */
  @Nullable
  static MaterialVariantId[] findMaterials(CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats) {
    // want one material for each
    MaterialVariantId[] materials = new MaterialVariantId[partCount];
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        for (int p = 0; p < partCount; p++) {
          MaterialVariantId current = materials[p];
          // if we have not found the material yet, or repeats are considered the same material, test the ingredient
          if ((current == null || checkRepeats) && parts.get(p).test(stack)) {
            MaterialVariantId matched = MaterialRecipeCache.getMaterial(stack);
            // first occurrence? thats our material
            if (current == null) {
              materials[p] = matched;
              break;
            } else if (!current.matchesVariant(matched)) {
              // if same material but different variants, just discard the variant
              if (current.getId().equals(matched.getId())) {
                materials[p] = current.getId();
                break;
              } else {
                // if different materials, no match
                return null;
              }
            }
          }
        }
      }
    }
    // ensure we found all materials needed
    for (int p = 0; p < partCount; p++) {
      if (materials[p] == null) {
        return null;
      }
    }
    return materials;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level level) {
    if (!super.matches(inventory, level)) {
      return false;
    }
    // ensure all part materials matched and we found all parts
    return findMaterials(inventory, parts, parts.size(), checkRepeats) != null;
  }

  /** Common logic to this and {@link ShapedMaterialsRecipe} */
  public static void setMaterial(ItemStack stack, MaterialVariantId material, List<MaterialVariantId> extraMaterials) {
    if (extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
      materialItem.setMaterial(stack, material);
    } else {
      MaterialNBT.Builder builder = MaterialNBT.builder();
      builder.add(material);
      for (MaterialVariantId extraMaterial : extraMaterials) {
        builder.add(extraMaterial);
      }
      ToolStack.from(stack).setMaterials(builder.build());
    }
  }

  /** Sets the material for the given stack */
  @Override
  public void setMaterial(ItemStack stack, MaterialVariantId material) {
    setMaterial(stack, material, extraMaterials);
  }

  /** Assembles the item with material information */
  static ItemStack assemble(ItemStack stack, CraftingInput inventory, List<Ingredient> parts, int partCount, boolean checkRepeats, List<MaterialVariantId> extraMaterials) {
    MaterialVariantId[] materials = findMaterials(inventory, parts, partCount, checkRepeats);
    if (materials != null) {
      // if the result is a tool part, and we only have the one material, set its material
      if (materials.length == 1 && extraMaterials.isEmpty() && stack.getItem() instanceof IMaterialItem materialItem) {
        return materialItem.setMaterial(stack, materials[0]);
      }
      MaterialNBT.Builder builder = MaterialNBT.builder();
      // add each material
      for (MaterialVariantId material : materials) {
        builder.add(material);
      }
      // add extra materials
      builder.add(extraMaterials);
      ToolStack.from(stack).setMaterials(builder.build());
    }
    return stack;
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryAccess) {
    return assemble(super.assemble(inventory, registryAccess), inventory, parts, parts.size(), checkRepeats, extraMaterials);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerTables.shapedMaterialsRecipeSerializer.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapedMaterialsRecipe> {
    static final Loadable<List<MaterialVariantId>> EXTRA_MATERIALS = MaterialVariantId.LOADABLE.list(0);
    static final Codec<List<MaterialVariantId>> EXTRA_MATERIALS_CODEC = Codec.STRING.<MaterialVariantId>comapFlatMap(text -> {
      MaterialVariantId id = MaterialVariantId.tryParse(text);
      return id == null ? DataResult.error(() -> "Invalid material variant: " + text) : DataResult.success(id);
    }, MaterialVariantId::toString).listOf();
    static final LoadableField<List<MaterialVariantId>, ShapedMaterialsRecipe> MATERIAL_FIELD = EXTRA_MATERIALS.defaultField("extra_materials", List.of(), r -> r.extraMaterials);
    private static final ShapedRecipe.Serializer BASE = new ShapedRecipe.Serializer();
    private record Definition(ShapedRecipe recipe, String parts, List<MaterialVariantId> extraMaterials) {}
    private static final MapCodec<ShapedMaterialsRecipe> CODEC = RecordCodecBuilder.<Definition>mapCodec(instance -> instance.group(
      BASE.codec().forGetter(Definition::recipe),
      Codec.STRING.fieldOf("parts").forGetter(Definition::parts),
      EXTRA_MATERIALS_CODEC.optionalFieldOf("extra_materials", List.of()).forGetter(Definition::extraMaterials)
    ).apply(instance, Definition::new)).flatXmap(definition -> {
      try {
        return DataResult.success(new ShapedMaterialsRecipe(definition.recipe(), definition.parts(), definition.extraMaterials()));
      } catch (IllegalArgumentException exception) {
        return DataResult.error(exception::getMessage);
      }
    }, recipe -> DataResult.success(new Definition(recipe, recipe.partPattern, recipe.extraMaterials)));

    @Override
    public MapCodec<ShapedMaterialsRecipe> codec() {
      return CODEC;
    }

    @Override
    public ShapedMaterialsRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      int width = buffer.readVarInt();
      int height = buffer.readVarInt();
      if (width < 1 || height < 1 || width > ShapedRecipePattern.getMaxWidth() || height > ShapedRecipePattern.getMaxHeight()) {
        throw new IllegalArgumentException("Invalid material recipe dimensions");
      }
      String group = buffer.readUtf();
      CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
      ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
      boolean showNotification = buffer.readBoolean();
      List<MaterialVariantId> extraMaterials = EXTRA_MATERIALS.decode(buffer);
      MaterialRecipePattern.WireData ingredients = MaterialRecipePattern.read(buffer, width, height);
      ShapedRecipe base = new ShapedRecipe(group, category, ingredients.pattern(), result, showNotification);
      return new ShapedMaterialsRecipe(base, MaterialRecipePattern.symbols(ingredients.pattern(), ingredients.parts()), extraMaterials);
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapedMaterialsRecipe recipe) {
      buffer.writeVarInt(recipe.getWidth());
      buffer.writeVarInt(recipe.getHeight());
      buffer.writeUtf(recipe.getGroup());
      buffer.writeEnum(recipe.category());
      ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
      buffer.writeBoolean(recipe.showNotification());
      EXTRA_MATERIALS.encode(buffer, recipe.extraMaterials);
      MaterialRecipePattern.write(buffer, recipe.getIngredients(), recipe.parts);
    }
  }
}
