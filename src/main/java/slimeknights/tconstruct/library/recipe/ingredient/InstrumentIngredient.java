package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Matches an instrument item using its native component, with legacy custom-data fallback. */
public class InstrumentIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "instrument");

  private record Fields(Item item, Optional<ResourceKey<Instrument>> instrument, Optional<TagKey<Instrument>> ignore) {}
  private static final MapCodec<Fields> FIELDS = RecordCodecBuilder.mapCodec(instance -> instance.group(
    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Fields::item),
    ResourceKey.codec(Registries.INSTRUMENT).optionalFieldOf("instrument").forGetter(Fields::instrument),
    TagKey.codec(Registries.INSTRUMENT).optionalFieldOf("ignore").forGetter(Fields::ignore)
  ).apply(instance, Fields::new));
  public static final MapCodec<InstrumentIngredient> CODEC = FIELDS.flatXmap(fields -> {
    if (fields.instrument().isEmpty() && fields.ignore().isEmpty()) {
      return DataResult.error(() -> "Invalid InstrumentIngredient: must set either 'instrument' or 'ignore'");
    }
    // Legacy JSON gives a specific instrument priority over an ignore tag.
    return DataResult.success(new InstrumentIngredient(fields.item(), fields.instrument().orElse(null),
      fields.instrument().isPresent() ? null : fields.ignore().orElse(null)));
  }, value -> DataResult.success(new Fields(value.item, Optional.ofNullable(value.instrument), Optional.ofNullable(value.ignore))));
  public static final IngredientType<InstrumentIngredient> TYPE = new IngredientType<>(CODEC);

  private final Item item;
  @Nullable
  private final ResourceKey<Instrument> instrument;
  @Nullable
  private final TagKey<Instrument> ignore;

  protected InstrumentIngredient(Item item, @Nullable ResourceKey<Instrument> instrument, @Nullable TagKey<Instrument> ignore) {
    this.item = Objects.requireNonNull(item);
    if (instrument == null && ignore == null) {
      throw new IllegalArgumentException("Must specify an instrument or ignore tag");
    }
    this.instrument = instrument;
    this.ignore = instrument == null ? ignore : null;
  }

  public static Ingredient of(ItemLike item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item.asItem(), Objects.requireNonNull(instrument), null).toVanilla();
  }

  public static Ingredient of(ItemLike item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item.asItem(), null, Objects.requireNonNull(ignore)).toVanilla();
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !stack.is(item)) {
      return false;
    }
    var selected = stack.get(DataComponents.INSTRUMENT);
    if (selected != null) {
      if (instrument != null) {
        return selected.is(instrument);
      }
      return selected.unwrapKey().filter(key -> BuiltInRegistries.INSTRUMENT.containsKey(key.location())).isPresent()
        && !selected.is(ignore);
    }
    CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
    if (custom != null) {
      CompoundTag tag = custom.copyTag();
      ResourceLocation key = ResourceLocation.tryParse(tag.getString("instrument"));
      if (key != null) {
        if (instrument != null) {
          return instrument.location().equals(key);
        }
        return BuiltInRegistries.INSTRUMENT.getHolder(ResourceKey.create(Registries.INSTRUMENT, key))
          .filter(holder -> !holder.is(ignore)).isPresent();
      }
    }
    // Preserve the original ignore-mode acceptance of unspecified instruments.
    return instrument == null;
  }

  @Override
  public Stream<ItemStack> getItems() {
    ItemStack stack = new ItemStack(item);
    if (instrument != null) {
      BuiltInRegistries.INSTRUMENT.getHolder(instrument).ifPresent(holder -> stack.set(DataComponents.INSTRUMENT, holder));
    }
    return Stream.of(stack);
  }

  @Override
  public IngredientType<InstrumentIngredient> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object object) {
    return object == this || object instanceof InstrumentIngredient other && item == other.item
      && Objects.equals(instrument, other.instrument) && Objects.equals(ignore, other.ignore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item, instrument, ignore);
  }
}
