package slimeknights.tconstruct.port;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.common.network.InventorySlotSyncPacket;
import slimeknights.tconstruct.smeltery.network.FluidUpdatePacket;
import slimeknights.tconstruct.library.utils.RegistryBuffer;

public final class InventoryPacketSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    BlockPos pos = new BlockPos(-11, 73, 42);
    ItemStack named = new ItemStack(Items.IRON_INGOT, 7);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Synced part"));
    for (ItemStack value : new ItemStack[] {named, ItemStack.EMPTY}) {
      var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
      try {
        new InventorySlotSyncPacket(value, 5, pos).encode(buffer);
        var decoded = new InventorySlotSyncPacket(buffer);
        check(decoded.pos.equals(pos) && decoded.slot == 5, "Item packet position or slot changed");
        check(ItemStack.matches(value, decoded.itemStack), "Item packet changed count or component patch");
        check(buffer.readableBytes() == 0, "Item packet left unread bytes");
      } finally { buffer.release(); }
    }

    FluidStack namedFluid = new FluidStack(Fluids.WATER, 275);
    namedFluid.set(DataComponents.CUSTOM_NAME, Component.literal("Synced fluid"));
    for (FluidStack value : new FluidStack[] {namedFluid, FluidStack.EMPTY}) {
      var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
      try {
        new FluidUpdatePacket(pos, value).encode(buffer);
        var decoded = new FluidUpdatePacket(buffer);
        FluidStack[] received = {null};
        decoded.handleBlockEntity(null, fluid -> received[0] = fluid);
        check(decoded.pos().equals(pos), "Fluid packet position changed");
        check(received[0] != null && FluidStack.matches(value, received[0]), "Fluid packet changed amount or component patch");
        check(buffer.readableBytes() == 0, "Fluid packet left unread bytes");
      } finally { buffer.release(); }
    }

    var entitySerializer = new slimeknights.tconstruct.tools.network.FluidDataSerializer();
    for (FluidStack value : new FluidStack[] {namedFluid, FluidStack.EMPTY}) {
      var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
      try {
        entitySerializer.codec().encode(buffer, value);
        var decoded = entitySerializer.codec().decode(buffer);
        check(FluidStack.matches(value, decoded), "Entity fluid sync lost amount or component patch");
        check(buffer.readableBytes() == 0, "Entity fluid sync left unread bytes");
      } finally { buffer.release(); }
    }
    var snapshot = entitySerializer.copy(namedFluid);
    snapshot.setAmount(1);
    snapshot.set(DataComponents.CUSTOM_NAME, Component.literal("Changed copy"));
    check(namedFluid.getAmount() == 275
      && namedFluid.get(DataComponents.CUSTOM_NAME).equals(Component.literal("Synced fluid")),
      "Entity fluid snapshot mutated the source");
    System.out.println("Native entity fluid codec empty/component round trips and isolated snapshots: PASS");

    FriendlyByteBuf plain = new FriendlyByteBuf(Unpooled.buffer());
    try {
      boolean rejected = false;
      try { RegistryBuffer.require(plain); } catch (IllegalArgumentException expected) { rejected = true; }
      check(rejected, "Missing registry context must be rejected rather than silently dropping components");
    } finally { plain.release(); }
    System.out.println("Native item and fluid update packet empty/component round trips: PASS");
  }
}
