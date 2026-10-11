package slimeknights.tconstruct.port;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.book.TinkerBook;

/** Real loaded book covers/pages, native blur and framebuffer evidence of the old ordering. */
public final class BookScreenRenderingCheck {
  private static int scenario;
  private static CheckBook current;
  private static RuntimeException failure;
  public static boolean tick(Minecraft minecraft) {
    if (failure != null) throw failure;
    if (current != null && !current.complete) return false;
    if (scenario == 4) {
      minecraft.setScreen(null);
      TConstruct.LOG.info("TiC actual Materials and You Puny Smelting cover pages native blur ordering sharp framebuffer and arrows PASS");
      return true;
    }
    BookData book = scenario < 2 ? TinkerBook.MATERIALS_AND_YOU : TinkerBook.PUNY_SMELTING;
    current = new CheckBook(book, scenario++);
    minecraft.setScreen(current);
    return false;
  }
  private static final class CheckBook extends BookScreen {
    final int scenario;
    int frame;
    int backgroundCalls;
    long legacyEdges;
    boolean complete;
    CheckBook(BookData book, int scenario) {
      super(Component.literal("Actual book blur regression"), book, "", null, null);
      this.scenario = scenario;
      if (scenario % 2 == 1) openPage(1);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      // Frame zero recreates the old late Screen.render background call exactly.
      if (frame != 0) { backgroundCalls++; super.renderBackground(graphics, mouseX, mouseY, partialTick); }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (complete || failure != null) return;
      try {
        backgroundCalls = 0;
        super.render(graphics, -100, -100, partialTick);
        if (frame == 0) {
          graphics.flush();
          backgroundCalls++;
          super.renderBackground(graphics, -100, -100, partialTick);
          for (Renderable widget : renderables) widget.render(graphics, -100, -100, partialTick);
        }
        graphics.flush();
        if (backgroundCalls != 1) throw new IllegalStateException("Book background dispatch count " + backgroundCalls);
        if (renderables.isEmpty()) throw new IllegalStateException("Book navigation arrows missing");
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
          image.writeToFile(Path.of("book-gui-" + scenario + (frame == 0 ? "-legacy.png" : "-fixed.png")));
          double scale = minecraft.getWindow().getGuiScale();
          // Interior cover text/texture or the loaded first right-hand page, excluding the changing world.
          int x0 = width / 2 + (scenario % 2 == 0 ? -90 : 10);
          int x1 = width / 2 + (scenario % 2 == 0 ? 90 : 180);
          int y0 = height / 2 - 85;
          int y1 = height / 2 + 80;
          long edges = 0;
          for (int y = (int)(y0 * scale); y < (int)(y1 * scale); y++) {
            for (int x = (int)(x0 * scale); x < (int)(x1 * scale) - 1; x++) {
              int a = image.getPixelRGBA(x, y), b = image.getPixelRGBA(x + 1, y);
              for (int shift = 0; shift < 24; shift += 8) edges += Math.abs(((a >>> shift) & 255) - ((b >>> shift) & 255));
            }
          }
          if (frame == 0) legacyEdges = edges;
          else {
            if (edges <= legacyEdges * 1.10) throw new IllegalStateException("Loaded book still blurred: scenario=" + scenario + " old=" + legacyEdges + " fixed=" + edges);
            TConstruct.LOG.info("TiC actual book framebuffer scenario {} late-blur edges {} corrected edges {} PASS", scenario, legacyEdges, edges);
            complete = true;
          }
        }
        frame++;
      } catch (Exception problem) {
        failure = new IllegalStateException("Actual book screen blur regression failed", problem);
      }
    }
  }
}
