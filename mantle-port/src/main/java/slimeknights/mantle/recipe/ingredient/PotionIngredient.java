package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching one potion, including items with no potion contents. */
public class PotionIngredient extends ItemIngredient {
  private static final StringLoadable<Potion> POTION = StringLoadable.DEFAULT.flatXmap(
    name -> name.equals("minecraft:empty") ? null : Loadables.POTION.parseString(name, "potion", TypedMap.EMPTY),
    potion -> potion == null ? "minecraft:empty" : Loadables.POTION.getString(potion));
  public static final LoadableIngredientSerializer<PotionIngredient> SERIALIZER = new LoadableIngredientSerializer<>(RecordLoadable.create(
    ItemsField.INSTANCE, TAG_FIELD,
    POTION.nullableField("potion", ingredient -> ingredient.potion),
    PotionIngredient::new));
  public static final IngredientType<PotionIngredient> TYPE = SERIALIZER.type("item", "tag", "potion");

  @Nullable
  private final Potion potion;

  protected PotionIngredient(List<Item> items, @Nullable TagKey<Item> tag, @Nullable Potion potion) {
    super(items, tag);
    this.potion = potion;
  }

  public static Ingredient of(@Nullable Potion potion, List<ItemLike> items) {
    return new PotionIngredient(toItem(items), null, potion).toVanilla();
  }

  public static Ingredient of(@Nullable Potion potion, ItemLike... items) {
    return of(potion, Arrays.asList(items));
  }

  public static Ingredient of(@Nullable Potion potion, TagKey<Item> tag) {
    return new PotionIngredient(List.of(), tag, potion).toVanilla();
  }

  public static Ingredient of(Holder<Potion> potion, List<ItemLike> items) {
    return of(potion.value(), items);
  }

  public static Ingredient of(Holder<Potion> potion, ItemLike... items) {
    return of(potion.value(), items);
  }

  public static Ingredient of(Holder<Potion> potion, TagKey<Item> tag) {
    return of(potion.value(), tag);
  }

  protected static ItemStack withPotion(ItemStack stack, @Nullable Potion potion) {
    stack.set(DataComponents.POTION_CONTENTS, potion == null ? PotionContents.EMPTY
      : new PotionContents(BuiltInRegistries.POTION.wrapAsHolder(potion)));
    return stack;
  }

  @Override
  public Stream<ItemStack> getItems() {
    return super.getItems().map(stack -> withPotion(stack.copy(), potion));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && super.test(stack)
      && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion()
        .map(Holder::value).orElse(null) == potion;
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<PotionIngredient> getType() {
    return TYPE;
  }

  public JsonElement toJson() {
    return SERIALIZER.serialize(this);
  }

  @Override
  public boolean equals(Object object) {
    return super.equals(object) && object instanceof PotionIngredient other && potion == other.potion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), potion);
  }
}
