package slimeknights.tconstruct.library.json;

import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Keeps TiC's established operation JSON while accepting native 1.21 spellings. */
public enum AttributeOperationLoadable implements StringLoadable<Operation> {
  INSTANCE;

  @Override
  public Operation parseString(String value, String key, TypedMap context) {
    return switch (value) {
      case "addition", "add_value" -> Operation.ADD_VALUE;
      case "multiply_base", "add_multiplied_base" -> Operation.ADD_MULTIPLIED_BASE;
      case "multiply_total", "add_multiplied_total" -> Operation.ADD_MULTIPLIED_TOTAL;
      default -> throw new JsonSyntaxException("Invalid attribute operation " + value + " for " + key);
    };
  }

  @Override
  public String getString(Operation value) {
    return switch (value) {
      case ADD_VALUE -> "addition";
      case ADD_MULTIPLIED_BASE -> "multiply_base";
      case ADD_MULTIPLIED_TOTAL -> "multiply_total";
    };
  }

  @Override
  public Operation decode(FriendlyByteBuf buffer, TypedMap context) {
    return buffer.readEnum(Operation.class);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, Operation value) {
    buffer.writeEnum(value);
  }
}
