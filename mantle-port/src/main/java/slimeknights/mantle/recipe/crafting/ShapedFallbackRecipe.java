package slimeknights.mantle.recipe.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.MantleRecipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A shaped recipe that yields to matching vanilla shaped or shapeless alternatives. */
public class ShapedFallbackRecipe extends ShapedRecipe {
  private final List<ResourceLocation> alternatives;

  /** Legacy constructor; recipe identity now belongs to RecipeHolder. */
  public ShapedFallbackRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack output, List<ResourceLocation> alternatives) {
    super(group, category, pattern(width, height, ingredients), output);
    this.alternatives = List.copyOf(alternatives);
  }

  public ShapedFallbackRecipe(ShapedRecipe base, List<ResourceLocation> alternatives) {
    super(base.getGroup(), base.category(), base.pattern, base.result, base.showNotification());
    this.alternatives = List.copyOf(alternatives);
  }

  /** Retains the JSON pattern data when constructing a recipe through the legacy API. */
  private static ShapedRecipePattern pattern(int width, int height, NonNullList<Ingredient> ingredients) {
    if (width <= 0 || height <= 0 || ingredients.size() != width * height) {
      throw new IllegalArgumentException("Invalid shaped recipe dimensions");
    }
    Map<Character,Ingredient> key = new LinkedHashMap<>();
    List<String> rows = new ArrayList<>(height);
    int symbol = '!';
    for (int y = 0; y < height; y++) {
      StringBuilder row = new StringBuilder(width);
      for (int x = 0; x < width; x++) {
        Ingredient ingredient = ingredients.get(x + y * width);
        if (ingredient.isEmpty()) {
          row.append(' ');
        } else {
          char token = (char) symbol++;
          key.put(token, ingredient);
          row.append(token);
        }
      }
      rows.add(row.toString());
    }
    return new ShapedRecipePattern(width, height, ingredients, Optional.of(new ShapedRecipePattern.Data(key, rows)));
  }

  @Override
  public boolean matches(CraftingInput input, Level level) {
    if (!super.matches(input, level)) {
      return false;
    }
    RecipeManager manager = level.getRecipeManager();
    // Resolve current holders so a data-pack reload cannot leave stale alternatives cached.
    return alternatives.stream()
      .map(manager::byKey)
      .flatMap(Optional::stream)
      .map(RecipeHolder::value)
      .filter(recipe -> recipe.getClass() == ShapedRecipe.class || recipe.getClass() == ShapelessRecipe.class)
      .map(recipe -> (CraftingRecipe) recipe)
      .noneMatch(recipe -> recipe.matches(input, level));
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return MantleRecipes.CRAFTING_SHAPED_FALLBACK.get();
  }

  public static class Serializer implements RecipeSerializer<ShapedFallbackRecipe> {
    private static final ShapedRecipe.Serializer BASE = new ShapedRecipe.Serializer();
    private static final MapCodec<ShapedFallbackRecipe> CODEC = RecordCodecBuilder.<ShapedFallbackRecipe>mapCodec(instance -> instance.group(
      BASE.codec().forGetter(recipe -> recipe),
      ResourceLocation.CODEC.listOf().fieldOf("alternatives").forGetter(recipe -> recipe.alternatives)
    ).apply(instance, ShapedFallbackRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf,ShapedFallbackRecipe> STREAM_CODEC = StreamCodec.of(
      (buffer, recipe) -> {
        BASE.streamCodec().encode(buffer, recipe);
        buffer.writeCollection(recipe.alternatives, (buf, id) -> buf.writeResourceLocation(id));
      },
      buffer -> new ShapedFallbackRecipe(BASE.streamCodec().decode(buffer), buffer.readList(buf -> buf.readResourceLocation())));

    @Override
    public MapCodec<ShapedFallbackRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf,ShapedFallbackRecipe> streamCodec() {
      return STREAM_CODEC;
    }
  }
}
