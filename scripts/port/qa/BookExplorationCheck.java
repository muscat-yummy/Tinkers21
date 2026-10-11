package slimeknights.tconstruct.port;

import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.element.IngredientData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.book.TinkerBook;

/** Exploratory real-client sweep; findings are retained instead of aborting on the first bad page. */
public final class BookExplorationCheck {
  private static final BookData[] BOOKS = {TinkerBook.MATERIALS_AND_YOU, TinkerBook.PUNY_SMELTING,
    TinkerBook.MIGHTY_SMELTING, TinkerBook.FANTASTIC_FOUNDRY, TinkerBook.TINKERS_GADGETRY, TinkerBook.ENCYCLOPEDIA};
  private static final String[] NAMES = {"materials", "puny", "mighty", "foundry", "gadgetry", "encyclopedia"};
  private static final String[] LANGUAGES = {"ja_jp", "en_us"};
  private static final ArrayList<String> findings = new ArrayList<>();
  private static final ArrayList<String> coverage = new ArrayList<>();
  private static int scenario, rendered, pages;
  private static String originalLanguage;
  private static SweepScreen screen;

  public static boolean tick(Minecraft minecraft) {
    if (originalLanguage == null) originalLanguage = minecraft.getLanguageManager().getSelected();
    if (screen != null && !screen.done) return false;
    if (scenario == BOOKS.length * LANGUAGES.length) {
      minecraft.setScreen(null);
      minecraft.getLanguageManager().setSelected(originalLanguage);
      String summary = "Native book exploration completed: languages=2 books=12 loadedPages=" + pages
        + " renderedSpreads=" + rendered + " findings=" + findings.size();
      try {
        Files.writeString(Path.of("book-exploration-report.txt"), summary + "\n" + String.join("\n", coverage)
          + "\nFINDINGS\n" + String.join("\n", findings) + "\n");
      } catch (Exception e) { throw new IllegalStateException("Cannot save exploration evidence", e); }
      TConstruct.LOG.info(summary);
      for (String finding : findings) TConstruct.LOG.info("BOOK_EXPLORATION_FINDING {}", finding);
      TConstruct.LOG.info("TiC actual all six books Japanese English every spread exploration COMPLETE");
      return true;
    }
    int index = scenario % BOOKS.length;
    String language = LANGUAGES[scenario / BOOKS.length];
    String label = language + "/" + NAMES[index];
    minecraft.getLanguageManager().setSelected(language);
    BookData book = BOOKS[index];
    book.reset();
    book.load();
    int count = 0;
    for (var section : book.sections) for (var page : section.pages) {
      count++;
      String id = label + "/" + section.name + "." + page.name;
      if (page.content == null) findings.add(id + " NULL_CONTENT");
      else if (page.content.getClass().getSimpleName().equals("ContentError")) {
        findings.add(id + " CONTENT_ERROR " + describe(page.content));
      } else scan(page.content, id, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    pages += count;
    coverage.add(label + " loadedPages=" + count + " spreads=" + book.getFullPageCount(null));
    TConstruct.LOG.info("BOOK_EXPLORATION_START {} loadedPages={} spreads={}", label, count, book.getFullPageCount(null));
    screen = new SweepScreen(book, label);
    minecraft.setScreen(screen);
    scenario++;
    return false;
  }

  private static String describe(Object content) {
    StringBuilder out = new StringBuilder();
    for (var field : content.getClass().getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())) continue;
      try { field.setAccessible(true); out.append(field.getName()).append('=').append(field.get(content)).append(' '); }
      catch (ReflectiveOperationException ignored) { }
    }
    return out.toString();
  }

  private static void scan(Object value, String id, int depth, Set<Object> visited) {
    if (value == null || depth > 5 || !visited.add(value)) return;
    if (value instanceof IngredientData ingredient) {
      var stacks = ingredient.getItems();
      if (stacks == null) findings.add(id + " UNLOADED_INGREDIENT");
      else for (var stack : stacks) if (stack.is(Items.BARRIER)) findings.add(id + " BARRIER_INGREDIENT " + describe(ingredient));
      return;
    }
    if (value instanceof Iterable<?> list) { for (Object element : list) scan(element, id, depth + 1, visited); return; }
    if (value instanceof Object[] array) { for (Object element : array) scan(element, id, depth + 1, visited); return; }
    if (!value.getClass().getName().startsWith("slimeknights.")) return;
    for (var field : value.getClass().getFields()) {
      if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
      try { scan(field.get(value), id + "/" + field.getName(), depth + 1, visited); }
      catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
  }

  private static final class SweepScreen extends BookScreen {
    private final String label;
    private int next = -1;
    private boolean done;
    SweepScreen(BookData book, String label) {
      super(Component.literal(label), book, "", null, null);
      this.label = label;
      // Null is the supported all-sections view, so progression locks cannot hide untested pages.
      this.advancementCache = null;
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (done) return;
      int current = next++;
      if (current >= book.getFullPageCount(advancementCache)) { done = true; return; }
      try {
        _setPage(current);
        super.render(graphics, -100, -100, partialTick);
        graphics.flush();
        rendered++;
        if (current == 1) try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
          image.writeToFile(Path.of("book-exploration-gui-" + label.replace('/', '-') + ".png"));
        }
      } catch (Exception problem) {
        findings.add(label + "/spread=" + current + " RENDER_EXCEPTION " + problem);
        TConstruct.LOG.info("BOOK_EXPLORATION_RENDER_EXCEPTION {} spread={} {}", label, current, problem.toString());
      }
    }
  }
}
