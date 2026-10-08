package slimeknights.tconstruct.port;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.ToolStorage;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class BootstrapGameTest {
  @GameTest(template = "bootstrap", timeoutTicks = 100)
  public static void nativeStorageAfterWorldLoad(GameTestHelper helper) {
    helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals("tconstruct")),
      "TiC items were not registered");
    helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream()
      .anyMatch(recipe -> recipe.id().getNamespace().equals("tconstruct")), "TiC recipes were not loaded");
    ResourceLocation key = ResourceLocation.fromNamespaceAndPath("tconstruct", "bootstrap_tank");
    CompoundTag fluid = new CompoundTag();
    fluid.putString("FluidName", "minecraft:water");
    fluid.putInt("Amount", 250);
    CompoundTag persistent = new CompoundTag();
    persistent.put(key.toString(), fluid);
    CompoundTag custom = new CompoundTag();
    custom.put("tic_persistent", persistent);
    ItemStack original = new ItemStack(Items.DIAMOND_SWORD);
    original.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
    ItemStack decoded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, original).getOrThrow()).getOrThrow();
    ToolStorage migrated = decoded.get(ToolDataComponents.STORAGE.get());
    helper.assertTrue(migrated != null && migrated.fluid(key).getAmount() == 250,
      "Native ItemStack codec Mixin did not migrate legacy tool storage");
    ItemStack strict = ItemStack.SINGLE_ITEM_CODEC.parse(ops,
      ItemStack.SINGLE_ITEM_CODEC.encodeStart(ops, original).getOrThrow()).getOrThrow();
    helper.assertTrue(strict.has(ToolDataComponents.STORAGE.get()), "Single-item native codec migration was not applied");
    System.out.println("TiC actual world load, recipe registration and native codec Mixin PASS");
    helper.succeed();
  }
}
