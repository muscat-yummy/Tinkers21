package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.registration.object.FluidObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching a full fluid container that drains to its crafting remainder. */
@SuppressWarnings("unused")
public class FluidContainerIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = Mantle.getResource("fluid_container");
  private final FluidIngredient fluidIngredient;
  @Nullable
  private final Ingredient display;

  /** Both legacy flat fluid fields and nested fluid objects/arrays are accepted. */
  public static final MapCodec<FluidContainerIngredient> CODEC = new MapCodec<>() {
    @Override
    public <T> DataResult<FluidContainerIngredient> decode(DynamicOps<T> ops, MapLike<T> input) {
      try {
        JsonObject json = new JsonObject();
        input.entries().forEach(entry -> {
          String key = ops.getStringValue(entry.getFirst()).getOrThrow();
          json.add(key, ops.convertTo(JsonOps.INSTANCE, entry.getSecond()));
        });
        JsonElement fluid = json.get("fluid");
        FluidIngredient ingredient = fluid != null && !fluid.isJsonPrimitive()
          ? FluidIngredient.LOADABLE.convert(fluid, "fluid")
          : FluidIngredient.LOADABLE.convert(json, "fluid");
        T displayValue = input.get("display");
        if (displayValue == null) {
          return DataResult.success(new FluidContainerIngredient(ingredient, null));
        }
        return Ingredient.CODEC.parse(ops, displayValue)
          .map(value -> new FluidContainerIngredient(ingredient, value));
      } catch (RuntimeException exception) {
        return DataResult.error(() -> "Invalid fluid container ingredient: " + exception.getMessage());
      }
    }

    @Override
    public <T> RecordBuilder<T> encode(FluidContainerIngredient ingredient, DynamicOps<T> ops, RecordBuilder<T> prefix) {
      JsonElement fluid = ingredient.fluidIngredient.serialize();
      if (fluid.isJsonObject()) {
        fluid.getAsJsonObject().entrySet().forEach(entry ->
          prefix.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue())));
      } else {
        prefix.add("fluid", JsonOps.INSTANCE.convertTo(ops, fluid));
      }
      if (ingredient.display != null) {
        prefix.add("display", Ingredient.CODEC.encodeStart(ops, ingredient.display));
      }
      return prefix;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.of("fluid", "tag", "name", "amount", "display").map(ops::createString);
    }
  };

  public static final StreamCodec<RegistryFriendlyByteBuf,FluidContainerIngredient> STREAM_CODEC = StreamCodec.of(
    (buffer, ingredient) -> {
      FluidIngredient.LOADABLE.encode(buffer, ingredient.fluidIngredient);
      buffer.writeBoolean(ingredient.display != null);
      if (ingredient.display != null) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.display);
      }
    },
    buffer -> {
      FluidIngredient fluid = FluidIngredient.LOADABLE.decode(buffer);
      Ingredient display = buffer.readBoolean() ? Ingredient.CONTENTS_STREAM_CODEC.decode(buffer) : null;
      return new FluidContainerIngredient(fluid, display);
    });
  public static final IngredientType<FluidContainerIngredient> TYPE = new IngredientType<>(CODEC, STREAM_CODEC);
  /** @deprecated NeoForge ingredients are registered by type rather than the old serializer API. */
  @Deprecated
  public static final IngredientType<FluidContainerIngredient> SERIALIZER = TYPE;

  protected FluidContainerIngredient(FluidIngredient fluidIngredient, @Nullable Ingredient display) {
    this.fluidIngredient = Objects.requireNonNull(fluidIngredient);
    this.display = display;
  }

  public static Ingredient fromIngredient(FluidIngredient ingredient, Ingredient display) {
    return new FluidContainerIngredient(ingredient, display).toVanilla();
  }

  public static Ingredient fromIngredient(FluidIngredient ingredient) {
    return new FluidContainerIngredient(ingredient, null).toVanilla();
  }

  public static Ingredient fromFluid(FluidObject<?> fluid) {
    return fromIngredient(fluid.ingredient(FluidType.BUCKET_VOLUME), Ingredient.of(fluid));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
    if (handler == null || handler.getTanks() != 1) {
      return false;
    }
    FluidStack contained = handler.getFluidInTank(0);
    if (contained.isEmpty() || fluidIngredient.getAmount(contained.getFluid()) != contained.getAmount()
        || !fluidIngredient.test(contained.getFluid())) {
      return false;
    }
    // Never drain or modify the tested stack. Query the capability on a single-item copy.
    IFluidHandlerItem copyHandler = stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
    if (copyHandler == null) {
      return false;
    }
    Fluid fluid = copyHandler.getFluidInTank(0).getFluid();
    int amount = fluidIngredient.getAmount(fluid);
    FluidStack drained = copyHandler.drain(amount, FluidAction.EXECUTE);
    return drained.getFluid() == fluid && drained.getAmount() == amount
      && ItemStack.matches(stack.getCraftingRemainingItem(), copyHandler.getContainer());
  }

  @Override
  public Stream<ItemStack> getItems() {
    return display == null ? Stream.empty() : Arrays.stream(display.getItems());
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public IngredientType<FluidContainerIngredient> getType() {
    return TYPE;
  }

  /** Compatibility helper for data-generation callers. */
  public JsonElement toJson() {
    JsonObject json = CODEC.codec().encodeStart(JsonOps.INSTANCE, this).getOrThrow().getAsJsonObject();
    json.addProperty("type", ID.toString());
    return json;
  }

  @Override
  public boolean equals(Object object) {
    return object == this || object instanceof FluidContainerIngredient other
      && fluidIngredient.serialize().equals(other.fluidIngredient.serialize())
      && Objects.equals(display, other.display);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fluidIngredient.serialize(), display);
  }
}
