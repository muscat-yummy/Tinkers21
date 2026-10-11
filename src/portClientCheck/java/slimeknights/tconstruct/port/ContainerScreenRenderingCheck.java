package slimeknights.tconstruct.port;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.screen.BackgroundContainerScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.menu.HeatingStructureContainerMenu;

/** Full production screen dispatch, with a controlled recreation of the erroneous legacy call. */
public final class ContainerScreenRenderingCheck {
  private static boolean started;
  private static boolean complete;
  private static RuntimeException failure;
  public static boolean tick(Minecraft minecraft) {
    if (failure != null) throw failure;
    if (complete) { minecraft.setScreen(null); return true; }
    if (!started) { started = true; minecraft.setScreen(new CheckScreen()); }
    return false;
  }
  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
  private static final class Controller extends FoundryBlockEntity {
    private final MultitankFuelModule shownFuel = new MultitankFuelModule(this, List::of) {
      @Override public FuelInfo getFuelInfo() {
        return FuelInfo.of(new FluidStack(Fluids.LAVA, 1000), 1000, 1000);
      }
    };
    Controller() {
      super(BlockPos.ZERO, TinkerSmeltery.foundryController.get().defaultBlockState());
      getMeltingInventory().resize(28, stack -> {});
      getTank().setCapacity(1000);
      getTank().setFluids(List.of(new FluidStack(TinkerFluids.moltenIron.get(), 500)));
    }
    @Override public MultitankFuelModule getFuelModule() { return shownFuel; }
  }
  private static final class FoundryScreen extends HeatingStructureScreen {
    int backgroundCalls;
    boolean legacy;
    FoundryScreen(Minecraft minecraft) {
      super(new HeatingStructureContainerMenu(0, minecraft.player.getInventory(), new Controller()),
        minecraft.player.getInventory(), Component.literal("Foundry GUI regression"));
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      backgroundCalls++;
      super.renderBg(graphics, partialTick, mouseX, mouseY);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      backgroundCalls = 0;
      // Recreate the exact removed call, before MultiModuleScreen swaps expanded bounds to main bounds.
      if (legacy) renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
    }
  }
  private static final class GenericScreen extends BackgroundContainerScreen<ChestMenu> {
    int backgroundCalls;
    GenericScreen(Minecraft minecraft) {
      super(ChestMenu.threeRows(0, minecraft.player.getInventory()), minecraft.player.getInventory(),
        Component.literal("Generic GUI regression"), 166, HeatingStructureScreen.BACKGROUND);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      backgroundCalls++;
      super.renderBg(graphics, partialTick, mouseX, mouseY);
    }
  }
  private static final class CheckScreen extends Screen {
    FoundryScreen foundry;
    int frame;
    int[] legacyPixels;
    CheckScreen() { super(Component.literal("Container dispatch regression")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
      foundry = new FoundryScreen(minecraft);
      foundry.init(minecraft, width, height);
      require(foundry.leftPos < foundry.cornerX, "Fixture has no left input panel offset");
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (complete || failure != null) return;
      try {
        foundry.legacy = frame == 0;
        foundry.render(graphics, -100, -100, partialTick);
        graphics.flush();
        require(foundry.backgroundCalls == (frame == 0 ? 2 : 1),
          "Full Foundry background was dispatched " + foundry.backgroundCalls + " times at frame " + frame);
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
          image.writeToFile(Path.of(frame == 0 ? "foundry-gui-legacy.png" : "foundry-gui-fixed.png"));
          double scale = minecraft.getWindow().getGuiScale();
          // Original duplicate iron crosses this empty input-panel band; no items hide the defect.
          int[] pixels = new int[60];
          for (int i = 0; i < pixels.length; i++) {
            int x = foundry.leftPos + 10 + i;
            int y = foundry.cornerY + 110;
            pixels[i] = image.getPixelRGBA((int)((x + .5) * scale), (int)((y + .5) * scale));
          }
          if (frame == 0) {
            legacyPixels = pixels;
            TConstruct.LOG.info("TiC actual full Foundry legacy two-background duplicate-fluid reproduction PASS");
          } else {
            int changed = 0;
            for (int i = 0; i < pixels.length; i++) if (pixels[i] != legacyPixels[i]) changed++;
            require(changed > 30, "Removing double dispatch did not clear the input-panel fluid ghost: " + changed);
            // Lava remains in its proper right-hand gauge, while its shifted duplicate is gone.
            int right = image.getPixelRGBA((int)((foundry.cornerX + 160.5) * scale), (int)((foundry.cornerY + 50.5) * scale));
            int ghost = image.getPixelRGBA((int)((foundry.leftPos + 160.5) * scale), (int)((foundry.cornerY + 50.5) * scale));
            require(right != ghost, "Shifted duplicate lava still matches the correct fuel gauge");
          }
        }
        if (frame++ == 0) return;
        GenericScreen generic = new GenericScreen(minecraft);
        generic.init(minecraft, width, height);
        generic.render(graphics, -100, -100, partialTick);
        graphics.flush();
        require(generic.backgroundCalls == 1, "Generic container also double-draws its background");
        TConstruct.LOG.info("TiC actual full Foundry and generic container single background dispatch no shifted lava or input-panel iron PASS");
        complete = true;
      } catch (Exception problem) {
        failure = new IllegalStateException("Actual full container-screen regression failed", problem);
      }
    }
  }
}
