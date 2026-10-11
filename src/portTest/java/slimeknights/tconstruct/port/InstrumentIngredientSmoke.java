package slimeknights.tconstruct.port;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import slimeknights.tconstruct.library.recipe.ingredient.InstrumentIngredient;

import java.util.List;
import java.util.Map;

public final class InstrumentIngredientSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
    var ponder = ResourceKey.create(Registries.INSTRUMENT, ResourceLocation.withDefaultNamespace("ponder_goat_horn"));
    var sing = ResourceKey.create(Registries.INSTRUMENT, ResourceLocation.withDefaultNamespace("sing_goat_horn"));
    var holder = BuiltInRegistries.INSTRUMENT.getHolderOrThrow(ponder);
    TagKey<Instrument> excluded = TagKey.create(Registries.INSTRUMENT, ResourceLocation.fromNamespaceAndPath("port_test", "excluded"));
    BuiltInRegistries.INSTRUMENT.bindTags(Map.of(excluded, List.of(holder)));

    var json = JsonParser.parseString("{\"item\":\"minecraft:goat_horn\",\"instrument\":\"minecraft:ponder_goat_horn\"}");
    InstrumentIngredient specific = InstrumentIngredient.CODEC.codec().parse(ops, json).getOrThrow();
    ItemStack stack = new ItemStack(Items.GOAT_HORN);
    stack.remove(DataComponents.INSTRUMENT);
    check(!specific.test(stack), "Specific ingredient accepted missing instrument");
    stack.set(DataComponents.INSTRUMENT, holder);
    check(specific.test(stack), "Native instrument did not match");
    stack.set(DataComponents.INSTRUMENT, BuiltInRegistries.INSTRUMENT.getHolderOrThrow(sing));
    check(!specific.test(stack), "Different instrument matched");
    check(!specific.test(new ItemStack(Items.STICK)), "Wrong item matched");
    check(specific.getItems().findFirst().orElseThrow().get(DataComponents.INSTRUMENT).is(ponder), "Display stack lost its component");

    stack.remove(DataComponents.INSTRUMENT);
    CompoundTag legacy = new CompoundTag();
    legacy.putString("instrument", ponder.location().toString());
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));
    check(specific.test(stack), "Legacy instrument stopped matching");
    // Native component must take precedence over a stale custom-data instrument.
    stack.set(DataComponents.INSTRUMENT, BuiltInRegistries.INSTRUMENT.getHolderOrThrow(sing));
    check(!specific.test(stack), "Legacy data overrode the native component");
    InstrumentIngredient ignore = InstrumentIngredient.CODEC.codec().parse(ops,
      JsonParser.parseString("{\"item\":\"minecraft:goat_horn\",\"ignore\":\"port_test:excluded\"}")).getOrThrow();
    check(ignore.test(stack), "Unexcluded instrument rejected");
    stack.set(DataComponents.INSTRUMENT, holder);
    check(!ignore.test(stack), "Excluded native instrument accepted");
    stack.remove(DataComponents.INSTRUMENT);
    check(!ignore.test(stack), "Excluded legacy instrument accepted");
    legacy.putString("instrument", "minecraft:missing_instrument");
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacy));
    check(!ignore.test(stack), "Unknown legacy instrument accepted");
    stack.remove(DataComponents.CUSTOM_DATA);
    check(ignore.test(stack), "Ignore mode rejected an unspecified instrument");

    check(InstrumentIngredient.CODEC.codec().parse(ops, JsonParser.parseString("{\"item\":\"minecraft:goat_horn\"}")).error().isPresent(),
      "JSON without instrument or ignore accepted");
    var both = InstrumentIngredient.CODEC.codec().parse(ops, JsonParser.parseString(
      "{\"item\":\"minecraft:goat_horn\",\"instrument\":\"minecraft:ponder_goat_horn\",\"ignore\":\"port_test:excluded\"}")).getOrThrow();
    check(both.equals(specific), "Legacy specific-instrument priority changed");
    var encoded = InstrumentIngredient.CODEC.codec().encodeStart(ops, specific).getOrThrow();
    check(InstrumentIngredient.CODEC.codec().parse(ops, encoded).getOrThrow().equals(specific), "JSON round trip changed ingredient");
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      InstrumentIngredient.TYPE.streamCodec().encode(buffer, specific);
      check(InstrumentIngredient.TYPE.streamCodec().decode(buffer).equals(specific), "Native wire round trip changed ingredient");
    } finally {
      buffer.release();
    }
    check(InstrumentIngredient.of(Items.GOAT_HORN, ponder).test(specific.getItems().findFirst().orElseThrow()),
      "Vanilla ingredient wrapper lost custom matching");
    System.out.println("Instrument component/legacy matching, ignore tags, JSON validation and native wire: PASS");
  }
}
