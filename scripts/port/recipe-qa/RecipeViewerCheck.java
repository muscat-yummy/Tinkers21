package slimeknights.tconstruct.port;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.screen.WidgetGroup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.TConstruct;

/** CI-only: exercise actual registered EMI/JEMI recipe widget construction and rendering. */
public final class RecipeViewerCheck {
  private static boolean started, complete;
  private static RuntimeException failure;
  private static int waiting;
  public static boolean tick(Minecraft mc) {
    if (failure != null) throw failure;
    if (complete) { mc.setScreen(null); return true; }
    if (!started) {
      var manager = EmiApi.getRecipeManager();
      if (manager == null || manager.getRecipes().isEmpty()) {
        if (++waiting > 600) throw new IllegalStateException("EMI recipe reload did not complete");
        return false;
      }
      List<EmiRecipe> recipes = manager.getRecipes().stream().filter(r ->
        r.getCategory().getId().getNamespace().equals("tconstruct") ||
        r.getId() != null && (r.getId().getNamespace().equals("tconstruct") ||
        r.getId().getNamespace().equals("jei") && r.getId().getPath().startsWith("/tconstruct/"))).toList();
      if (recipes.isEmpty()) throw new IllegalStateException("No TiC recipes registered in EMI");
      started = true;
      mc.setScreen(new CheckScreen(recipes));
    }
    return false;
  }
  private static final class CheckScreen extends Screen {
    private final List<EmiRecipe> recipes;
    private final List<String> findings = new ArrayList<>();
    private final Map<String,Integer> counts = new TreeMap<>();
    private int index, jeiIndex;
    private final List<mezz.jei.api.gui.IRecipeLayoutDrawable<?>> nativeLayouts = new ArrayList<>();
    private final java.util.Set<String> logged = new java.util.HashSet<>();
    @SuppressWarnings({"rawtypes", "unchecked"})
    CheckScreen(List<EmiRecipe> recipes) {
      super(Component.literal("Actual JEI and EMI recipe audit")); this.recipes=recipes;
      var runtime=dev.emi.emi.jemi.JemiPlugin.runtime;
      for (var category : runtime.getRecipeManager().createRecipeCategoryLookup().get().toList()) {
        if (!category.getRecipeType().getUid().getNamespace().equals("tconstruct")) continue;
        var lookup=runtime.getRecipeManager().createRecipeLookup(category.getRecipeType());
        for (Object recipe : lookup.get().toList()) {
          var layout=runtime.getRecipeManager().createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory)category,recipe,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
          if (layout.isEmpty()) throw new IllegalStateException("Native JEI rejected layout: "+category.getRecipeType().getUid()+" "+recipe);
          nativeLayouts.add((mezz.jei.api.gui.IRecipeLayoutDrawable<?>)layout.get());
        }
      }
      if(nativeLayouts.isEmpty()) throw new IllegalStateException("No native JEI layouts");
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.render(g,mouseX,mouseY,delta);
      for (int batch=0; batch<8 && jeiIndex<nativeLayouts.size(); batch++,jeiIndex++) {
        var layout=nativeLayouts.get(jeiIndex);
        layout.setPosition(25,40);
        layout.tick();
        layout.drawRecipe(g,29,44);
        layout.drawOverlays(g,29,44);
        g.flush();
      }
      if(jeiIndex<nativeLayouts.size()) return;
      for (int batch=0; batch<8 && index<recipes.size(); batch++,index++) {
        EmiRecipe recipe=recipes.get(index);
        String category=recipe.getCategory().getId().toString();
        String id=String.valueOf(recipe.getId());
        g.pose().pushPose();
        try {
          g.pose().translate(25,40,0);
          WidgetGroup group=new WidgetGroup(recipe,0,0,recipe.getDisplayWidth(),recipe.getDisplayHeight());
          recipe.addWidgets(group);
          if (group.widgets.size()<2) throw new IllegalStateException("No recipe widgets");
          for (var widget : group.widgets) {
            widget.render(g,4,4,delta);
            widget.getTooltip(4,4);
          }
          g.flush();
          counts.merge(category,1,Integer::sum);
        } catch (Throwable error) {
          String finding=category+" "+id+" "+error;
          findings.add(finding);
          if (logged.add(category + error.getClass().getName())) TConstruct.LOG.error("Recipe audit failure {}",finding,error);
        } finally { g.pose().popPose(); }
      }
      if (index==recipes.size() && !complete && failure==null) {
        String report="native_jei_layouts="+nativeLayouts.size()+"\nrecipes="+recipes.size()+"\nrendered="+counts.values().stream().mapToInt(Integer::intValue).sum()+"\ncategories="+counts+"\nfindings="+findings.size()+"\n"+String.join("\n",findings)+"\n";
        try { Files.writeString(Path.of("recipe-viewer-report.txt"),report); }
        catch (Exception e) { throw new IllegalStateException(e); }
        TConstruct.LOG.info("TiC actual EMI JEI all registered recipe audit COMPLETE {}",report);
        if (!findings.isEmpty()) failure=new IllegalStateException("Actual recipe viewer audit: "+findings.size()+" failures; first "+findings.get(0));
        else complete=true;
      }
    }
    @Override public boolean isPauseScreen() { return false; }
  }
}
