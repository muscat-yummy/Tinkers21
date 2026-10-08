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
    checkInventoryAndCooking(helper);
    helper.succeed();
    System.out.println("TiC actual world load, recipe registration, native codec Mixin, inventory and cooking PASS");
  }
  private static void checkInventoryAndCooking(GameTestHelper helper) {
    var pickaxe = BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse("tconstruct:pickaxe")).orElseThrow();
    ItemStack nativeTool = new ItemStack(pickaxe);
    var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(nativeTool);
    var modifier = slimeknights.tconstruct.library.modifiers.ModifierEntry.EMPTY.withLevel(1);
    ResourceLocation key = ResourceLocation.parse("tconstruct:bootstrap_inventory");
    var inventory = slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.builder().key(key).flatSlots(301);
    ItemStack named = new ItemStack(Items.DIAMOND, 3);
    named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Native inventory"));
    inventory.setStack(tool, modifier, 0, new ItemStack(Items.EMERALD, 2));
    inventory.setStack(tool, modifier, 300, named);
    named.setCount(1);
    var metadata = tool.getPersistentData().get(key,
      slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.GET_COMPOUND_LIST);
    metadata.getCompound(1).putInt("tic_remaining_time", 37);
    tool.getPersistentData().put(key, metadata);
    var twoSlots = slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.builder().key(key).flatSlots(2);
    helper.assertTrue(twoSlots.validate(tool, modifier) == null, "Inventory slot migration failed");
    ItemStack moved = twoSlots.getStack(tool, modifier, 1);
    helper.assertTrue(moved.getCount() == 3 && moved.get(DataComponents.CUSTOM_NAME).getString().equals("Native inventory"),
      "Slot migration lost item counts or components");
    metadata = tool.getPersistentData().get(key,
      slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.GET_COMPOUND_LIST);
    helper.assertTrue(metadata.getCompound(1).getInt("Slot") == 1 &&
      metadata.getCompound(1).getInt("tic_remaining_time") == 37, "Slot migration lost cooking metadata");
    ToolStorage before = nativeTool.get(ToolDataComponents.STORAGE.get());
    var oneSlot = slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.builder().key(key).flatSlots(1);
    helper.assertTrue(oneSlot.validate(tool, modifier) != null && before.equals(nativeTool.get(ToolDataComponents.STORAGE.get())),
      "Failed inventory shrink partially changed the native stack");
    helper.assertTrue(twoSlots.onRemoved(tool, modifier.getModifier()) != null, "Nonempty inventory allowed removal");
    moved.setCount(1);
    helper.assertTrue(twoSlots.getStack(tool, modifier, 1).getCount() == 3, "Inventory getter leaked a mutable stack");
    twoSlots.setStack(tool, modifier, 0, ItemStack.EMPTY);
    twoSlots.setStack(tool, modifier, 1, ItemStack.EMPTY);
    helper.assertTrue(twoSlots.onRemoved(tool, modifier.getModifier()) == null, "Empty inventory prevented removal");

    ResourceLocation inputKey = ResourceLocation.parse("tconstruct:bootstrap_cooking_input");
    ResourceLocation outputKey = ResourceLocation.parse("tconstruct:bootstrap_cooking_output");
    var input = slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.builder().key(inputKey).flatSlots(1);
    var smelting = new slimeknights.tconstruct.tools.modules.SmeltingModule(
      net.minecraft.world.item.crafting.RecipeType.SMELTING, 1, input, outputKey,
      slimeknights.tconstruct.library.tools.layout.Patterns.RESULT);
    ItemStack raw = new ItemStack(Items.RAW_IRON, 2);
    var recipe = helper.getLevel().getRecipeManager().getRecipeFor(
      net.minecraft.world.item.crafting.RecipeType.SMELTING,
      new net.minecraft.world.item.crafting.SingleRecipeInput(raw), helper.getLevel()).orElseThrow();
    ItemStack expected = recipe.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(raw),
      helper.getLevel().registryAccess());
    input.setStack(tool, modifier, 0, raw);
    var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    float halfHeat = recipe.value().getCookingTime() / 2f;
    smelting.afterSlingLaunch(tool, modifier, player, player, modifier, halfHeat, 2);
    helper.assertTrue(input.getStack(tool, modifier, 0).getCount() == 2 &&
      smelting.output().getStack(tool, modifier, 0).isEmpty(), "Partial cooking consumed items");
    var timers = tool.getPersistentData().get(inputKey,
      slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.GET_COMPOUND_LIST);
    int remaining = timers.getCompound(0).getInt("tic_remaining_time");
    helper.assertTrue(remaining > 0 && remaining < recipe.value().getCookingTime(), "Partial cooking timer was not committed");
    smelting.afterSlingLaunch(tool, modifier, player, player, modifier, halfHeat, 2);
    helper.assertTrue(input.getStack(tool, modifier, 0).getCount() == 1 &&
      ItemStack.matches(smelting.output().getStack(tool, modifier, 0), expected), "Cooking did not update component-backed items");
  }
}
