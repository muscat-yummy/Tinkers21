package slimeknights.mantle.data.loadable.common;

import net.minecraft.nbt.CompoundTag;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.mantle.data.loadable.LoadableOps;
import slimeknights.mantle.util.typed.TypedMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/** Loadable for a fluid stack */
@SuppressWarnings("unused")  // API
public class FluidStackLoadable {
  private FluidStackLoadable() {}

  /* reused lambdas */
  /** Getter for an item from a stack */
  private static final Function<FluidStack,Fluid> FLUID_GETTER = FluidStack::getFluid;
  /** Checks if a stack can be serialized to a primitive, ignoring count */
  private static final Predicate<FluidStack> COMPACT_NBT = FluidStack::isComponentsPatchEmpty;
  /** Maps a fluid stack that may be empty to a strictly not empty one */
  private static final BiFunction<FluidStack,ErrorFactory,FluidStack> NOT_EMPTY = (stack, error) -> {
    if (stack.isEmpty()) {
      throw error.create("FluidStack cannot be empty");
    }
    return stack;
  };

  /* fields */
  /** Field for an optional fluid */
  private static final LoadableField<Fluid,FluidStack> FLUID = Loadables.FLUID.defaultField("fluid", Fluids.EMPTY, false, FLUID_GETTER);
  /** Field for fluid stack count that allows empty */
  private static final LoadableField<Integer,FluidStack> AMOUNT = IntLoadable.FROM_ZERO.requiredField("amount", FluidStack::getAmount);



  /* Optional */
  /** Single item which may be empty with an amount of 1000 */
  public static final Loadable<FluidStack> OPTIONAL_BUCKET = fixedSize(FluidType.BUCKET_VOLUME);
  /** Loadable for a stack that may be empty with variable count */
  public static final RecordLoadable<FluidStack> OPTIONAL_STACK = RecordLoadable.create(FLUID, AMOUNT, (fluid, count) -> makeStack(fluid, count, null));
  /** Loadable for a stack that may be empty with NBT and an amount of 1000 */
  public static final RecordLoadable<FluidStack> OPTIONAL_BUCKET_NBT = fixedSizeNBT(FluidType.BUCKET_VOLUME);
  /** Loadable for a stack that may be empty with variable count and NBT */
  public static final RecordLoadable<FluidStack> OPTIONAL_STACK_NBT = metadataLoadable(0);


  /* Required */
  /** Single item which may not be empty with an amount of 1000 */
  public static final Loadable<FluidStack> REQUIRED_BUCKET = notEmpty(OPTIONAL_BUCKET);
  /** Loadable for a stack that may not be empty with variable count */
  public static final RecordLoadable<FluidStack> REQUIRED_STACK = notEmpty(OPTIONAL_STACK);
  /** Loadable for a stack that may not be empty with NBT and an amount of 1000 */
  public static final RecordLoadable<FluidStack> REQUIRED_BUCKET_NBT = notEmpty(OPTIONAL_BUCKET_NBT);
  /** Loadable for a stack that may not be empty with variable count and NBT */
  public static final RecordLoadable<FluidStack> REQUIRED_STACK_NBT = notEmpty(OPTIONAL_STACK_NBT);


  /* Helpers */

  /** Makes an item stack from the given parameters */
  private static FluidStack makeStack(Fluid fluid, int amount, @Nullable CompoundTag nbt) {
    if (fluid == Fluids.EMPTY || amount <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack stack = new FluidStack(fluid, amount);
    if (nbt != null && !nbt.isEmpty()) {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt.copy()));
    }
    return stack;
  }

  /** Creates a loadable for a stack with a single item */
  public static Loadable<FluidStack> fixedSize(int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Count must be positive, received " + amount);
    }
    return Loadables.FLUID.flatXmap(fluid -> makeStack(fluid, amount, null), FLUID_GETTER);
  }

  /** Creates a loadable for a stack with a single item */
  public static RecordLoadable<FluidStack> fixedSizeNBT(int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Amount must be positive, received " + amount);
    }
    return metadataLoadable(amount).compact(fixedSize(amount), COMPACT_NBT);
  }

  /** Metadata variant: legacy fluid NBT is custom data, modern patches retain all components. */
  private static RecordLoadable<FluidStack> metadataLoadable(int fixedAmount) {
    return new RecordLoadable<>() {
      @Override
      public FluidStack deserialize(JsonObject json, TypedMap context) {
        CompoundTag nbt = NBTLoadable.ALLOW_STRING.getOrDefault(json, "nbt", null, context);
        if (nbt != null && json.has("components")) {
          throw new com.google.gson.JsonSyntaxException("Specify either legacy nbt or components, not both");
        }
        FluidStack stack = makeStack(FLUID.get(json, context),
          fixedAmount == 0 ? AMOUNT.get(json, context) : fixedAmount, nbt);
        if (!stack.isEmpty() && json.has("components")) {
          stack.applyComponents(DataComponentPatch.CODEC.parse(LoadableOps.current(), json.get("components")).getOrThrow());
        }
        return stack;
      }

      @Override
      public void serialize(FluidStack stack, JsonObject json) {
        FLUID.serialize(stack, json);
        if (fixedAmount == 0) {
          AMOUNT.serialize(stack, json);
        }
        if (!stack.isComponentsPatchEmpty()) {
          json.add("components", DataComponentPatch.CODEC.encodeStart(LoadableOps.current(), stack.getComponentsPatch()).getOrThrow());
        }
      }

      @Override
      public FluidStack decode(FriendlyByteBuf buffer, TypedMap context) {
        FluidStack stack = FluidStack.OPTIONAL_STREAM_CODEC.decode(ItemStackLoadable.registryBuffer(buffer));
        if (fixedAmount > 0 && !stack.isEmpty()) {
          stack.setAmount(fixedAmount);
        }
        return stack;
      }

      @Override
      public void encode(FriendlyByteBuf buffer, FluidStack stack) {
        if (fixedAmount > 0 && !stack.isEmpty()) {
          stack = stack.copy();
          stack.setAmount(fixedAmount);
        }
        FluidStack.OPTIONAL_STREAM_CODEC.encode(ItemStackLoadable.registryBuffer(buffer), stack);
      }
    };
  }

  /** Creates a non-empty variant of the loadable */
  public static Loadable<FluidStack> notEmpty(Loadable<FluidStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }

  /** Creates a non-empty variant of the loadable */
  public static RecordLoadable<FluidStack> notEmpty(RecordLoadable<FluidStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }
}
