package slimeknights.tconstruct.port;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStorage;
import slimeknights.tconstruct.library.tools.nbt.ToolStackData;

import java.util.List;
import java.util.Map;

public final class ToolStorageSmoke {
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    ToolStackDataSmoke.main(args);
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    ResourceLocation tank = ResourceLocation.parse("tconstruct:tank_fluid");
    ResourceLocation inventory = ResourceLocation.parse("tconstruct:inventory");
    FluidStack fluid = new FluidStack(Fluids.WATER, 500);
    fluid.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
    ItemStack item = new ItemStack(Items.DIAMOND, 3);
    item.set(DataComponents.CUSTOM_NAME, Component.literal("Stored native name"));
    ToolStorage storage = new ToolStorage(Map.of(tank, fluid), Map.of(inventory, Map.of(300, item)));
    fluid.setAmount(1);
    item.setCount(1);
    check(storage.fluid(tank).getAmount() == 500, "input fluid aliases storage");
    check(storage.inventory(inventory).get(300).getCount() == 3, "input item aliases storage");
    storage.fluid(tank).setAmount(2);
    storage.inventory(inventory).get(300).setCount(2);
    check(storage.fluid(tank).getAmount() == 500, "fluid getter aliases storage");
    check(storage.inventory(inventory).get(300).getCount() == 3, "item getter aliases storage");
    var ops = RegistryOps.create(NbtOps.INSTANCE, registries);
    var encoded = ToolStorage.CODEC.encodeStart(ops, storage).getOrThrow();
    ToolStorage decoded = ToolStorage.CODEC.parse(ops, encoded).getOrThrow();
    check(storage.equals(decoded) && storage.hashCode() == decoded.hashCode(), "native save round trip changed storage");
    check(decoded.fluid(tank).get(DataComponents.POTION_CONTENTS).equals(new PotionContents(Potions.HEALING)), "potion component lost");
    RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      ToolStorage.STREAM_CODEC.encode(buffer, storage);
      check(storage.equals(ToolStorage.STREAM_CODEC.decode(buffer)), "native wire round trip changed storage");
      check(buffer.readableBytes() == 0, "wire bytes left over");
    } finally { buffer.release(); }
    ToolStorage cleared = storage.withFluid(tank, FluidStack.EMPTY).withInventory(inventory, Map.of());
    check(cleared.isEmpty() && !storage.isEmpty(), "immutable removal damaged original");
    net.minecraft.nbt.CompoundTag legacy = new net.minecraft.nbt.CompoundTag();
    net.minecraft.nbt.CompoundTag persistent = new net.minecraft.nbt.CompoundTag();
    net.minecraft.nbt.CompoundTag tankData = new net.minecraft.nbt.CompoundTag();
    tankData.putString("FluidName", "minecraft:water");
    tankData.putInt("Amount", 250);
    net.minecraft.nbt.CompoundTag fluidTag = new net.minecraft.nbt.CompoundTag();
    fluidTag.putString("Potion", "minecraft:healing");
    fluidTag.putString("custom_marker", "retained");
    tankData.put("Tag", fluidTag);
    persistent.put(tank.toString(), tankData);
    net.minecraft.nbt.CompoundTag oldItem = new net.minecraft.nbt.CompoundTag();
    oldItem.putString("id", "minecraft:diamond");
    oldItem.putByte("Count", (byte) 4);
    oldItem.putInt("Slot", 300);
    oldItem.putInt("tic_remaining_time", 37);
    net.minecraft.nbt.ListTag oldItems = new net.minecraft.nbt.ListTag();
    oldItems.add(oldItem);
    persistent.put(inventory.toString(), oldItems);
    legacy.put("tic_persistent", persistent);
    legacy.putString("unrelated", "preserved");
    var migrated = slimeknights.tconstruct.library.tools.nbt.ToolStorageLegacy.read(legacy, ToolStorage.EMPTY, ops);
    check(migrated.changed(), "legacy storage not migrated");
    check(migrated.storage().fluid(tank).getAmount() == 250, "legacy amount changed");
    check(migrated.storage().fluid(tank).get(DataComponents.POTION_CONTENTS).equals(new PotionContents(Potions.HEALING)), "legacy potion metadata lost");
    check(migrated.storage().fluid(tank).getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("custom_marker").equals("retained"), "legacy custom fluid data lost");
    check(migrated.storage().inventory(inventory).get(300).getCount() == 4, "legacy slot/count lost");
    var migratedPersistent = migrated.custom().getCompound("tic_persistent");
    check(migratedPersistent.getList(inventory.toString(), net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getInt("tic_remaining_time") == 37, "cooking metadata lost");
    check(migrated.custom().getString("unrelated").equals("preserved") && legacy.getCompound("tic_persistent").contains(tank.toString()), "migration mutated input or unrelated data");
    ItemStack tool = new ItemStack(Items.DIAMOND_SWORD);
    ToolStackData data = ToolStackData.from(tool, true);
    ItemContainerContents contents = ItemContainerContents.fromItems(List.of(storage.inventory(inventory).get(300)));
    data.setComponent(DataComponents.CONTAINER, contents);
    check(tool.get(DataComponents.CONTAINER).equals(contents) && data.isSameStack(tool), "native component was not synchronized");
    ToolStackData copy = data.copy();
    data.setComponent(DataComponents.CONTAINER, null);
    check(copy.createStack(1).has(DataComponents.CONTAINER) && !tool.has(DataComponents.CONTAINER), "native component copy/removal aliased");
    tool.set(DataComponents.CUSTOM_NAME, Component.literal("Outside edit"));
    data.setComponent(DataComponents.CONTAINER, contents);
    check(!data.isSameStack(tool), "component synchronization concealed an external edit");
    check(tool.get(DataComponents.CUSTOM_NAME).getString().equals("Outside edit"), "outside edit lost");
    System.out.println("Tool storage native persistence, wire, copies and component cache checks PASS");
  }
}
