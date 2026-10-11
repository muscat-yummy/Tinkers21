package slimeknights.tconstruct.port;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.book.TinkerBook;

/** Loaded production book icons must resolve real stacks, preserving their display variants. */
public final class BookIconRenderingCheck {
  private static boolean started, complete;
  private static String language;
  private static RuntimeException failure;
  public static boolean tick(Minecraft minecraft) {
    if (failure != null) throw failure;
    if (complete) {
      minecraft.setScreen(null);
      minecraft.getLanguageManager().setSelected(language);
      return true;
    }
    if (!started) {
      started = true;
      language = minecraft.getLanguageManager().getSelected();
      minecraft.getLanguageManager().setSelected("ja_jp");
      int icons = 0;
      for (BookData book : new BookData[]{TinkerBook.MATERIALS_AND_YOU, TinkerBook.PUNY_SMELTING,
          TinkerBook.MIGHTY_SMELTING, TinkerBook.FANTASTIC_FOUNDRY, TinkerBook.TINKERS_GADGETRY, TinkerBook.ENCYCLOPEDIA}) {
        book.reset();
        book.load();
        for (var section : book.sections) {
          if (section.icon.item == null) continue;
          var stacks = section.icon.item.getItems();
          require(stacks != null && !stacks.isEmpty(), "Empty section icon: " + section.name);
          for (var stack : stacks) require(!stack.isEmpty() && !stack.is(Items.BARRIER), "Unresolved section icon: " + section.name + " " + stack.getHoverName().getString());
          icons++;
        }
      }
      var book = TinkerBook.ENCYCLOPEDIA;
      require(book.findSection("materials_slime").getTitle().equals("スライムスーツ用素材"), "Missing Japanese slimesuit index translation");
      for (String name : new String[]{"tools", "materials_harvest", "upgrades", "defense", "slotless", "abilities"}) {
        var stack = book.findSection(name).icon.item.getItems().get(0);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (name.equals("tools")) require(data.getList("tic_materials", 8).size() == 3 && data.getBoolean("tic_display"), "Tool icon lost its three display materials");
        else if (name.equals("materials_harvest")) require(data.getString("Material").equals("tconstruct:ui_render#extra_large"), "Part icon lost its material variant");
        else require(data.getString("slot").equals(name), "Creative-slot icon lost its slot kind: " + name);
      }
      TConstruct.LOG.info("TiC actual loaded six-book section icons resolved without barriers count {} PASS", icons);
      minecraft.setScreen(new IndexScreen(book));
    }
    return false;
  }
  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
  private static final class IndexScreen extends BookScreen {
    IndexScreen(BookData book) {
      super(Component.literal("Japanese encyclopedia icon regression"), book, "index.page1", null, null);
      require(getPage_() >= 0, "Actual encyclopedia index page not found");
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (complete || failure != null) return;
      try {
        super.render(graphics, -100, -100, partialTick);
        graphics.flush();
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
          image.writeToFile(Path.of("book-icon-gui-ja-jp.png"));
        }
        TConstruct.LOG.info("TiC actual six loaded book section icons legacy material tool slot components Japanese encyclopedia index native rendering PASS");
        complete = true;
      } catch (Exception problem) {
        failure = new IllegalStateException("Actual loaded book icon rendering failed", problem);
      }
    }
  }
}
