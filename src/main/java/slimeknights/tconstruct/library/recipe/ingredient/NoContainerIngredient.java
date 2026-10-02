package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Matches the nested ingredient only when the stack has no crafting remainder. */
public class NoContainerIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "no_container");
  /** Retain both legacy flat item/tag JSON and the nested match representation. */
  private static final MapCodec<Ingredient> MATCH_CODEC = new MapCodec<>() {
    @Override
    public <T> DataResult<Ingredient> decode(DynamicOps<T> ops, MapLike<T> input) {
      T match = input.get("match");
      if (match != null) {
        return Ingredient.CODEC_NONEMPTY.parse(ops, match);
      }
      return Ingredient.Value.CODEC.parse(ops, ops.createMap(input.entries()))
        .map(value -> Ingredient.fromValues(Stream.of(value)));
    }

    @Override
    public <T> RecordBuilder<T> encode(Ingredient nested, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      return prefix.add("match", Ingredient.CODEC_NONEMPTY.encodeStart(ops, nested));
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.of("match", "item", "tag").map(ops::createString);
    }
  };
  public static final MapCodec<NoContainerIngredient> CODEC =
    MATCH_CODEC.xmap(NoContainerIngredient::new, value -> value.nested);
  public static final IngredientType<NoContainerIngredient> TYPE = new IngredientType<>(CODEC);
  protected final Ingredient nested;

  protected NoContainerIngredient(Ingredient nested) {
    this.nested = Objects.requireNonNull(nested);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && nested.test(stack) && !stack.hasCraftingRemainingItem();
  }

  @Override
  public Stream<ItemStack> getItems() {
    return Arrays.stream(nested.getItems());
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<NoContainerIngredient> getType() {
    return TYPE;
  }

  public static Ingredient of(Ingredient ingredient) {
    return new NoContainerIngredient(ingredient).toVanilla();
  }

  public static Ingredient of(ItemLike... items) {
    return of(Ingredient.of(items));
  }

  public static Ingredient of(ItemStack... stacks) {
    return of(Ingredient.of(stacks));
  }

  public static Ingredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }

  @Override
  public boolean equals(Object object) {
    return object == this || object instanceof NoContainerIngredient other && nested.equals(other.nested);
  }

  @Override
  public int hashCode() {
    return nested.hashCode();
  }
}
