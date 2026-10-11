package slimeknights.tconstruct.port;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.library.fluid.PotionData;

import java.util.List;
import java.util.Optional;

public final class PotionContentsSmoke {
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var effect = new MobEffectInstance(MobEffects.POISON, 321, 2, true, false, true);
    var contents = new PotionContents(Optional.of(Potions.HEALING), Optional.of(0x123456), List.of(effect));
    ItemStack bottle = new ItemStack(Items.POTION);
    bottle.set(DataComponents.POTION_CONTENTS, contents);
    bottle.set(DataComponents.CUSTOM_NAME, Component.literal("Special potion"));
    FluidStack fluid = PotionData.fluid(bottle, Fluids.WATER, 333);
    check(PotionData.get(fluid).equals(contents), "Bottle-to-fluid potion contents lost");
    check(fluid.getAmount() == 333 && fluid.has(DataComponents.CUSTOM_NAME), "Amount or other components lost");
    ItemStack refilled = PotionData.fill(new ItemStack(Items.SPLASH_POTION), fluid);
    check(PotionData.get(refilled).equals(contents) && refilled.has(DataComponents.CUSTOM_NAME), "Fluid-to-item contents lost");
    bottle.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    check(PotionData.get(fluid).equals(contents), "Transfer aliased source component replacement");

    var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
    FluidStack decoded = FluidStack.CODEC.parse(ops, FluidStack.CODEC.encodeStart(ops, fluid).getOrThrow()).getOrThrow();
    check(PotionData.get(decoded).equals(contents), "Potion fluid JSON round trip");
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      FluidStack.STREAM_CODEC.encode(buffer, fluid);
      decoded = FluidStack.STREAM_CODEC.decode(buffer);
      check(PotionData.get(decoded).equals(contents) && buffer.readableBytes() == 0, "Potion fluid wire round trip");
    } finally {
      buffer.release();
    }
    decoded = FluidStack.parseOptional(registries, (CompoundTag) fluid.save(registries));
    check(PotionData.get(decoded).equals(contents), "Potion fluid persistence round trip");

    CompoundTag old = new CompoundTag();
    old.putString("Potion", "minecraft:healing");
    old.putInt("CustomPotionColor", 0x123456);
    old.putString("fixture", "keep");
    CompoundTag oldEffect = new CompoundTag();
    oldEffect.putString("Id", "minecraft:poison");
    oldEffect.putInt("Duration", 321);
    oldEffect.putByte("Amplifier", (byte) 2);
    oldEffect.putBoolean("Ambient", true);
    oldEffect.putBoolean("ShowParticles", false);
    oldEffect.putBoolean("ShowIcon", true);
    ListTag oldEffects = new ListTag();
    oldEffects.add(oldEffect);
    old.put("CustomPotionEffects", oldEffects);
    ItemStack legacyBottle = new ItemStack(Items.POTION);
    legacyBottle.set(DataComponents.CUSTOM_DATA, CustomData.of(old));
    check(PotionData.get(legacyBottle).equals(contents), "Empty item default hid legacy potion metadata");
    FluidStack migrated = PotionData.fluid(legacyBottle, Fluids.WATER, 1000);
    check(PotionData.get(migrated).equals(contents), "Legacy contents migration");
    var metadata = migrated.get(DataComponents.CUSTOM_DATA).copyTag();
    check(metadata.getString("fixture").equals("keep") && !metadata.contains("Potion"), "Migration lost unrelated metadata or kept duplicate potion fields");
    var predicate = PotionData.predicateTag(migrated);
    check(predicate.getString("Potion").equals("minecraft:healing") && predicate.getInt("CustomPotionColor") == 0x123456
      && predicate.getList("CustomPotionEffects", 10).getCompound(0).getInt("Duration") == 321, "Legacy predicate projection changed");
    predicate.putString("fixture", "changed");
    check(migrated.get(DataComponents.CUSTOM_DATA).copyTag().getString("fixture").equals("keep"), "Predicate view mutated contents");
    migrated.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.AWKWARD));
    check(PotionData.get(migrated).potion().orElseThrow().equals(Potions.AWKWARD), "Native component did not override old data");
    check(PotionData.potion("missing:potion").isEmpty() && PotionData.potion("INVALID").isEmpty(), "Unknown potion silently fell back");
    CompoundTag unknown = new CompoundTag();
    unknown.putString("Potion", "missing:potion");
    check(PotionData.readLegacy(unknown).equals(PotionContents.EMPTY), "Unknown legacy potion fallback");
    check(new PotionContents(Potions.WATER).is(Potions.WATER), "Plain water must match vanilla water contents");
    check(!new PotionContents(Optional.of(Potions.WATER), Optional.empty(), List.of(effect)).is(Potions.WATER),
      "Custom effects must distinguish special water");
    var wrapper = new slimeknights.tconstruct.fluids.item.PotionBucketItem.PotionBucketWrapper(refilled, Fluids.WATER);
    var simulated = wrapper.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
    check(PotionData.get(simulated).equals(contents) && wrapper.getContainer() == refilled, "Potion bucket simulated drain mutated container or lost contents");
    var drained = wrapper.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
    check(PotionData.get(drained).equals(contents) && wrapper.getContainer().is(Items.BUCKET), "Potion bucket drain lost contents or failed replacement");
    check(wrapper.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Potion bucket drained twice");

    slimeknights.tconstruct.plugin.jei.util.PotionSubtypeInterpreter<ItemStack> interpreter = PotionData::get;
    String identity = interpreter.apply(refilled, mezz.jei.api.ingredients.subtypes.UidContext.Ingredient);
    ItemStack alternate = refilled.copy();
    alternate.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(Potions.HEALING), Optional.of(0x654321), List.of(effect)));
    check(!identity.equals(interpreter.apply(alternate, mezz.jei.api.ingredients.subtypes.UidContext.Ingredient)), "JEI merged different custom colors");
    alternate.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(Potions.HEALING), Optional.of(0x123456),
      List.of(new MobEffectInstance(MobEffects.POISON, 322, 2, true, false, true))));
    check(!identity.equals(interpreter.apply(alternate, mezz.jei.api.ingredients.subtypes.UidContext.Ingredient)), "JEI merged different custom effects");
    slimeknights.tconstruct.plugin.jei.util.PotionSubtypeInterpreter<FluidStack> fluidInterpreter = PotionData::get;
    check(identity.equals(fluidInterpreter.apply(fluid, mezz.jei.api.ingredients.subtypes.UidContext.Ingredient)), "Item and fluid subtype identity differ");
    var patch = net.minecraft.core.component.DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, contents).build();
    var output = slimeknights.mantle.recipe.helper.FluidOutput.fromTagWithComponents(net.minecraft.tags.FluidTags.WATER, 333, patch);
    slimeknights.mantle.data.loadable.LoadableOps.with(ops, () -> {
      var json = output.serialize();
      check(json.has("components") && !json.has("nbt"), "Tag output emitted old potion NBT");
      var restored = slimeknights.mantle.recipe.helper.FluidOutput.Loadable.REQUIRED.deserialize(json, slimeknights.mantle.util.typed.TypedMap.EMPTY);
      check(restored.getTag().equals(net.minecraft.tags.FluidTags.WATER) && restored.getAmount() == 333, "Tag preference output identity changed");
      check(restored.serialize().equals(json), "Tag component output JSON round trip");
      return null;
    });
    System.out.println("Native potion item/fluid components, legacy effects/color, predicate snapshots, bucket simulation, JEI identities and JSON/wire/save: PASS");
  }
}
