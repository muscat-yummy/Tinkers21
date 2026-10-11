package slimeknights.tconstruct.port;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipePattern;

import java.util.List;

public final class MaterialRecipePatternSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void rejected(Runnable action, String message) {
    try { action.run(); } catch (IllegalArgumentException expected) { return; }
    throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = registries.createSerializationContext(JsonOps.INSTANCE);
    var json = JsonParser.parseString("{\"pattern\":[\"A A\",\" B \"],\"key\":{\"A\":{\"item\":\"minecraft:iron_ingot\"},\"B\":{\"item\":\"minecraft:stick\"}}}");
    var pattern = ShapedRecipePattern.MAP_CODEC.codec().parse(ops, json).getOrThrow();
    var parts = MaterialRecipePattern.parts(pattern, "AAB");
    check(parts.get(0) == parts.get(1) && parts.get(0) == pattern.ingredients().get(0)
      && parts.get(0) == pattern.ingredients().get(2), "Native JSON lost repeated symbol identity");
    check(MaterialRecipePattern.symbols(pattern, parts).equals("AAB"), "Ordered part symbols changed");
    rejected(() -> MaterialRecipePattern.parts(pattern, "X"), "Undefined part symbol was accepted");
    rejected(() -> MaterialRecipePattern.symbols(pattern, List.of(Ingredient.of(Items.DIAMOND))),
      "Part outside the pattern was accepted");
    var input = CraftingInput.of(3, 2, List.of(new ItemStack(Items.IRON_INGOT, 3), ItemStack.EMPTY,
      new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY));
    check(pattern.matches(input), "Native shaped pattern no longer matches its inputs");
    int originalCount = input.getItem(0).getCount();
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      MaterialRecipePattern.write(buffer, pattern.ingredients(), parts);
      var decoded = MaterialRecipePattern.read(buffer, pattern.width(), pattern.height());
      check(buffer.readableBytes() == 0 && decoded.pattern().matches(input), "Native wire changed pattern matching");
      check(decoded.parts().get(0) == decoded.parts().get(1)
        && decoded.parts().get(0) == decoded.pattern().ingredients().get(0)
        && decoded.parts().get(0) == decoded.pattern().ingredients().get(2), "Wire lost part/grid ingredient aliasing");
      var encoded = ShapedRecipePattern.MAP_CODEC.codec().encodeStart(ops, decoded.pattern()).getOrThrow();
      var reparsed = ShapedRecipePattern.MAP_CODEC.codec().parse(ops, encoded).getOrThrow();
      check(reparsed.matches(input), "Wire-decoded recipe lost native JSON pattern data");
    } finally { buffer.release(); }
    check(input.getItem(0).getCount() == originalCount, "Pattern lookup mutated crafting inputs");

    var first = Ingredient.of(Items.IRON_INGOT);
    var second = Ingredient.of(Items.IRON_INGOT);
    var distinct = MaterialRecipePattern.create(2, 1, NonNullList.of(Ingredient.EMPTY, first, second));
    buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      MaterialRecipePattern.write(buffer, distinct.ingredients(), List.of(first, second));
      var decoded = MaterialRecipePattern.read(buffer, 2, 1);
      check(decoded.parts().get(0) != decoded.parts().get(1), "Equal item ingredients were incorrectly merged by identity");
    } finally { buffer.release(); }
    var padded = NonNullList.withSize(9, Ingredient.EMPTY);
    padded.set(4, Ingredient.of(Items.STICK));
    var trimmed = MaterialRecipePattern.create(3, 3, padded);
    check(trimmed.width() == 1 && trimmed.height() == 1, "Native border trimming was lost");
    rejected(() -> MaterialRecipePattern.create(0, 1, NonNullList.create()), "Invalid dimensions were accepted");
    rejected(() -> MaterialRecipePattern.indexed(List.of(first), -1), "Negative ingredient index was accepted");
    rejected(() -> MaterialRecipePattern.indexed(List.of(first), 1), "Out-of-range ingredient index was accepted");
    System.out.println("Native material pattern JSON/wire, repeated and distinct ingredient identity, ordered parts, trimming and unchanged inputs: PASS");
  }
}
