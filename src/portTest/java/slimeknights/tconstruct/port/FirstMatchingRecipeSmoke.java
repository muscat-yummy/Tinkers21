package slimeknights.tconstruct.port;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import slimeknights.mantle.recipe.helper.FirstMatchingCodec;

public final class FirstMatchingRecipeSmoke {
  public static void main(String[] args) {
    var codec = FirstMatchingCodec.create(Codec.INT.optionalFieldOf("recipe").codec()).codec();
    var first = JsonParser.parseString("{\"recipes\":[{}, {\"recipe\":3}, {\"recipe\":7}]}");
    if (codec.parse(JsonOps.INSTANCE, first).getOrThrow() != 3) {
      throw new AssertionError("The first enabled alternative must win");
    }
    var lazy = JsonParser.parseString("{\"recipes\":[{\"recipe\":3}, {\"recipe\":\"invalid\"}]}");
    if (codec.parse(JsonOps.INSTANCE, lazy).getOrThrow() != 3) {
      throw new AssertionError("A later alternative must not be decoded");
    }
    for (String invalid : new String[] {
        "{\"recipes\":[]}", "{\"recipes\":[{}]}", "{}",
        "{\"recipes\":[{\"recipe\":\"invalid\"}, {\"recipe\":7}]}"
    }) {
      if (codec.parse(JsonOps.INSTANCE, JsonParser.parseString(invalid)).error().isEmpty()) {
        throw new AssertionError("Missing/malformed alternatives must fail: " + invalid);
      }
    }
    var encoded = codec.encodeStart(JsonOps.INSTANCE, 9).getOrThrow();
    if (codec.parse(JsonOps.INSTANCE, encoded).getOrThrow() != 9) {
      throw new AssertionError("Selected recipe must retain its native encoding");
    }
    System.out.println("First conditional match, lazy alternatives, fallback and malformed-entry validation: PASS");
  }
}
