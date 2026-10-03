package slimeknights.tconstruct.port;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.TrueCondition;
import net.neoforged.neoforge.common.conditions.FalseCondition;
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
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    // Standalone test bootstrap registers the actual native condition codecs, as the mod registry event does in game.
    Registry.register(NeoForgeRegistries.CONDITION_SERIALIZERS, ResourceLocation.parse("neoforge:true"), TrueCondition.CODEC);
    Registry.register(NeoForgeRegistries.CONDITION_SERIALIZERS, ResourceLocation.parse("neoforge:false"), FalseCondition.CODEC);
    var ops = new ConditionalOps<>(RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.EMPTY), ICondition.IContext.EMPTY);
    var nativeCodec = FirstMatchingCodec.create(ConditionalOps.createConditionalCodec(Codec.INT.fieldOf("recipe").codec())).codec();
    var nativeFirst = JsonParser.parseString("{\"recipes\":[{\"neoforge:conditions\":[{\"type\":\"neoforge:false\"}],\"recipe\":\"disabled invalid payload\"},{\"neoforge:conditions\":[{\"type\":\"neoforge:true\"}],\"recipe\":3},{\"recipe\":\"later invalid payload\"}]}");
    if (nativeCodec.parse(ops, nativeFirst).getOrThrow() != 3) {
      throw new AssertionError("Native conditions must skip disabled invalid payloads and stop after the first match");
    }
    var allFalse = JsonParser.parseString("{\"neoforge:conditions\":[{\"type\":\"neoforge:false\"}],\"recipes\":[{\"recipe\":\"invalid\"}]}");
    if (ConditionalOps.createConditionalCodec(nativeCodec).parse(ops, allFalse).getOrThrow().isPresent()) {
      throw new AssertionError("A false outer condition must skip the entire recipe");
    }
    System.out.println("Native conditional first match, disabled/later payload laziness, outer skip and malformed-entry validation: PASS");
  }
}
