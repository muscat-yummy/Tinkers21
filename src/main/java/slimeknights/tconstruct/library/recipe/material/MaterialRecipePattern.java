package slimeknights.tconstruct.library.recipe.material;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Retains ingredient identity between the native shaped pattern and its ordered material parts. */
public final class MaterialRecipePattern {
  private MaterialRecipePattern() {}

  public static ShapedRecipePattern create(int width, int height, NonNullList<Ingredient> ingredients) {
    if (width < 1 || height < 1 || width > ShapedRecipePattern.getMaxWidth()
        || height > ShapedRecipePattern.getMaxHeight() || ingredients.size() != width * height) {
      throw new IllegalArgumentException("Invalid material recipe dimensions");
    }
    Map<Character,Ingredient> key = new LinkedHashMap<>();
    Map<Ingredient,Character> symbols = new IdentityHashMap<>();
    List<String> rows = new ArrayList<>(height);
    char next = '!';
    for (int y = 0; y < height; y++) {
      StringBuilder row = new StringBuilder(width);
      for (int x = 0; x < width; x++) {
        Ingredient ingredient = ingredients.get(x + y * width);
        if (ingredient.isEmpty()) {
          row.append(' ');
        } else {
          Character symbol = symbols.get(ingredient);
          if (symbol == null) {
            symbol = next++;
            symbols.put(ingredient, symbol);
            key.put(symbol, ingredient);
          }
          row.append(symbol);
        }
      }
      rows.add(row.toString());
    }
    return ShapedRecipePattern.of(key, rows);
  }

  public static List<Ingredient> parts(ShapedRecipePattern pattern, String symbols) {
    Map<Character,Ingredient> key = pattern.data.orElseThrow(() ->
      new IllegalArgumentException("Material recipe pattern has no source keys")).key();
    List<Ingredient> parts = new ArrayList<>(symbols.length());
    for (int i = 0; i < symbols.length(); i++) {
      Ingredient ingredient = key.get(symbols.charAt(i));
      if (ingredient == null) {
        throw new IllegalArgumentException("Parts references undefined symbol '" + symbols.charAt(i) + "'");
      }
      parts.add(ingredient);
    }
    return List.copyOf(parts);
  }

  public static String symbols(ShapedRecipePattern pattern, List<Ingredient> parts) {
    Map<Character,Ingredient> key = pattern.data.orElseThrow(() ->
      new IllegalArgumentException("Material recipe pattern has no source keys")).key();
    StringBuilder result = new StringBuilder(parts.size());
    for (Ingredient part : parts) {
      Character symbol = null;
      for (var entry : key.entrySet()) {
        if (entry.getValue() == part) {
          symbol = entry.getKey();
          break;
        }
      }
      if (symbol == null) {
        throw new IllegalArgumentException("Material part is not an ingredient of the shaped pattern");
      }
      result.append(symbol);
    }
    return result.toString();
  }

  public static Ingredient indexed(List<Ingredient> ingredients, int index) {
    if (index < 0 || index >= ingredients.size()) {
      throw new IllegalArgumentException("Invalid material recipe ingredient index: " + index);
    }
    return ingredients.get(index);
  }

  public record WireData(ShapedRecipePattern pattern, List<Ingredient> parts) {}

  /** One ingredient table preserves aliasing between repeated pattern symbols and ordered parts. */
  public static void write(RegistryFriendlyByteBuf buffer, List<Ingredient> inputs, List<Ingredient> parts) {
    List<Ingredient> distinct = new ArrayList<>();
    IdentityHashMap<Ingredient,Integer> indices = new IdentityHashMap<>();
    for (Ingredient ingredient : inputs) {
      if (!indices.containsKey(ingredient)) {
        indices.put(ingredient, distinct.size());
        distinct.add(ingredient);
      }
    }
    buffer.writeVarInt(distinct.size());
    for (Ingredient ingredient : distinct) {
      Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
    }
    for (Ingredient ingredient : inputs) {
      buffer.writeVarInt(indices.get(ingredient));
    }
    buffer.writeVarInt(parts.size());
    for (Ingredient ingredient : parts) {
      Integer index = indices.get(ingredient);
      if (index == null) {
        throw new IllegalArgumentException("Material part is not an ingredient of the shaped pattern");
      }
      buffer.writeVarInt(index);
    }
  }

  public static WireData read(RegistryFriendlyByteBuf buffer, int width, int height) {
    if (width < 1 || height < 1 || width > ShapedRecipePattern.getMaxWidth()
        || height > ShapedRecipePattern.getMaxHeight()) {
      throw new IllegalArgumentException("Invalid material recipe dimensions");
    }
    int size = buffer.readVarInt();
    if (size < 1 || size > width * height) {
      throw new IllegalArgumentException("Invalid distinct ingredient count");
    }
    List<Ingredient> distinct = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      distinct.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
    }
    NonNullList<Ingredient> inputs = NonNullList.withSize(width * height, Ingredient.EMPTY);
    for (int i = 0; i < inputs.size(); i++) {
      inputs.set(i, indexed(distinct, buffer.readVarInt()));
    }
    size = buffer.readVarInt();
    if (size < 0 || size > 32767) {
      throw new IllegalArgumentException("Invalid material part count");
    }
    List<Ingredient> parts = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      parts.add(indexed(distinct, buffer.readVarInt()));
    }
    return new WireData(create(width, height, inputs), List.copyOf(parts));
  }
}
