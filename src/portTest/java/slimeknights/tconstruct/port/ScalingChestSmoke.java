package slimeknights.tconstruct.port;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.tables.block.entity.inventory.ScalingChestItemHandler;

public final class ScalingChestSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static ScalingChestItemHandler inventory() {
    return new ScalingChestItemHandler(12) {
      @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
      @Override public int getSlotLimit(int slot) { return 8; }
    };
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var source = inventory();
    ItemStack named = new ItemStack(Items.IRON_INGOT, 10);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("Stored part"));
    check(source.insertItem(4, named, true).getCount() == 2, "Simulated insert did not respect slot limit");
    check(source.getVisualSize() == 1 && source.getStackInSlot(4).isEmpty(), "Simulation changed inventory or visual size");
    check(source.insertItem(4, named, false).getCount() == 2, "Executed insert did not respect slot limit");
    check(source.getVisualSize() == 6 && source.getStackInSlot(4).getCount() == 8, "Inserted slot must show a trailing empty slot");
    check(source.extractItem(4, 8, true).getCount() == 8 && source.getVisualSize() == 6, "Simulated extraction changed visual size");
    source.setStackInSlot(11, new ItemStack(Items.GOLD_INGOT));
    check(source.getVisualSize() == 12, "Last slot exceeded inventory bounds");

    // Match AbstractChestBlockEntity's retained Items-only layout.
    CompoundTag saved = source.serializeNBT(registries);
    CompoundTag itemsOnly = new CompoundTag();
    itemsOnly.put("Items", saved.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND));
    var restored = inventory();
    restored.deserializeNBT(registries, itemsOnly);
    check(restored.getSlots() == 12 && restored.getVisualSize() == 12, "Items-only save changed physical or visible size");
    check(ItemStack.isSameItemSameComponents(restored.getStackInSlot(4), source.getStackInSlot(4)), "Registry-aware save lost item components");
    check(restored.getStackInSlot(4).getCount() == 8, "Save changed item count");
    restored.extractItem(11, 1, false);
    check(restored.getVisualSize() == 6, "Removing last item did not shrink visible range");
    restored.extractItem(4, 8, false);
    check(restored.getVisualSize() == 1, "Empty inventory must retain one visible slot");
    restored.deserializeNBT(registries, new CompoundTag());
    check(restored.getSlots() == 12 && restored.getVisualSize() == 1 && restored.getStackInSlot(4).isEmpty(), "Empty load retained old inventory contents");
    System.out.println("Native scaling chest persistence and simulation: PASS");
  }
}
