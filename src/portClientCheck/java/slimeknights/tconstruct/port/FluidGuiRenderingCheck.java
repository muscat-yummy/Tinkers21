package slimeknights.tconstruct.port;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiSmelteryTank;

/** Framebuffer checks of production GUI draws, including queued geometry and rectangular tanks. */
public final class FluidGuiRenderingCheck {
  private static boolean started;
  private static boolean complete;
  private static RuntimeException failure;
  private static final int BACKGROUND = 0xFF203040;

  public static boolean tick(Minecraft minecraft) {
    if (failure != null) throw failure;
    if (complete) {
      minecraft.setScreen(null);
      return true;
    }
    if (!started) {
      started = true;
      minecraft.setScreen(new CheckScreen());
    }
    return false;
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  private static final class Anchor extends AbstractContainerScreen<ChestMenu> {
    Anchor(Minecraft minecraft) {
      super(ChestMenu.threeRows(0, minecraft.player.getInventory()), minecraft.player.getInventory(), Component.empty());
      init(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
      leftPos = 0;
      topPos = 0;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {}
  }

  private static final class CheckScreen extends Screen {
    private int frames;
    CheckScreen() { super(Component.literal("Fluid GUI regression")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (complete || failure != null) return;
      try {
        Anchor anchor = new Anchor(minecraft);
        graphics.fill(0, 0, width, height, BACKGROUND);
        FluidStack lava = new FluidStack(Fluids.LAVA, 1000);
        FluidStack iron = new FluidStack(TinkerFluids.moltenIron.get(), 1000);
        // Deliberately queue a rectangle immediately before changing fluid atlas/tint.
        graphics.fill(4, 4, 12, 12, 0xFF6987A5);
        GuiUtil.renderFluidTank(graphics, anchor, lava, 1000, 20, 20, 16, 90, 100);
        GuiUtil.renderFluidTank(graphics, anchor, iron, 1000, 50, 20, 53, 37, 100);
        GuiUtil.renderFluidTank(graphics, anchor, iron, 500, 1000, 120, 20, 19, 53, 100);
        GuiUtil.renderFluidTank(graphics, anchor, lava, 0, 1000, 150, 20, 16, 90, 100);
        GuiUtil.renderTiledFluid(graphics, anchor, lava, 150, 20, 0, 90, 100);
        var controller = new SmelteryBlockEntity(BlockPos.ZERO, TinkerSmeltery.smelteryController.get().defaultBlockState());
        controller.getTank().setCapacity(2000);
        controller.getTank().setFluids(List.of(iron, lava));
        new GuiSmelteryTank(anchor, controller.getTank(), 180, 20, 23, 53,
          TConstruct.getResource("smeltery")).renderFluids(graphics);
        graphics.flush();
        // Let the real screen projection settle and permit animated sprites to advance.
        if (++frames < 3) return;
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
          image.writeToFile(Path.of("fluid-gui-check.png"));
          double scale = minecraft.getWindow().getGuiScale();
          for (int y = 2; y < Math.min(height - 2, 125); y++) {
            for (int x = 2; x < Math.min(width - 2, 215); x++) {
              boolean fluid = in(x,y,20,20,16,90) || in(x,y,50,20,53,37) ||
                in(x,y,120,47,19,26) || in(x,y,180,20,23,53);
              int expected = in(x,y,4,4,8,8) ? 0xFFA58769 : 0xFF403020; // NativeImage ABGR
              int pixel = image.getPixelRGBA((int)((x + 0.5) * scale), (int)((y + 0.5) * scale));
              if (!fluid) require(pixel == expected, "Fluid GUI escaped its bounds or corrupted queued geometry at " + x + "," + y + ": " + Integer.toHexString(pixel));
              else require(pixel != 0xFF403020, "Fluid GUI failed to fill its tank at " + x + "," + y);
            }
          }
          int lavaPixel = image.getPixelRGBA((int)(28.5 * scale), (int)(45.5 * scale));
          int ironPixel = image.getPixelRGBA((int)(58.5 * scale), (int)(45.5 * scale));
          require(lavaPixel != ironPixel, "Molten iron reused the lava sprite/tint");
        }
        TConstruct.LOG.info("TiC actual fluid GUI framebuffer bounds queued geometry lava iron partial empty and rectangular tanks PASS");
        complete = true;
      } catch (Exception problem) {
        failure = new IllegalStateException("Actual fluid GUI framebuffer verification failed", problem);
      }
    }
    private static boolean in(int x, int y, int left, int top, int width, int height) {
      return x >= left && x < left + width && y >= top && y < top + height;
    }
  }
}
