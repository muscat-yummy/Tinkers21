package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

/** Matches any potion on the selected items and displays every registered potion variant. */
public class PotionDisplayIngredient extends ItemIngredient {
  public static final LoadableIngredientSerializer<PotionDisplayIngredient> SERIALIZER = new LoadableIngredientSerializer<>(
    RecordLoadable.create(ItemsField.INSTANCE, TAG_FIELD, PotionDisplayIngredient::new));
  public static final IngredientType<PotionDisplayIngredient> TYPE = SERIALIZER.type("item", "tag");

  protected PotionDisplayIngredient(List<Item> items, @Nullable TagKey<Item> tag) {
    super(items, tag);
  }

  public static Ingredient of(List<ItemLike> items) {
    return new PotionDisplayIngredient(toItem(items), null).toVanilla();
  }

  public static Ingredient of(ItemLike... items) {
    return of(List.of(items));
  }

  public static Ingredient of(TagKey<Item> tag) {
    return new PotionDisplayIngredient(List.of(), tag).toVanilla();
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public Stream<ItemStack> getItems() {
    List<ItemStack> parent = super.getItems().toList();
    return BuiltInRegistries.POTION.stream().flatMap(potion ->
      parent.stream().map(stack -> PotionIngredient.withPotion(stack.copy(), potion)));
  }

  @Override
  public IngredientType<PotionDisplayIngredient> getType() {
    return TYPE;
  }

  public JsonElement toJson() {
    return SERIALIZER.serialize(this);
  }
}
