package slimeknights.tconstruct.port;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.PartBuilderScreen;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;
import slimeknights.tconstruct.tables.menu.PartBuilderContainerMenu;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tools.TinkerTools;

/** Isolated check of real rendering, integrated-world login and construction packets. */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class ClientBootstrapCheck {
  private static int step;
  private static int ticks;
  private static int totalTicks;
  private static int screenTicks;
  private static Screen lastScreen;
  private static int partIndex;
  private static int usedPlanks;
  private static volatile BlockPos builderPos;
  private static volatile BlockPos stationPos;
  private static volatile List<Item> parts = List.of();
  private static volatile boolean serverReady;
  private static boolean serverValidationRequested;
  private static volatile boolean tablesPlaced;
  private static boolean builderOpenRequested;
  private static volatile boolean stationOpenPending;
  private static volatile int baseUpgradeSlots;
  private static volatile float baseMiningSpeed;
  private static final ModifierId HASTE = new ModifierId("tconstruct:haste");
  private static volatile RuntimeException serverFailure;

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  private static void advance(int next) {
    step = next;
    ticks = 0;
    TConstruct.LOG.info("TiC actual client construction step {}", next);
  }

  /** Fixture setup/verification runs on the actual server thread. */
  private static void serverAction(Runnable action) {
    Minecraft.getInstance().getSingleplayerServer().execute(() -> {
      try {
        action.run();
      } catch (Throwable failure) {
        serverFailure = new IllegalStateException("Actual server construction check failed", failure);
      }
    });
  }

  /** Resolve and bake the real material override and GUI composite, never a fixed substitute. */
  private static List<Integer> geometry(Minecraft minecraft, ItemStack stack) {
    var model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 17);
    require(model != minecraft.getModelManager().getMissingModel(), "Missing actual material item model");
    model = model.applyTransform(ItemDisplayContext.GUI, new PoseStack(), false);
    List<Integer> signature = new ArrayList<>();
    for (var pass : model.getRenderPasses(stack, false)) {
      for (Direction face : Direction.values()) {
        for (var quad : pass.getQuads(null, face, RandomSource.create(17), ModelData.EMPTY, null)) {
          signature.add(Arrays.hashCode(quad.getVertices()));
        }
      }
      for (var quad : pass.getQuads(null, null, RandomSource.create(17), ModelData.EMPTY, null)) {
        signature.add(Arrays.hashCode(quad.getVertices()));
      }
    }
    require(!signature.isEmpty(), "Actual material model has no rendered geometry");
    return signature;
  }

  /** Wait for the actual close packet before opening the next station transaction. */
  private static void reopenStation(Minecraft minecraft, int transaction) {
    if (stationOpenPending || ticks % 10 != 0) return;
    stationOpenPending = true;
    var server = minecraft.getSingleplayerServer();
    var uuid = minecraft.player.getUUID();
    serverAction(() -> {
      var player = server.getPlayerList().getPlayer(uuid);
      if (player.containerMenu != player.inventoryMenu) {
        stationOpenPending = false;
        return;
      }
      var station = (TinkerStationBlockEntity) player.serverLevel().getBlockEntity(stationPos);
      int inventorySlot = -1;
      for (int i = 0; i < player.getInventory().items.size(); i++) {
        if (player.getInventory().getItem(i).getItem() == TinkerTools.pickaxe.get()) {
          require(inventorySlot == -1, "Actual construction duplicated a pickaxe");
          inventorySlot = i;
        }
      }
      require(inventorySlot >= 0, "Actual station transaction lost the player's tool");
      ItemStack tool = player.getInventory().getItem(inventorySlot);
      player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
      if (transaction == 0) {
        ToolStack.from(tool).setDamage(1);
        station.setItem(0, tool);
        station.setItem(1, new ItemStack(Items.OAK_PLANKS, 2));
      } else if (transaction == 1) {
        require(tool.getDamageValue() == 0 && station.getItem(1).getCount() == 1,
          "Actual repair packets did not consume exactly one material and repair the tool");
        var view = ToolStack.from(tool);
        baseUpgradeSlots = view.getFreeSlots(SlotType.UPGRADE);
        baseMiningSpeed = view.getStats().get(ToolStats.MINING_SPEED);
        station.setItem(0, tool);
        station.setItem(1, new ItemStack(Items.REDSTONE, 9));
      } else {
        var partial = ToolStack.from(tool);
        require(station.getItem(0).isEmpty() && station.getItem(1).isEmpty() &&
          partial.getUpgrades().getEntry(HASTE).getAmount(0) == 9 &&
          partial.getFreeSlots(SlotType.UPGRADE) == baseUpgradeSlots - 1,
          "Actual partial modifier packets lost their amount, slot or exact inputs");
        var ops = RegistryOps.create(NbtOps.INSTANCE, player.serverLevel().registryAccess());
        ItemStack loaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, tool).getOrThrow()).getOrThrow();
        require(ItemStack.matches(tool, loaded), "Actual partial tool changed during native save/load");
        station.setItem(0, loaded);
        station.setItem(1, new ItemStack(Items.REDSTONE, 36));
      }
      player.openMenu(station, buffer -> buffer.writeBlockPos(stationPos));
    });
  }

  private static ItemStack stationOutput(Minecraft minecraft) {
    var station = (TinkerStationBlockEntity) minecraft.level.getBlockEntity(stationPos);
    return minecraft.player.containerMenu.slots.get(station.getContainerSize()).getItem();
  }

  private static void takeStationOutput(Minecraft minecraft) {
    var station = (TinkerStationBlockEntity) minecraft.level.getBlockEntity(stationPos);
    minecraft.gameMode.handleInventoryMouseClick(minecraft.player.containerMenu.containerId,
      station.getContainerSize(), 0, ClickType.QUICK_MOVE, minecraft.player);
  }

  private static ItemStack inventoryTool(Minecraft minecraft) {
    return minecraft.player.getInventory().items.stream()
      .filter(stack -> stack.getItem() == TinkerTools.pickaxe.get()).findFirst().orElse(null);
  }

  @SubscribeEvent
  public static void clientTick(ClientTickEvent.Post event) {
    if (step == 19) return;
    if (serverFailure != null) throw serverFailure;
    Minecraft minecraft = Minecraft.getInstance();
    if (step > 0 && minecraft.screen instanceof DisconnectedScreen) {
      throw new IllegalStateException("Actual client disconnected during construction step " + step);
    }
    if (step == 1 && ticks > 60 && minecraft.screen instanceof TitleScreen) {
      throw new IllegalStateException("Actual fresh-world loading returned to the title screen");
    }
    if (++totalTicks > 6000) {
      throw new IllegalStateException("Actual construction timed out at step " + step + ": " +
        (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName()));
    }
    ticks++;
    if (minecraft.screen != lastScreen) {
      lastScreen = minecraft.screen;
      screenTicks = 0;
    } else if (minecraft.getOverlay() == null) {
      screenTicks++;
    }
    if (totalTicks % 200 == 0) {
      TConstruct.LOG.info("TiC client check waiting: step {}, screen {}, overlay {}", step,
        minecraft.screen == null ? "none" : minecraft.screen.getClass().getName(),
        minecraft.getOverlay() == null ? "none" : minecraft.getOverlay().getClass().getName());
    }
    if (minecraft.screen instanceof AccessibilityOnboardingScreen && minecraft.getOverlay() == null) {
      minecraft.options.onboardingAccessibilityFinished();
      minecraft.screen.onClose();
      return;
    }
    if (step == 0) {
      if (!(minecraft.screen instanceof TitleScreen) || minecraft.getOverlay() != null) {
        ticks = 0;
        return;
      }
      if (ticks < 20) return;
      require(minecraft.getWindow().getWidth() > 0 && minecraft.getItemRenderer() != null,
        "Actual graphical client did not initialize");
      TConstruct.LOG.info("TiC actual graphical client and initial resource reload PASS");
      minecraft.options.renderDistance().set(2);
      minecraft.options.pauseOnLostFocus = false;
      advance(1);
      minecraft.createWorldOpenFlows().createFreshLevel("port-client-construction",
        new LevelSettings("Port client construction", GameType.SURVIVAL, false, Difficulty.PEACEFUL,
          true, new GameRules(), WorldDataConfiguration.DEFAULT),
        new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
      return;
    }
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null ||
        minecraft.getSingleplayerServer() == null || minecraft.getOverlay() != null) return;
    if (step == 1) {
      if (minecraft.screen != null) return;
      require(MaterialRegistry.isFullyLoaded(), "Actual world did not load material definitions");
      var wood = new MaterialId("tconstruct:wood");
      var iron = new MaterialId("tconstruct:iron");
      ItemStack woodTool = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(),
        MaterialNBT.builder().add(wood).add(wood).add(wood).build());
      ItemStack ironTool = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(),
        MaterialNBT.builder().add(iron).add(iron).add(iron).build());
      require(!geometry(minecraft, woodTool).equals(geometry(minecraft, ironTool)),
        "Material tool rendering lost its wood/iron distinction");
      TConstruct.LOG.info("TiC actual joined-world material tool composition PASS");
      // Exercise the actual registered tint/model and native renderer after world resource reload.
      for (String id : new String[] {"tconstruct:ambidextrous", "tconstruct:haste"}) {
        ItemStack crystal = slimeknights.tconstruct.tools.item.ModifierCrystalItem.withModifier(new ModifierId(id));
        int expectedRgb = slimeknights.mantle.client.ResourceColorManager.getColor(
          slimeknights.mantle.util.Util.makeTranslationKey("modifier", new ModifierId(id))) & 0xFFFFFF;
        int actual = minecraft.getItemColors().getColor(crystal, 0);
        require((actual >>> 24) == 255 && (actual & 0xFFFFFF) == expectedRgb,
          "Actual modifier crystal tint is transparent or changed RGB: " + id + " color=" + Integer.toHexString(actual));
        require(minecraft.getResourceManager().getResource(TConstruct.getResource(
          "textures/item/materials/modifier_crystal.png")).isPresent(), "Modifier crystal PNG is absent");
        var model = minecraft.getItemRenderer().getModel(crystal, minecraft.level, minecraft.player, 17);
        require(model.getParticleIcon().contents().name().equals(TConstruct.getResource(
          "item/materials/modifier_crystal")), "Modifier crystal atlas sprite is missing or unrelated");
        geometry(minecraft, crystal);
        var buffers = minecraft.renderBuffers().bufferSource();
        minecraft.getItemRenderer().renderStatic(crystal, ItemDisplayContext.GUI, 0xF000F0, 0,
          new PoseStack(), buffers, minecraft.level, 17);
        buffers.endBatch();
      }
      ItemStack blankCrystal = new ItemStack(slimeknights.tconstruct.tools.TinkerModifiers.modifierCrystal.get());
      require(minecraft.getItemColors().getColor(blankCrystal, 0) == -1,
        "Unassigned modifier crystal lost its opaque white fallback");
      TConstruct.LOG.info("TiC actual modifier crystal opaque tint atlas geometry and native rendering PASS");

      var server = minecraft.getSingleplayerServer();
      var uuid = minecraft.player.getUUID();
      advance(2);
      serverAction(() -> {
        var player = server.getPlayerList().getPlayer(uuid);
        require(player != null, "Actual integrated-world player did not log in");
        var level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
          level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        }
        builderPos = origin.offset(1, 0, 0);
        stationPos = origin.offset(2, 0, 0);
        level.setBlockAndUpdate(builderPos, TinkerTables.partBuilder.get().defaultBlockState());
        level.setBlockAndUpdate(stationPos, TinkerTables.tinkerStation.get().defaultBlockState());
        var builder = (PartBuilderBlockEntity) level.getBlockEntity(builderPos);
        var recipe = (ToolBuildingRecipe) level.getRecipeManager()
          .byKey(ResourceLocation.parse("tconstruct:tools/building/pickaxe")).orElseThrow().value();
        parts = recipe.getToolParts().stream().map(part -> part.asItem()).toList();
        require(parts.size() == 3, "Actual pickaxe recipe lost its parts");
        builder.setItem(PartBuilderBlockEntity.MATERIAL_SLOT, new ItemStack(Items.OAK_PLANKS, 64));
        builder.setItem(PartBuilderBlockEntity.PATTERN_SLOT, new ItemStack(TinkerTables.pattern.get(), 3));
        tablesPlaced = true;
      });
      return;
    }
    if (step == 2) {
      if (!builderOpenRequested) {
        if (!tablesPlaced || !(minecraft.level.getBlockEntity(builderPos) instanceof PartBuilderBlockEntity) ||
            !(minecraft.level.getBlockEntity(stationPos) instanceof TinkerStationBlockEntity)) return;
        builderOpenRequested = true;
        var server = minecraft.getSingleplayerServer();
        var uuid = minecraft.player.getUUID();
        serverAction(() -> {
          var player = server.getPlayerList().getPlayer(uuid);
          var builder = (PartBuilderBlockEntity) player.serverLevel().getBlockEntity(builderPos);
          player.openMenu(builder, buffer -> buffer.writeBlockPos(builderPos));
        });
        return;
      }
      if (!(minecraft.screen instanceof PartBuilderScreen) ||
          !(minecraft.player.containerMenu instanceof PartBuilderContainerMenu menu) || ticks < 20) return;
      var builder = (PartBuilderBlockEntity) minecraft.level.getBlockEntity(builderPos);
      require(builder != null && menu.getOutputSlot() != null, "Actual Part Builder client menu lost its tile");
      int button = builder.getSortedButtons().indexOf(Pattern.fromItem(parts.get(partIndex)));
      require(button >= 0, "Actual client did not receive the required part pattern");
      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
      advance(3);
      return;
    }
    if (step == 3) {
      var menu = (PartBuilderContainerMenu) minecraft.player.containerMenu;
      ItemStack output = menu.getOutputSlot().getItem();
      if (ticks < 20 || output.isEmpty() || output.getItem() != parts.get(partIndex)) return;
      require(new MaterialId("tconstruct:wood").equals(IMaterialItem.getMaterialFromStack(output)),
        "Actual client part output lost its material");
      geometry(minecraft, output);
      var builder = (PartBuilderBlockEntity) minecraft.level.getBlockEntity(builderPos);
      usedPlanks += builder.getPartRecipe().getItemsUsed(builder.getInventoryWrapper());
      int destination = menu.slots.stream().filter(slot -> slot.container == minecraft.player.getInventory() &&
        slot.getContainerSlot() < 36 && !slot.hasItem()).findFirst().orElseThrow().index;
      minecraft.gameMode.handleInventoryMouseClick(menu.containerId, menu.getOutputSlot().index, 0,
        ClickType.PICKUP, minecraft.player);
      minecraft.gameMode.handleInventoryMouseClick(menu.containerId, destination, 0,
        ClickType.PICKUP, minecraft.player);
      advance(4);
      return;
    }
    if (step == 4) {
      if (ticks < 20) return;
      require(minecraft.player.containerMenu.getCarried().isEmpty(), "Actual part pickup left a carried stack");
      require(minecraft.player.getInventory().items.stream().anyMatch(stack ->
        stack.getItem() == parts.get(partIndex)), "Actual crafted part did not reach the client inventory");
      if (++partIndex < parts.size()) {
        advance(2);
        return;
      }
      minecraft.player.closeContainer();
      stationOpenPending = false;
      advance(5);
      return;
    }
    if (step == 5) {
      if (!(minecraft.screen instanceof TinkerStationScreen screen) ||
          !(minecraft.player.containerMenu instanceof TinkerStationContainerMenu)) {
        if (!stationOpenPending && ticks % 10 == 0) {
          stationOpenPending = true;
          var server = minecraft.getSingleplayerServer();
          var uuid = minecraft.player.getUUID();
          serverAction(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player.containerMenu != player.inventoryMenu) {
              stationOpenPending = false;
              return;
            }
            var builder = (PartBuilderBlockEntity) player.serverLevel().getBlockEntity(builderPos);
            require(builder.getItem(0).getCount() == 64 - usedPlanks && builder.getItem(1).isEmpty(),
              "Native part packets did not consume exact materials and patterns on the actual server");
            for (Item part : parts) require(player.getInventory().items.stream().anyMatch(stack ->
              stack.getItem() == part && new MaterialId("tconstruct:wood").equals(IMaterialItem.getMaterialFromStack(stack))),
              "Actual server inventory did not retain a crafted material part");
            var station = (TinkerStationBlockEntity) player.serverLevel().getBlockEntity(stationPos);
            player.openMenu(station, buffer -> buffer.writeBlockPos(stationPos));
          });
        }
        return;
      }
      if (ticks < 20) return;
      screen.onToolSelection(StationSlotLayoutLoader.getInstance().get(ResourceLocation.parse("tconstruct:pickaxe")));
      advance(6);
      return;
    }
    if (step == 6) {
      if (ticks < 20) return;
      var menu = (TinkerStationContainerMenu) minecraft.player.containerMenu;
      for (int i = 0; i < parts.size(); i++) {
        Item part = parts.get(i);
        int source = menu.slots.stream().filter(slot -> slot.container == minecraft.player.getInventory() &&
          slot.getItem().getItem() == part).findFirst().orElseThrow().index;
        var destination = menu.getInputSlots().get(i);
        require(destination.isActive(), "Actual station selection packet did not activate a construction slot");
        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, source, 0, ClickType.PICKUP, minecraft.player);
        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, destination.index, 0, ClickType.PICKUP, minecraft.player);
      }
      advance(7);
      return;
    }
    if (step == 7) {
      if (ticks < 20) return;
      var station = (TinkerStationBlockEntity) minecraft.level.getBlockEntity(stationPos);
      var menu = (TinkerStationContainerMenu) minecraft.player.containerMenu;
      int outputSlot = station.getContainerSize();
      ItemStack output = menu.slots.get(outputSlot).getItem();
      if (output.isEmpty()) return;
      require(output.getItem() == TinkerTools.pickaxe.get(), "Actual client station showed the wrong construction result");
      var tool = ToolStack.from(output);
      require(tool.getMaterials().size() == 3 && tool.getCurrentDurability() > 0 && !tool.isBroken(),
        "Actual client station preview lost usable tool data");
      geometry(minecraft, output);
      minecraft.gameMode.handleInventoryMouseClick(menu.containerId, outputSlot, 0, ClickType.QUICK_MOVE, minecraft.player);
      advance(8);
      return;
    }
    if (step == 8) {
      if (ticks < 20) return;
      ItemStack output = minecraft.player.getInventory().items.stream()
        .filter(stack -> stack.getItem() == TinkerTools.pickaxe.get()).findFirst().orElse(null);
      if (output == null) return;
      geometry(minecraft, output);
      if (!serverReady) {
        if (serverValidationRequested) return;
        serverValidationRequested = true;
        var server = minecraft.getSingleplayerServer();
        var uuid = minecraft.player.getUUID();
        serverAction(() -> {
          var player = server.getPlayerList().getPlayer(uuid);
          var station = (TinkerStationBlockEntity) player.serverLevel().getBlockEntity(stationPos);
          for (int i = 1; i <= 3; i++) require(station.getItem(i).isEmpty(),
            "Actual station packets did not consume all construction parts");
          ItemStack built = player.getInventory().items.stream()
            .filter(stack -> stack.getItem() == TinkerTools.pickaxe.get()).findFirst().orElseThrow();
          require(ToolStack.from(built).getMaterials().size() == 3 && built.getDamageValue() == 0,
            "Actual server did not retain the constructed usable tool");
          serverReady = true;
        });
        return;
      }
      TConstruct.LOG.info("TiC actual rendered Part Builder and Station construction packets PASS");
      minecraft.player.closeContainer();
      serverReady = false;
      stationOpenPending = false;
      advance(9);
      return;
    }
    if (step == 9 || step == 11 || step == 13) {
      if (!(minecraft.screen instanceof TinkerStationScreen)) {
        reopenStation(minecraft, (step - 9) / 2);
        return;
      }
      if (screenTicks < 20) return;
      ItemStack output = stationOutput(minecraft);
      if (output.isEmpty()) return;
      var tool = ToolStack.from(output);
      require(tool.getMaterials().size() == 3 && tool.getCurrentDurability() > 0,
        "Actual station client result lost usable material tool data");
      if (step == 9) {
        require(output.getDamageValue() == 0, "Actual repair screen did not show a repaired tool");
      } else if (step == 11) {
        require(tool.getUpgrades().getEntry(HASTE).getAmount(0) == 9 &&
          tool.getFreeSlots(SlotType.UPGRADE) == baseUpgradeSlots - 1,
          "Actual partial modifier screen lost its amount or slot");
      } else {
        require(tool.getUpgrades().getEntry(HASTE).getLevel() == 1 &&
          tool.getUpgrades().getEntry(HASTE).getAmount(45) == 45 &&
          tool.getFreeSlots(SlotType.UPGRADE) == baseUpgradeSlots - 1 &&
          tool.getStats().get(ToolStats.MINING_SPEED) > baseMiningSpeed,
          "Actual resumed modifier screen lost native amount, stats or slots");
      }
      geometry(minecraft, output);
      takeStationOutput(minecraft);
      advance(step + 1);
      return;
    }
    if (step == 10 || step == 12) {
      if (ticks < 20 || inventoryTool(minecraft) == null) return;
      minecraft.player.closeContainer();
      stationOpenPending = false;
      advance(step + 1);
      return;
    }
    if (step == 14) {
      if (ticks < 20 || inventoryTool(minecraft) == null) return;
      var finalTool = ToolStack.from(inventoryTool(minecraft));
      require(finalTool.getUpgrades().getEntry(HASTE).getLevel() == 1 &&
        finalTool.getUpgrades().getEntry(HASTE).getAmount(45) == 45 &&
        finalTool.getFreeSlots(SlotType.UPGRADE) == baseUpgradeSlots - 1,
        "Actual final client inventory lost its completed modifier");
      var server = minecraft.getSingleplayerServer();
      var uuid = minecraft.player.getUUID();
      advance(15);
      serverAction(() -> {
        var player = server.getPlayerList().getPlayer(uuid);
        var station = (TinkerStationBlockEntity) player.serverLevel().getBlockEntity(stationPos);
        require(station.getItem(0).isEmpty() && station.getItem(1).isEmpty(),
          "Actual final modifier packets did not consume their remaining inputs");
        ItemStack output = player.getInventory().items.stream()
          .filter(stack -> stack.getItem() == TinkerTools.pickaxe.get()).findFirst().orElseThrow();
        var completed = ToolStack.from(output);
        require(completed.getUpgrades().getEntry(HASTE).getLevel() == 1 &&
          completed.getUpgrades().getEntry(HASTE).getAmount(45) == 45 &&
          completed.getFreeSlots(SlotType.UPGRADE) == baseUpgradeSlots - 1 &&
          completed.getStats().get(ToolStats.MINING_SPEED) > baseMiningSpeed,
          "Actual final server inventory lost its persisted incremental modifier");
        serverReady = true;
      });
      return;
    }
    if (step == 15 && serverReady) {
      TConstruct.LOG.info("TiC actual client construction repair and persisted modifier packets PASS");
      advance(16);
      minecraft.player.closeContainer();
      return;
    }
    if (step == 16 && ArmorRenderingCheck.tick(minecraft)) {
      advance(17);
      return;
    }
    if (step == 17 && WorldEntityRenderingCheck.tick(minecraft)) {
      advance(18);
      return;
    }
    if (step == 18 && AddonClientCheck.tick(minecraft)) {
      geometry(minecraft, minecraft.player.getInventory().getItem(8));
      TConstruct.LOG.info("TiC independent addon native inventory synchronization and material rendering PASS");
      advance(19);
      minecraft.stop();
    }
  }
}
