package slimeknights.tconstruct.port;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.fluids.util.ConstantFluidContainerWrapper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import slimeknights.tconstruct.library.fluid.StoredFluid;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.smeltery.item.ScaledFluidTank;

public final class FluidContentsSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    // Initialize the real loader's empty mod-file list before vanilla feature flags.
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    CompoundTag metadata = new CompoundTag();
    metadata.putString("fixture", "retained");
    FluidStack original = new FluidStack(Fluids.WATER, 1000);
    original.set(DataComponents.CUSTOM_NAME, Component.literal("Named fluid"));
    original.set(DataComponents.CUSTOM_DATA, CustomData.of(metadata.copy()));
    StoredFluid contents = StoredFluid.of(original);
    original.setAmount(7);
    original.set(DataComponents.CUSTOM_NAME, Component.literal("Changed original"));
    check(contents.amount() == 1000, "Stored amount aliased the original");
    FluidStack first = contents.toStack();
    first.setAmount(9);
    first.remove(DataComponents.CUSTOM_NAME);
    check(contents.toStack().getAmount() == 1000 && contents.toStack().has(DataComponents.CUSTOM_NAME), "Access leaked a mutable stored stack");

    var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
    var legacyBridge = slimeknights.mantle.data.loadable.common.LoadableJsonCodec.of(
      new slimeknights.mantle.data.loadable.common.CodecLoadable<>(StoredFluid.CODEC));
    var bridgedJson = legacyBridge.encodeStart(ops, contents).getOrThrow();
    check(FluidStack.isSameFluidSameComponents(legacyBridge.parse(ops, bridgedJson).getOrThrow().toStack(), contents.toStack()),
      "Loadable codec bridge lost actual registry context or fluid components");
    var nbtOps = RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries);
    var bridgedNbt = legacyBridge.encodeStart(nbtOps, contents).getOrThrow();
    check(legacyBridge.parse(nbtOps, bridgedNbt).getOrThrow().amount() == 1000, "NBT loadable codec bridge changed amount");
    check(legacyBridge.parse(ops, com.google.gson.JsonParser.parseString("{\"bad\":true}")).error().isPresent(), "Invalid loadable data accepted");
    check(slimeknights.mantle.data.loadable.LoadableOps.current() == JsonOps.INSTANCE, "Loadable codec bridge leaked registry scope");
    var json = StoredFluid.CODEC.encodeStart(ops, contents).getOrThrow();
    StoredFluid roundTrip = StoredFluid.CODEC.parse(ops, json).getOrThrow();
    check(FluidStack.matches(contents.toStack(), roundTrip.toStack()), "JSON component patch round trip");
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      StoredFluid.STREAM_CODEC.encode(buffer, contents);
      check(FluidStack.matches(contents.toStack(), StoredFluid.STREAM_CODEC.decode(buffer).toStack()), "Wire component patch round trip");
    } finally {
      buffer.release();
    }

    var tank = (ScaledFluidTank) ScaledFluidTank.create(1000, 4);
    tank.setFluid(contents.toStack().copyWithAmount(4000));
    check(tank.drain(5, FluidAction.SIMULATE).getAmount() == 4 && tank.getFluidAmount() == 4000, "Simulation or integral scale");
    check(tank.drain(5, FluidAction.EXECUTE).getAmount() == 4 && tank.getFluidPerItem().getAmount() == 999, "Per-item normalization");
    check(tank.fill(contents.toStack().copyWithAmount(5), FluidAction.SIMULATE) == 4 && tank.getFluidAmount() == 3996, "Scaled fill simulation");
    check(tank.fill(contents.toStack().copyWithAmount(5), FluidAction.EXECUTE) == 4 && tank.getFluidAmount() == 4000, "Scaled fill execution");
    check(FluidStack.isSameFluidSameComponents(tank.getFluidPerItem(), contents.toStack()), "Scaled tank lost components");
    CompoundTag saved = tank.writeToNBT(registries, new CompoundTag());
    var restored = (ScaledFluidTank) ScaledFluidTank.create(1000, 4);
    restored.readFromNBT(registries, saved);
    check(FluidStack.matches(tank.getFluid(), restored.getFluid()), "Native tank save/load double-scaled or lost metadata");

    ItemStack filled = new ItemStack(Items.WATER_BUCKET);
    ItemStack empty = new ItemStack(Items.BUCKET);
    var constant = new ConstantFluidContainerWrapper(contents.toStack(), filled, empty);
    check(constant.drain(1000, FluidAction.SIMULATE).getAmount() == 1000 && constant.getContainer() == filled, "Constant container simulation");
    check(FluidStack.matches(constant.drain(1000, FluidAction.EXECUTE), contents.toStack()) && constant.getContainer() == empty, "Constant container replacement or metadata");
    check(constant.drain(1000, FluidAction.EXECUTE).isEmpty(), "Constant container drained twice");

    CompoundTag legacy = new CompoundTag();
    legacy.putString("FluidName", "minecraft:water");
    legacy.putInt("Amount", 123);
    legacy.put("Tag", metadata.copy());
    FluidStack converted = StoredFluid.readLegacyTank(legacy);
    metadata.putString("fixture", "mutated outside");
    check(converted.getAmount() == 123 && converted.get(DataComponents.CUSTOM_DATA).copyTag().getString("fixture").equals("retained"), "Legacy amount or defensive metadata conversion");
    restored.readFromNBT(registries, legacy);
    check(restored.getFluidAmount() == 492, "Legacy tank per-item scale");
    check(StoredFluid.readLegacy("missing:fluid", 1, new CompoundTag()).isEmpty(), "Unknown fluid ID must remain empty");
    check(StoredFluid.readLegacy("INVALID", 1, new CompoundTag()).isEmpty(), "Invalid fluid ID must remain empty");
    boolean rejected = false;
    try { ScaledFluidTank.create(1000, 0); } catch (IllegalArgumentException expected) { rejected = true; }
    check(rejected, "Zero scale must be rejected");

    CompoundTag position = new CompoundTag();
    CompoundTag legacyPos = new CompoundTag();
    legacyPos.putInt("X", 1); legacyPos.putInt("Y", -2); legacyPos.putInt("Z", 3);
    position.put("old", legacyPos);
    position.put("modern", new IntArrayTag(new int[]{1, -2, 3}));
    BlockPos expected = new BlockPos(11, 18, 33);
    check(expected.equals(TagUtil.readOptionalPos(position, "old", new BlockPos(10, 20, 30))), "Legacy multiblock offset");
    check(expected.equals(TagUtil.readOptionalPos(position, "modern", new BlockPos(10, 20, 30))), "Native multiblock offset");
    check(TagUtil.readOptionalPos(position, "missing", BlockPos.ZERO) == null, "Missing offset");
    System.out.println("Fluid component snapshots, JSON/wire codecs, scaled tank simulation/save and legacy offsets: PASS");
  }
}
