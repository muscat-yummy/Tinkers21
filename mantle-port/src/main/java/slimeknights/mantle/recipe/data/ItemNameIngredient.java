package slimeknights.mantle.recipe.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.Arrays;
import java.util.List;

/** Datagen factory for optional-mod items without registering fake item instances. */
public final class ItemNameIngredient {
  private ItemNameIngredient() {}
  public static Ingredient from(List<ResourceLocation> names) {
    if (names.isEmpty()) { throw new IllegalArgumentException("At least one item name is required"); }
    if (names.size() == 1) { return new DatagenIngredient(forName(names.getFirst())).toVanilla(); }
    JsonArray items = new JsonArray();
    names.forEach(name -> items.add(forName(name)));
    JsonObject compound = new JsonObject();
    compound.addProperty("type", "neoforge:compound");
    compound.add("ingredients", items);
    return new DatagenIngredient(compound).toVanilla();
  }
  public static Ingredient from(ResourceLocation... names) { return from(Arrays.asList(names)); }
  private static JsonObject forName(ResourceLocation name) {
    JsonObject json = new JsonObject();
    json.addProperty("item", name.toString());
    return json;
  }
}
