package slimeknights.tconstruct.port;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.library.tools.part.MaterialItemData;

public final class MaterialItemDataSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    ItemStack stack = new ItemStack(Items.IRON_INGOT, 3);
    stack.set(DataComponents.CUSTOM_NAME, Component.literal("Material part"));
    MaterialItemData.update(stack, tag -> tag.putInt("OtherMod", 7));
    MaterialItemData.set(stack, "tconstruct:iron#test");
    ItemStack copied = stack.copy();
    var snapshot = MaterialItemData.read(stack);
    snapshot.putString(MaterialItemData.KEY, "mutated:read");
    check(MaterialItemData.read(stack).getString(MaterialItemData.KEY).equals("tconstruct:iron#test"), "Read snapshot mutated material component");
    MaterialItemData.set(stack, "tconstruct:copper");
    check(MaterialItemData.read(copied).getString(MaterialItemData.KEY).equals("tconstruct:iron#test"), "Material update mutated copied stack");
    check(MaterialItemData.read(stack).getInt("OtherMod") == 7
      && stack.get(DataComponents.CUSTOM_NAME).equals(Component.literal("Material part")), "Material update discarded unrelated data");
    MaterialItemData.update(stack, tag -> tag.putString(MaterialItemData.KEY, tag.getString(MaterialItemData.KEY).replace("copper", "gold")));
    check(MaterialItemData.read(stack).getString(MaterialItemData.KEY).equals("tconstruct:gold"), "Remapping did not commit through component API");
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      ItemStack.STREAM_CODEC.encode(buffer, stack);
      ItemStack decoded = ItemStack.STREAM_CODEC.decode(buffer);
      check(ItemStack.matches(stack, decoded) && buffer.readableBytes() == 0, "Material metadata lost native item wire round trip");
    } finally { buffer.release(); }
    MaterialItemData.set(ItemStack.EMPTY, "tconstruct:iron");
    check(MaterialItemData.read(ItemStack.EMPTY).isEmpty(), "Empty material item must remain empty");
    System.out.println("Material item component read/copy isolation, preserved data, committed remaps and native wire: PASS");
  }
}
