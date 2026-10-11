package slimeknights.tconstruct.port;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.RecipeDecodeContext;
import slimeknights.mantle.recipe.helper.RecipeHolderCodec;
import slimeknights.mantle.recipe.helper.SimpleRecipeSerializer;

public final class RecipeIdentityScopeSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static SmeltingRecipe recipe(String group) {
    return new SmeltingRecipe(group, CookingBookCategory.MISC,
      Ingredient.of(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT), 0.7F, 200);
  }

  public static void main(String[] args) throws Exception {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    ResourceLocation first = ResourceLocation.parse("tconstruct:test/first");
    ResourceLocation second = ResourceLocation.parse("tconstruct:test/second");
    check(RecipeDecodeContext.currentId() == null, "Unbound context must not generate an ID");
    RecordLoadable<SmeltingRecipe> loader = RecordLoadable.create(
      ContextKey.ID.requiredField(), id -> recipe(id.toString()));
    var codec = LoadableRecipeSerializer.of(loader).codec().codec();
    check(codec.parse(JsonOps.INSTANCE, new JsonObject()).error().isPresent(), "ID-required payload must reject missing holder context");
    RecipeDecodeContext.withId(first, () -> {
      check(codec.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow().getGroup().equals(first.toString()), "Loader lost actual holder ID");
      var simple = new SimpleRecipeSerializer<SmeltingRecipe>((ResourceLocation id) -> recipe(id.toString()));
      check(simple.codec().codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow().getGroup().equals(first.toString()), "Simple factory lost holder ID");
      try {
        RecipeDecodeContext.withId(second, () -> { throw new IllegalArgumentException("nested decode"); });
        throw new AssertionError("Nested failure was swallowed");
      } catch (IllegalArgumentException expected) {
        check(first.equals(RecipeDecodeContext.requireId()), "Nested failure leaked its ID");
      }
      AtomicReference<Throwable> failure = new AtomicReference<>();
      Thread thread = new Thread(() -> {
        try {
          check(RecipeDecodeContext.currentId() == null, "Decode ID leaked into another thread");
          RecipeDecodeContext.withId(second, () -> {
            check(second.equals(RecipeDecodeContext.requireId()), "Other thread lost its own ID");
            return null;
          });
          check(RecipeDecodeContext.currentId() == null, "Other thread retained a completed decode ID");
        } catch (Throwable problem) { failure.set(problem); }
      });
      thread.start();
      try { thread.join(5000); } catch (InterruptedException problem) { throw new AssertionError(problem); }
      check(!thread.isAlive(), "Independent decode thread did not finish");
      if (failure.get() != null) throw new AssertionError(failure.get());
      return null;
    });
    check(RecipeDecodeContext.currentId() == null, "Successful decode left an ID behind");

    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var nativeBytes = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    var scopedBytes = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    var badBytes = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      var holder = new RecipeHolder<>(first, recipe("native-wire"));
      RecipeHolder.STREAM_CODEC.encode(nativeBytes, holder);
      StreamCodec<RegistryFriendlyByteBuf,Recipe<?>> checkedPayload = StreamCodec.of(
        Recipe.STREAM_CODEC::encode, buffer -> {
          check(first.equals(RecipeDecodeContext.requireId()), "Wire payload did not receive its preceding holder ID");
          return Recipe.STREAM_CODEC.decode(buffer);
        });
      var scopedCodec = RecipeHolderCodec.create(checkedPayload);
      scopedCodec.encode(scopedBytes, holder);
      check(ByteBufUtil.equals(nativeBytes, scopedBytes), "Holder wire format differs from native ID-then-payload encoding");
      var decoded = scopedCodec.decode(scopedBytes);
      check(decoded.id().equals(first) && decoded.value().getGroup().equals("native-wire"), "Holder ID or payload changed");
      check(scopedBytes.readableBytes() == 0, "Holder decode left unread bytes");
      check(RecipeDecodeContext.currentId() == null, "Wire decode leaked its ID");
      ResourceLocation.STREAM_CODEC.encode(badBytes, second);
      var failing = RecipeHolderCodec.create(StreamCodec.of((buffer, value) -> {}, buffer -> {
        check(second.equals(RecipeDecodeContext.requireId()), "Malformed payload lost actual ID");
        throw new IllegalArgumentException("bad payload");
      }));
      try {
        failing.decode(badBytes);
        throw new AssertionError("Malformed payload was accepted");
      } catch (IllegalArgumentException expected) {
        check(RecipeDecodeContext.currentId() == null, "Malformed wire payload leaked its ID");
      }
    } finally {
      nativeBytes.release();
      scopedBytes.release();
      badBytes.release();
    }
    System.out.println("Actual recipe holder IDs, required payload context, nested/thread isolation, native wire equality and failure cleanup: PASS");
  }
}
