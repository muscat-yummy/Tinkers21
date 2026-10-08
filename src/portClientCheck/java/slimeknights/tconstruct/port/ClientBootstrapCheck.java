package slimeknights.tconstruct.port;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
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
  private static int partIndex;
  private static int usedPlanks;
  private static volatile BlockPos builderPos;
  private static volatile BlockPos stationPos;
  private static volatile List<Item> parts = List.of();
  private static volatile boolean serverReady;
  private static boolean serverValidationRequested;
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

  @SubscribeEvent
  public static void clientTick(ClientTickEvent.Post event) {
    if (step == 9) return;
    if (serverFailure != null) throw serverFailure;
    Minecraft minecraft = Minecraft.getInstance();
    if (++totalTicks > 9000) {
      throw new IllegalStateException("Actual construction timed out at step " + step + ": " +
        (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName()));
    }
    ticks++;
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
      minecraft.options.simulationDistance().set(2);
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
        player.openMenu(builder, buffer -> buffer.writeBlockPos(builderPos));
      });
      return;
    }
    if (step == 2) {
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
      var server = minecraft.getSingleplayerServer();
      var uuid = minecraft.player.getUUID();
      advance(5);
      serverAction(() -> {
        var player = server.getPlayerList().getPlayer(uuid);
        var builder = (PartBuilderBlockEntity) player.serverLevel().getBlockEntity(builderPos);
        require(builder.getItem(0).getCount() == 64 - usedPlanks && builder.getItem(1).isEmpty(),
          "Native part packets did not consume exact materials and patterns on the actual server");
        for (Item part : parts) require(player.getInventory().items.stream().anyMatch(stack ->
          stack.getItem() == part && new MaterialId("tconstruct:wood").equals(IMaterialItem.getMaterialFromStack(stack))),
          "Actual server inventory did not retain a crafted material part");
        var station = (TinkerStationBlockEntity) player.serverLevel().getBlockEntity(stationPos);
        player.openMenu(station, buffer -> buffer.writeBlockPos(stationPos));
      });
      return;
    }
    if (step == 5) {
      if (!(minecraft.screen instanceof TinkerStationScreen screen) ||
          !(minecraft.player.containerMenu instanceof TinkerStationContainerMenu) || ticks < 20) return;
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
      advance(9);
      minecraft.player.closeContainer();
      minecraft.stop();
    }
  }
}
