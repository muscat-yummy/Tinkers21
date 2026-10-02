package slimeknights.tconstruct.port;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import slimeknights.mantle.data.loadable.LoadableOps;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.recipe.ingredient.NoContainerIngredient;

import java.util.List;
import java.util.Map;

public final class LoadableIngredientScopeSmoke {
  private static void checkScope() {
    if (!(LoadableOps.current() instanceof RegistryOps<?>)) {
      throw new AssertionError("Ingredient bridge lost the actual registry operations");
    }
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
    var serializer = new LoadableIngredientSerializer<>(new RecordLoadable<NoContainerIngredient>() {
      @Override
      public NoContainerIngredient deserialize(JsonObject json, TypedMap context) {
        checkScope();
        if (json.has("fail")) throw new IllegalArgumentException("intentional fixture failure");
        return NoContainerIngredient.CODEC.codec().parse(LoadableOps.current(), json).getOrThrow();
      }

      @Override
      public void serialize(NoContainerIngredient value, JsonObject json) {
        checkScope();
        NoContainerIngredient.CODEC.codec().encodeStart(LoadableOps.current(), value).getOrThrow()
          .getAsJsonObject().entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
      }

      @Override
      public NoContainerIngredient decode(FriendlyByteBuf buffer, TypedMap context) {
        return NoContainerIngredient.TYPE.streamCodec().decode((RegistryFriendlyByteBuf) buffer);
      }

      @Override
      public void encode(FriendlyByteBuf buffer, NoContainerIngredient value) {
        NoContainerIngredient.TYPE.streamCodec().encode((RegistryFriendlyByteBuf) buffer, value);
      }
    });
    var codec = serializer.mapCodec("match", "item", "tag", "fail").codec();
    var value = codec.parse(ops, JsonParser.parseString("{\"match\":{\"item\":\"minecraft:stone\"}}")).getOrThrow();
    var encoded = codec.encodeStart(ops, value).getOrThrow();
    if (!codec.parse(ops, encoded).getOrThrow().equals(value)) throw new AssertionError("Nested ingredient changed");
    if (codec.parse(ops, JsonParser.parseString("{\"fail\":true}")).error().isEmpty()) throw new AssertionError("Failure was hidden");
    if (LoadableOps.current() != JsonOps.INSTANCE) throw new AssertionError("Ingredient scope leaked after failure");
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      serializer.write(buffer, value);
      if (!serializer.parse(buffer).equals(value)) throw new AssertionError("Native nested wire changed");
    } finally {
      buffer.release();
    }
    System.out.println("Loadable ingredient registry scope, nested JSON/wire and failure restoration: PASS");
  }
}
