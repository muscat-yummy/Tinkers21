package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.MantleRecipes;
import slimeknights.mantle.recipe.helper.LoggingRecipeSerializer;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/** Recipe which sets the texture for a {@link slimeknights.mantle.block.RetexturedBlock} based on an ingredient input. */
@SuppressWarnings("WeakerAccess")
public class ShapedRetexturedRecipe extends ShapedRecipe {
  /** Ingredient used to determine the texture on the output */
  @Getter
  private final Ingredient texture;
  private final boolean matchAll;
  @Nullable
  private final String textureKey;

  /** Creates a new recipe using the passed parameters */
  protected ShapedRetexturedRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification, Ingredient texture, boolean matchAll) {
    super(group, category, ShapedFallbackRecipe.pattern(width, height, ingredients), result, showNotification);
    this.texture = texture;
    this.matchAll = matchAll;
    this.textureKey = null;
  }

  /**
   * Creates a new recipe using an existing shaped recipe
   * @param orig       Shaped recipe to copy
   * @param texture    Ingredient to use for the texture
   * @param matchAll   If true, all inputs must match for the recipe to match
   */
  protected ShapedRetexturedRecipe(ShapedRecipe orig, Ingredient texture, boolean matchAll) {
    this(orig, texture, matchAll, null);
  }

  private ShapedRetexturedRecipe(ShapedRecipe orig, Ingredient texture, boolean matchAll, @Nullable String textureKey) {
    super(orig.getGroup(), orig.category(), orig.pattern, orig.result, orig.showNotification());
    this.texture = texture;
    this.matchAll = matchAll;
    this.textureKey = textureKey;
  }

  /**
   * Gets the output using the given texture
   * @param texture  Texture to use
   * @return  Output with texture. Will be blank if the input is not a block
   */
  public ItemStack getResultItem(Item texture, HolderLookup.Provider access) {
    return RetexturedHelper.setTexture(getResultItem(access).copy(), Block.byItem(texture));
  }

  @Override
  public ItemStack assemble(CraftingInput craftMatrix, HolderLookup.Provider access) {
    ItemStack result = super.assemble(craftMatrix, access);
    Block currentTexture = null;
    for (int i = 0; i < craftMatrix.size(); i++) {
      ItemStack stack = craftMatrix.getItem(i);
      if (!stack.isEmpty() && texture.test(stack)) {
        // fetch texture from the block if it has one
        Block block = RetexturedHelper.getTexture(stack);
        // assuming it does not, use the block itself as the texture (provided it is not the result that is)
        if (block == Blocks.AIR && stack.getItem() != result.getItem()) {
          block = Block.byItem(stack.getItem());
        }
        // if no texture, skip
        if (block == Blocks.AIR) {
          continue;
        }

        // if we have not found a texture yet, store the found block
        if (currentTexture == null) {
          currentTexture = block;
          // match all means we must check the rest. If not match all, we can be done
          if (!matchAll) {
            break;
          }

          // if we found a texture before, must match or we do no texture
        } else if (currentTexture != block) {
          currentTexture = null;
          break;
        }
      }
    }

    // set the texture if found. No texture will use the fallback
    if (currentTexture != null) {
      return RetexturedHelper.setTexture(result, currentTexture);
    }
    return result;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_RETEXTURED.get();
  }

  public static class Serializer implements LoggingRecipeSerializer<ShapedRetexturedRecipe> {
    private static final ShapedRecipe.Serializer BASE = new ShapedRecipe.Serializer();
    private static final MapCodec<ShapedRetexturedRecipe> CODEC = new MapCodec<>() {
      @Override
      public <O> DataResult<ShapedRetexturedRecipe> decode(DynamicOps<O> ops, MapLike<O> input) {
        return BASE.codec().decode(ops, input).flatMap(base -> {
          O rawTexture = input.get("texture");
          if (rawTexture == null) {
            return DataResult.error(() -> "Missing texture ingredient or pattern symbol");
          }
          var symbol = ops.getStringValue(rawTexture).result();
          String textureKey = symbol.orElse(null);
          O ingredient = rawTexture;
          if (textureKey != null) {
            if (textureKey.length() != 1) {
              return DataResult.error(() -> "Texture symbol must be one character");
            }
            O rawKey = input.get("key");
            if (rawKey == null) {
              return DataResult.error(() -> "Missing pattern key for texture symbol");
            }
            var key = ops.getMap(rawKey).result();
            ingredient = key.isPresent() ? key.get().get(textureKey) : null;
            if (ingredient == null) {
              return DataResult.error(() -> "Undefined texture symbol: " + textureKey);
            }
          } else {
            Mantle.logger.warn("Using deprecated ingredient format on 'texture' for mantle:crafting_shaped_retextured. Use key instead.");
          }
          O rawMatchAll = input.get("match_all");
          DataResult<Boolean> matchAll = rawMatchAll == null ? DataResult.success(false) : ops.getBooleanValue(rawMatchAll);
          return Ingredient.CODEC_NONEMPTY.parse(ops, ingredient).flatMap(texture ->
            matchAll.map(all -> new ShapedRetexturedRecipe(base, texture, all, textureKey)));
        });
      }

      @Override
      public <O> RecordBuilder<O> encode(ShapedRetexturedRecipe recipe, DynamicOps<O> ops, RecordBuilder<O> prefix) {
        prefix = BASE.codec().encode(recipe, ops, prefix);
        if (recipe.textureKey != null) {
          prefix.add("texture", ops.createString(recipe.textureKey));
        } else {
          prefix.add("texture", Ingredient.CODEC_NONEMPTY.encodeStart(ops, recipe.texture));
        }
        prefix.add("match_all", ops.createBoolean(recipe.matchAll));
        return prefix;
      }

      @Override
      public <O> Stream<O> keys(DynamicOps<O> ops) {
        return Stream.concat(BASE.codec().keys(ops), Stream.of(ops.createString("texture"), ops.createString("match_all")));
      }
    };

    @Override
    public MapCodec<ShapedRetexturedRecipe> codec() {
      return CODEC;
    }

    @Override
    public ShapedRetexturedRecipe fromNetworkSafe(RegistryFriendlyByteBuf buffer) {
      return new ShapedRetexturedRecipe(BASE.streamCodec().decode(buffer), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readBoolean());
    }

    @Override
    public void toNetworkSafe(RegistryFriendlyByteBuf buffer, ShapedRetexturedRecipe recipe) {
      BASE.streamCodec().encode(buffer, recipe);
      Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.texture);
      buffer.writeBoolean(recipe.matchAll);
    }
  }
}
