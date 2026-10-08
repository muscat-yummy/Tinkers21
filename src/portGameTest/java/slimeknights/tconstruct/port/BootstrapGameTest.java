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
    for (String recipeId : new String[] {
        "tconstruct:tools/armor/travelers/boots", "tconstruct:common/materials/steel_ingot_from_block",
        "tconstruct:common/materials/knightmetal_nugget_blasting", "tconstruct:smeltery/seared/duct_retextured"}) {
      helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(recipeId)).isPresent(),
        "Retained native recipe did not load: " + recipeId);
    }
    helper.assertTrue(slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader.getInstance()
      .get(ResourceLocation.parse("tconstruct:pickaxe")) != slimeknights.tconstruct.library.tools.layout.StationSlotLayout.EMPTY,
      "Legacy SNBT station icon did not load");
    var ironRecipe = helper.getLevel().getRecipeManager()
      .byKey(ResourceLocation.parse("tconstruct:tools/materials/iron/ingot")).orElseThrow().value();
    helper.assertTrue(ironRecipe instanceof slimeknights.tconstruct.library.recipe.material.MaterialRecipe &&
      ((slimeknights.tconstruct.library.recipe.material.MaterialRecipe) ironRecipe).getIngredient()
        .test(new ItemStack(Items.IRON_INGOT)), "Native iron ingots did not enter the retained material recipe");
    helper.assertTrue(new ItemStack(Items.STRING).is(net.minecraft.tags.TagKey.create(
      net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("forge:string"))),
      "Native string contributors did not enter retained tool recipes");
    checkCoreToolPersistence(helper);
    checkInventoryAndCooking(helper);
    checkNativeHarvest(helper);
    checkNativeMelee(helper);
    checkNativeConstruction(helper);
    helper.succeed();
    slimeknights.tconstruct.TConstruct.LOG.info("TiC actual world load, recipe registration, native codec Mixin, inventory and cooking PASS");
  }

  private static void checkCoreToolPersistence(GameTestHelper helper) {
    var ironId = new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct:iron");
    var iron = slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(ironId);
    helper.assertTrue(ironId.equals(iron.getIdentifier()), "Actual iron material was not loaded");
    var item = BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse("tconstruct:pickaxe")).orElseThrow().value();
    var definition = ((slimeknights.tconstruct.library.tools.item.IModifiable) item).getToolDefinition();
    var materials = slimeknights.tconstruct.library.tools.nbt.MaterialNBT.of(iron, iron, iron);
    var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.createTool(item, definition, materials);
    var miningSpeed = slimeknights.tconstruct.library.tools.stat.ToolStats.MINING_SPEED;
    int durability = tool.getStats().getInt(slimeknights.tconstruct.library.tools.stat.ToolStats.DURABILITY);
    float initialSpeed = tool.getStats().get(miningSpeed);
    helper.assertTrue(durability > 1 && initialSpeed > 0, "Actual material tool stats were not built");
    var haste = new slimeknights.tconstruct.library.modifiers.ModifierId("tconstruct:haste");
    tool.addModifier(haste, 1);
    float modifiedSpeed = tool.getStats().get(miningSpeed);
    helper.assertTrue(modifiedSpeed > initialSpeed, "Actual haste modifier did not rebuild mining speed");
    tool.setDamage(1);
    ItemStack nativeTool = tool.createStack();
    nativeTool.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Material tool"));
    helper.assertTrue(nativeTool.getMaxDamage() == durability && nativeTool.getDamageValue() == 1,
      "Native item durability callbacks do not reflect material tool data");
    helper.assertTrue(slimeknights.tconstruct.library.utils.ItemAttributeData.forSlot(
      nativeTool.getAttributeModifiers(), net.minecraft.world.entity.EquipmentSlot.MAINHAND)
        .containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
      "Actual material tool attributes were not exposed to native equipment");
    checkLegacyMaterialTool(helper, nativeTool, modifiedSpeed);
    var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
    ItemStack loaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, nativeTool).getOrThrow()).getOrThrow();
    var restored = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(loaded);
    helper.assertTrue(restored.getMaterials().equals(materials) && restored.getDamage() == 1 && !restored.isBroken(),
      "Native material tool loading lost materials or damage");
    helper.assertTrue(restored.getStats().get(miningSpeed) == modifiedSpeed &&
      "Material tool".equals(loaded.get(DataComponents.CUSTOM_NAME).getString()),
      "Native tool load lost modifier stats or custom name");
    var detached = restored.copy();
    detached.setDamage(durability);
    helper.assertTrue(detached.isBroken() && restored.getDamage() == 1 && !restored.isBroken(),
      "Detached core tool data shared durability state");
    ItemStack broken = detached.createStack();
    ItemStack loadedBroken = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, broken).getOrThrow()).getOrThrow();
    var restoredBroken = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(loadedBroken);
    helper.assertTrue(restoredBroken.isBroken() && restoredBroken.getCurrentDurability() == 0,
      "Native load did not retain the broken tool state");
    restoredBroken.setDamage(0);
    restoredBroken.removeModifier(haste, 1);
    helper.assertTrue(!restoredBroken.isBroken() && restoredBroken.getStats().get(miningSpeed) == initialSpeed,
      "Repair and modifier removal did not rebuild the actual core tool");
  }


  private static void checkLegacyMaterialTool(GameTestHelper helper, ItemStack tool, float miningSpeed) {
    CompoundTag old = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    old.putInt("Damage", 1);
    old.putBoolean("Unbreakable", true);
    old.putInt("HideFlags", 4);
    CompoundTag display = new CompoundTag();
    display.putString("Name", "{\"text\":\"Legacy material tool\"}");
    old.put("display", display);
    ResourceLocation inventoryKey = ResourceLocation.parse("tconstruct:bootstrap_legacy_inventory");
    ResourceLocation fluidKey = ResourceLocation.parse("tconstruct:bootstrap_legacy_fluid");
    CompoundTag persistent = old.getCompound("tic_persistent");
    CompoundTag entry = new CompoundTag();
    entry.putInt("Slot", 300);
    entry.putString("id", "minecraft:diamond");
    entry.putByte("Count", (byte) 4);
    entry.putInt("tic_remaining_time", 37);
    CompoundTag itemTag = new CompoundTag();
    CompoundTag itemDisplay = new CompoundTag();
    itemDisplay.putString("Name", "{\"text\":\"Legacy diamond\"}");
    itemTag.put("display", itemDisplay);
    entry.put("tag", itemTag);
    var inventory = new net.minecraft.nbt.ListTag();
    inventory.add(entry);
    persistent.put(inventoryKey.toString(), inventory);
    CompoundTag fluid = new CompoundTag();
    fluid.putString("FluidName", "minecraft:water");
    fluid.putInt("Amount", 250);
    persistent.put(fluidKey.toString(), fluid);
    old.put("tic_persistent", persistent);
    ItemStack fixed = new ItemStack(tool.getItem());
    var jsonOps = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, helper.getLevel().registryAccess());
    fixed.applyComponents(slimeknights.mantle.data.loadable.common.LegacyItemData.convert(tool.getItem(), old, jsonOps));
    var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
    ItemStack loaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, fixed).getOrThrow()).getOrThrow();
    helper.assertTrue(loaded.getDamageValue() == 1 && loaded.has(DataComponents.UNBREAKABLE) &&
      !loaded.get(DataComponents.UNBREAKABLE).showInTooltip(), "Legacy native durability or tooltip flags were lost");
    helper.assertTrue("Legacy material tool".equals(loaded.get(DataComponents.CUSTOM_NAME).getString()),
      "Legacy native tool name was lost");
    var restored = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(loaded);
    helper.assertTrue(restored.getStats().get(slimeknights.tconstruct.library.tools.stat.ToolStats.MINING_SPEED) == miningSpeed &&
      restored.getMaterials().size() == 3, "Legacy tool materials or modifier stats were lost");
    ToolStorage storage = loaded.get(ToolDataComponents.STORAGE.get());
    ItemStack nested = storage.inventory(inventoryKey).get(300);
    helper.assertTrue(nested.getCount() == 4 && "Legacy diamond".equals(nested.get(DataComponents.CUSTOM_NAME).getString()) &&
      storage.fluid(fluidKey).getAmount() == 250, "Legacy material tool storage lost counts, names or fluids");
    var metadata = restored.getPersistentData().get(inventoryKey,
      slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.GET_COMPOUND_LIST);
    helper.assertTrue(metadata.getCompound(0).getInt("Slot") == 300 &&
      metadata.getCompound(0).getInt("tic_remaining_time") == 37 && !metadata.getCompound(0).contains("id"),
      "Legacy storage migration lost cooking metadata or retained duplicate contents");
    ItemStack copied = loaded.copy();
    helper.assertTrue(ItemStack.matches(loaded, copied), "Actual legacy tool copying changed native components");
  }


  private static void checkNativeHarvest(GameTestHelper helper) {
    var iron = slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(
      new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct:iron"));
    var item = BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse("tconstruct:pickaxe")).orElseThrow().value();
    var definition = ((slimeknights.tconstruct.library.tools.item.IModifiable) item).getToolDefinition();
    var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.createTool(item, definition,
      slimeknights.tconstruct.library.tools.nbt.MaterialNBT.of(iron, iron, iron));
    ItemStack stack = tool.createStack();
    var player = new net.neoforged.testframework.gametest.ExtendedGameTestHelper(helper.testInfo)
      .makeTickingMockServerPlayerInLevel(net.minecraft.world.level.GameType.SURVIVAL);
    helper.assertTrue(slimeknights.tconstruct.library.tools.capability.PersistentDataCapability.getOptionalData(player).isPresent() &&
      slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.getOptionalData(player).isPresent() &&
      slimeknights.tconstruct.gadgets.capability.PiggybackCapability.getOptionalData(player).isPresent(),
      "Actual player attachment capabilities were not registered before login");
    player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
    var relative = new net.minecraft.core.BlockPos(0, 1, 0);
    var pos = helper.absolutePos(relative);
    player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
    helper.setBlock(relative, net.minecraft.world.level.block.Blocks.STONE);
    var events = new java.util.concurrent.atomic.AtomicInteger();
    var cancel = new java.util.concurrent.atomic.AtomicBoolean(true);
    java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> listener = event -> {
      if (event.getPlayer() == player && event.getPos().equals(pos)) {
        events.incrementAndGet();
        if (cancel.get()) event.setCanceled(true);
      }
    };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
    try {
      int damage = stack.getDamageValue();
      helper.assertTrue(!player.gameMode.destroyBlock(pos) &&
        helper.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.STONE) &&
        stack.getDamageValue() == damage && events.get() == 1,
        "Canceled native harvest changed the block/tool or fired permission twice");
      cancel.set(false);
      events.set(0);
      helper.assertTrue(player.gameMode.destroyBlock(pos) && helper.getLevel().getBlockState(pos).isAir() &&
        events.get() == 1, "Actual harvest bridge did not destroy the block after one approved permission event");
      helper.assertTrue(stack.getDamageValue() > damage,
        "Actual native harvest did not commit tool durability consumption");
      var stored = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
      helper.assertTrue(stored.getDamage() == stack.getDamageValue() && !stored.isBroken(),
        "Actual harvest left native and tool durability inconsistent");
    } finally {
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
      player.disconnectGameTest();
    }
  }


  private static void checkNativeMelee(GameTestHelper helper) {
    var iron = slimeknights.tconstruct.library.materials.MaterialRegistry.getMaterial(
      new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct:iron"));
    var item = BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse("tconstruct:sword")).orElseThrow().value();
    var definition = ((slimeknights.tconstruct.library.tools.item.IModifiable) item).getToolDefinition();
    var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.createTool(item, definition,
      slimeknights.tconstruct.library.tools.nbt.MaterialNBT.of(iron, iron, iron));
    ItemStack stack = tool.createStack();
    var player = new net.neoforged.testframework.gametest.ExtendedGameTestHelper(helper.testInfo)
      .makeTickingMockServerPlayerInLevel(net.minecraft.world.level.GameType.SURVIVAL);
    var target = new net.minecraft.world.entity.monster.Zombie(
      net.minecraft.world.entity.EntityType.ZOMBIE, helper.getLevel());
    target.setNoAi(true);
    var pos = helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0));
    target.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    helper.getLevel().addFreshEntity(target);
    player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
    player.doTick();
    var events = new java.util.concurrent.atomic.AtomicInteger();
    var cancel = new java.util.concurrent.atomic.AtomicBoolean(true);
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.AttackEntityEvent> listener = event -> {
      if (event.getEntity() == player && event.getTarget() == target) {
        events.incrementAndGet();
        if (cancel.get()) event.setCanceled(true);
      }
    };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
    try {
      helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > 1,
        "Native equipped sword attributes did not reach the actual player");
      float health = target.getHealth();
      int damage = stack.getDamageValue();
      player.attack(target);
      helper.assertTrue(target.getHealth() == health && stack.getDamageValue() == damage && events.get() == 1,
        "Canceled native melee changed target health or tool durability");
      cancel.set(false);
      events.set(0);
      player.attack(target);
      helper.assertTrue(target.getHealth() < health && events.get() == 1 && stack.getDamageValue() > damage,
        "Actual native melee did not damage the target/tool after one approved attack event");
      var stored = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack);
      helper.assertTrue(stored.getDamage() == stack.getDamageValue() && !stored.isBroken(),
        "Actual melee left native and tool durability inconsistent");
      helper.assertTrue(!target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
        .hasModifier(ResourceLocation.parse("tconstruct:anti_knockback")),
        "Actual melee leaked temporary knockback state");
    } finally {
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
      target.discard();
      player.disconnectGameTest();
    }
  }


  private static void checkNativeConstruction(GameTestHelper helper) {
    var builderPos = new net.minecraft.core.BlockPos(1, 1, 0);
    var stationPos = new net.minecraft.core.BlockPos(2, 1, 0);
    helper.setBlock(builderPos, slimeknights.tconstruct.tables.TinkerTables.partBuilder.get());
    helper.setBlock(stationPos, slimeknights.tconstruct.tables.TinkerTables.tinkerStation.get());
    var builder = (slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity)
      helper.getLevel().getBlockEntity(helper.absolutePos(builderPos));
    var station = (slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity)
      helper.getLevel().getBlockEntity(helper.absolutePos(stationPos));
    var player = new net.neoforged.testframework.gametest.ExtendedGameTestHelper(helper.testInfo)
      .makeTickingMockServerPlayerInLevel(net.minecraft.world.level.GameType.SURVIVAL);
    try {
      helper.assertTrue(builder != null && station != null, "Actual construction tables did not create block entities");
      var holder = helper.getLevel().getRecipeManager()
        .byKey(ResourceLocation.parse("tconstruct:tools/building/pickaxe")).orElseThrow();
      var recipe = (slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe) holder.value();
      var parts = recipe.getToolParts();
      helper.assertTrue(parts.size() == 3, "Actual pickaxe building recipe lost its tool parts");
      builder.setItem(slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity.MATERIAL_SLOT,
        new ItemStack(Items.OAK_PLANKS, 64));
      builder.setItem(slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity.PATTERN_SLOT,
        new ItemStack(slimeknights.tconstruct.tables.TinkerTables.pattern.get(), parts.size()));
      int expectedMaterials = 64;
      int expectedPatterns = parts.size();
      for (int i = 0; i < parts.size(); i++) {
        builder.selectRecipe(slimeknights.tconstruct.library.recipe.partbuilder.Pattern.fromItem(parts.get(i)));
        var selected = builder.getPartRecipe();
        helper.assertTrue(selected != null, "Actual part builder did not select a required pickaxe part");
        int used = selected.getItemsUsed(builder.getInventoryWrapper());
        ItemStack part = builder.calcResult(player);
        helper.assertTrue(!part.isEmpty() && part.getItem() == parts.get(i).asItem() &&
          new slimeknights.tconstruct.library.materials.definition.MaterialId("tconstruct:wood").equals(
            slimeknights.tconstruct.library.tools.part.IMaterialItem.getMaterialFromStack(part)),
          "Actual part building lost its output or material");
        ItemStack input = part.copy();
        builder.onCraft(player, part, part.getCount());
        expectedMaterials -= used;
        expectedPatterns--;
        helper.assertTrue(builder.getItem(0).getCount() == expectedMaterials &&
          builder.getItem(1).getCount() == expectedPatterns,
          "Actual part building did not consume its materials and pattern exactly once");
        station.setItem(i + slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.INPUT_SLOT, input);
      }
      ItemStack result = station.calcResult(player);
      helper.assertTrue(!result.isEmpty() &&
        result.getItem() == slimeknights.tconstruct.tools.TinkerTools.pickaxe.get(),
        "Actual station did not assemble the built pickaxe parts");
      var built = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(result);
      helper.assertTrue(built.getMaterials().size() == 3 && built.getCurrentDurability() > 0 && !built.isBroken(),
        "Actual construction produced an unusable material tool");
      ItemStack output = result.copy();
      station.onCraft(player, result, result.getCount());
      for (int i = 0; i < parts.size(); i++) {
        helper.assertTrue(station.getItem(i + 1).isEmpty(), "Actual station did not consume a building input");
      }
      var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
      ItemStack loaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, output).getOrThrow()).getOrThrow();
      helper.assertTrue(ItemStack.matches(output, loaded), "Actual constructed tool changed during native persistence");
      var loadedTool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(loaded);
      loadedTool.setDamage(1);
      station.setItem(0, loaded);
      station.setItem(1, new ItemStack(Items.OAK_PLANKS, 2));
      ItemStack repaired = station.calcResult(player);
      helper.assertTrue(!repaired.isEmpty() && repaired.getDamageValue() == 0 &&
        slimeknights.tconstruct.library.tools.nbt.ToolStack.from(repaired).getMaterials().equals(built.getMaterials()),
        "Actual station repair did not preserve materials or repair native damage");
      ItemStack repairedCopy = repaired.copy();
      station.onCraft(player, repaired, repaired.getCount());
      helper.assertTrue(station.getItem(0).isEmpty() && station.getItem(1).getCount() == 1,
        "Actual station repair did not consume one tool and one material");
      station.setItem(1, ItemStack.EMPTY);
      station.setItem(0, repairedCopy);
      var haste = new slimeknights.tconstruct.library.modifiers.ModifierId("tconstruct:haste");
      var upgradeSlot = slimeknights.tconstruct.library.tools.SlotType.UPGRADE;
      int slots = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(repairedCopy).getFreeSlots(upgradeSlot);
      float speed = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(repairedCopy).getStats()
        .get(slimeknights.tconstruct.library.tools.stat.ToolStats.MINING_SPEED);
      station.setItem(1, new ItemStack(Items.REDSTONE, 9));
      ItemStack partial = station.calcResult(player);
      helper.assertTrue(!partial.isEmpty(), "Actual station did not start an incremental modifier");
      var partialTool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(partial);
      helper.assertTrue(partialTool.getUpgrades().getEntry(haste).getAmount(0) == 9 &&
        partialTool.getFreeSlots(upgradeSlot) == slots - 1,
        "Actual partial modifier lost its amount or consumed an incorrect upgrade slot count");
      ItemStack partialCopy = partial.copy();
      station.onCraft(player, partial, partial.getCount());
      helper.assertTrue(station.getItem(0).isEmpty() && station.getItem(1).isEmpty(),
        "Actual partial modifier did not consume its inputs exactly once");
      ItemStack reloadedPartial = ItemStack.CODEC.parse(ops,
        ItemStack.CODEC.encodeStart(ops, partialCopy).getOrThrow()).getOrThrow();
      station.setItem(0, reloadedPartial);
      station.setItem(1, new ItemStack(Items.REDSTONE, 36));
      ItemStack complete = station.calcResult(player);
      helper.assertTrue(!complete.isEmpty(), "Actual station did not continue a persisted partial modifier");
      var completeTool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(complete);
      helper.assertTrue(completeTool.getUpgrades().getEntry(haste).getLevel() == 1 &&
        completeTool.getUpgrades().getEntry(haste).getAmount(45) == 45 &&
        completeTool.getFreeSlots(upgradeSlot) == slots - 1 &&
        completeTool.getStats().get(slimeknights.tconstruct.library.tools.stat.ToolStats.MINING_SPEED) > speed,
        "Actual resumed modifier lost its level/stats or consumed its upgrade slot twice");
      station.onCraft(player, complete, complete.getCount());
      helper.assertTrue(station.getItem(0).isEmpty() && station.getItem(1).isEmpty(),
        "Actual completed modifier did not consume its remaining inputs exactly once");

    } finally {
      player.disconnectGameTest();
    }
  }

  private static void checkInventoryAndCooking(GameTestHelper helper) {
    var pickaxe = BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse("tconstruct:pickaxe")).orElseThrow();
    ItemStack nativeTool = new ItemStack(pickaxe);
    nativeTool.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Verified tool"));
    ItemStack copiedTool = nativeTool.copy();
    helper.assertTrue(ItemStack.matches(nativeTool, copiedTool), "Native tool copying changed its components");
    var nativeOps = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
    ItemStack loadedTool = ItemStack.CODEC.parse(nativeOps,
      ItemStack.CODEC.encodeStart(nativeOps, nativeTool).getOrThrow()).getOrThrow();
    helper.assertTrue(ItemStack.matches(nativeTool, loadedTool), "Native tool load verification changed its components");
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
    smelting.afterSlingLaunch(tool, modifier, player, player, modifier, halfHeat, 2, net.minecraft.world.phys.Vec3.ZERO);
    helper.assertTrue(input.getStack(tool, modifier, 0).getCount() == 2 &&
      smelting.output().getStack(tool, modifier, 0).isEmpty(), "Partial cooking consumed items");
    var timers = tool.getPersistentData().get(inputKey,
      slimeknights.tconstruct.library.tools.capability.inventory.InventoryModule.GET_COMPOUND_LIST);
    int remaining = timers.getCompound(0).getInt("tic_remaining_time");
    helper.assertTrue(remaining > 0 && remaining < recipe.value().getCookingTime(), "Partial cooking timer was not committed");
    smelting.afterSlingLaunch(tool, modifier, player, player, modifier, halfHeat, 2, net.minecraft.world.phys.Vec3.ZERO);
    helper.assertTrue(input.getStack(tool, modifier, 0).getCount() == 1 &&
      ItemStack.matches(smelting.output().getStack(tool, modifier, 0), expected), "Cooking did not update component-backed items");
  }
}
