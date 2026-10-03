package slimeknights.tconstruct.port;

import com.google.gson.JsonSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.json.AttributeOperationLoadable;

public final class AttributeOperationSmoke {
  public static void main(String[] args) {
    var loadable = AttributeOperationLoadable.INSTANCE;
    String[] legacy = {"addition", "multiply_base", "multiply_total"};
    String[] nativeNames = {"add_value", "add_multiplied_base", "add_multiplied_total"};
    Operation[] operations = {Operation.ADD_VALUE, Operation.ADD_MULTIPLIED_BASE, Operation.ADD_MULTIPLIED_TOTAL};
    for (int i = 0; i < operations.length; i++) {
      Operation operation = operations[i];
      if (loadable.parseString(legacy[i], "operation") != operation
          || loadable.parseString(nativeNames[i], "operation") != operation
          || !loadable.serialize(operation).getAsString().equals(legacy[i])) {
        throw new AssertionError("Legacy/native operation JSON semantics changed");
      }
      FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
      try {
        loadable.encode(buffer, operation);
        if (buffer.readVarInt() != i) throw new AssertionError("Legacy wire index changed");
        buffer.readerIndex(0);
        if (loadable.decode(buffer, TypedMap.EMPTY) != operation || buffer.readableBytes() != 0) {
          throw new AssertionError("Native operation wire round trip failed");
        }
      } finally {
        buffer.release();
      }
    }
    for (String invalid : new String[] {"", "ADDITION", "unknown", "3"}) {
      try {
        loadable.parseString(invalid, "operation");
        throw new AssertionError("Invalid operation accepted");
      } catch (JsonSyntaxException expected) {
        // Established strict string validation.
      }
    }
    System.out.println("Native attribute operations retain legacy JSON, native aliases and exact wire indices: PASS");
  }
}
