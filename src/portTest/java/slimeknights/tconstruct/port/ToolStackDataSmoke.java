package slimeknights.tconstruct.port;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import slimeknights.tconstruct.library.tools.nbt.ToolStackData;

import java.util.List;
import java.util.Map;

public final class ToolStackDataSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
  private static CompoundTag custom(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }
  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    ItemStack source = new ItemStack(Items.DIAMOND_SWORD);
    source.set(DataComponents.CUSTOM_NAME, Component.literal("Native name"));
    source.set(DataComponents.DAMAGE, 7);
    CompoundTag initial = new CompoundTag();
    initial.putString("unrelated", "preserved");
    initial.putInt("Damage", 99);
    source.set(DataComponents.CUSTOM_DATA, CustomData.of(initial));
    ToolStackData live = ToolStackData.from(source, true);
    check(live.data().getInt("Damage") == 7, "Native damage component was not authoritative");
    check(live.isSameStack(source), "Actual source binding missing");
    CompoundTag owned = new CompoundTag();
    owned.putInt("counter", 1);
    live.data().put("tic_persistent", owned);
    CustomData oldComponent = source.get(DataComponents.CUSTOM_DATA);
    check(!custom(source).contains("tic_persistent"), "Working edits leaked before commit");
    live.commit();
    check(custom(source).getCompound("tic_persistent").getInt("counter") == 1,
      "Persistent data was not committed");
    check(!oldComponent.copyTag().contains("tic_persistent"), "Immutable component was mutated in place");
    CompoundTag snapshot = custom(source);
    snapshot.getCompound("tic_persistent").putInt("counter", 50);
    check(custom(source).getCompound("tic_persistent").getInt("counter") == 1,
      "Read snapshot leaked mutable data");

    // An independent item user edits an unrelated key after our view was created.
    CustomData.update(DataComponents.CUSTOM_DATA, source, tag -> tag.putString("external", "kept"));
    check(!live.isSameStack(source), "External component replacement did not invalidate cache identity");
    owned.putInt("counter", 2);
    live.commit();
    check(custom(source).getString("external").equals("kept")
      && custom(source).getString("unrelated").equals("preserved"), "Delta commit lost unrelated metadata");
    check(!live.isSameStack(source), "Delta commit masked outside changes from the cached working view");
    live.data().putInt("Damage", 8);
    live.data().putBoolean("Unbreakable", true);
    live.data().putInt("HideFlags", 4);
    live.commit();
    check(source.getOrDefault(DataComponents.DAMAGE, 0) == 8
      && source.get(DataComponents.UNBREAKABLE).equals(new Unbreakable(false)),
      "Vanilla damage/unbreakable component bridge failed");
    check(!custom(source).contains("Damage") && !custom(source).contains("Unbreakable"),
      "Vanilla bridge keys remained in custom data");

    ItemStack alias = live.createStack(1);
    ItemStack nativeCopy = source.copy();
    check(live.isSameStack(alias) && !live.isSameStack(nativeCopy), "Explicit binding and ordinary copy conflated");
    owned.putInt("counter", 3);
    live.commit();
    check(custom(alias).getCompound("tic_persistent").getInt("counter") == 3
      && custom(nativeCopy).getCompound("tic_persistent").getInt("counter") == 2,
      "Native ordinary copy was not isolated");
    check(alias.get(DataComponents.CUSTOM_NAME).equals(Component.literal("Native name")), "Native name lost");
    source.set(DataComponents.CUSTOM_NAME, Component.literal("External name"));
    check(!live.isSameStack(source), "External native component replacement did not invalidate tool cache");
    ToolStackData refreshed = ToolStackData.from(source, false);
    check(refreshed.createStack(1).get(DataComponents.CUSTOM_NAME).equals(Component.literal("External name")),
      "Refreshed view discarded external native component metadata");


    ToolStackData detached = ToolStackData.from(source, false);
    detached.data().getCompound("tic_persistent").putInt("counter", 4);
    detached.commit();
    check(!detached.isSameStack(source)
      && custom(source).getCompound("tic_persistent").getInt("counter") == 3, "Detached copy modified source");
    ItemStack detachedStack = detached.createStack(1);
    check(custom(detachedStack).getCompound("tic_persistent").getInt("counter") == 4,
      "Detached working changes lost on stack creation");
    ToolStackData copied = live.copy();
    copied.data().getCompound("tic_persistent").putInt("counter", 5);
    copied.commit();
    check(custom(copied.createStack(1)).getCompound("tic_persistent").getInt("counter") == 5
      && custom(source).getCompound("tic_persistent").getInt("counter") == 3, "Tool data copy shared mutable state");

    // copyData=true deliberately detaches a previously bound output.
    live.updateStack(alias, true);
    owned.putInt("counter", 6);
    live.commit();
    check(!live.isSameStack(alias) && custom(alias).getCompound("tic_persistent").getInt("counter") == 3,
      "Snapshot output retained a live binding");
    ListTag slots = new ListTag();
    CompoundTag slot = new CompoundTag();
    slot.putInt("Slot", 2);
    slots.add(slot);
    live.data().getCompound("tic_persistent").put("inventory", slots);
    live.commit();
    slot.putInt("Slot", 0);
    check(custom(source).getCompound("tic_persistent").getList("inventory", 10).getCompound(0).getInt("Slot") == 2,
      "Nested working mutation leaked without commit");
    live.commit();
    check(custom(source).getCompound("tic_persistent").getList("inventory", 10).getCompound(0).getInt("Slot") == 0,
      "Nested mutation was not included in commit");
    live.data().remove("tic_persistent");
    live.data().remove("Unbreakable");
    live.commit();
    check(!custom(source).contains("tic_persistent") && !source.has(DataComponents.UNBREAKABLE),
      "Owned field removal was not committed");

    ItemStack saved = ItemStack.parse(registries, source.saveOptional(registries)).orElseThrow();
    check(ItemStack.isSameItemSameComponents(source, saved), "Native save round trip changed tool components");
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      ItemStack.STREAM_CODEC.encode(buffer, source);
      ItemStack decoded = ItemStack.STREAM_CODEC.decode(buffer);
      check(buffer.readableBytes() == 0 && ItemStack.isSameItemSameComponents(source, decoded),
        "Native component wire round trip failed");
    } finally { buffer.release(); }
    live.detach();
    live.data().putBoolean("tic_broken", true);
    live.commit();
    check(!custom(source).getBoolean("tic_broken"), "Detached stale view still changed its former source");
    System.out.println("Native tool data delta commits, damage/unbreakable, nested snapshots, bindings, copies and save/wire: PASS");
  }
}
