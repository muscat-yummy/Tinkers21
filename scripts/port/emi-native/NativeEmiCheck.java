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

/** CI-only: require native EMI recipe registration and real output/input lookup without JEI. */
public final class NativeEmiCheck {
  private static boolean started, complete;
  private static RuntimeException failure;
  private static int waiting, screenTicks;
  private static boolean actualScreen;
  public static boolean tick(Minecraft mc) {
    if (failure != null) throw failure;
    if (complete) {
      if (!actualScreen) {
        EmiRecipe basin=EmiApi.getRecipeManager().getRecipes().stream().filter(r -> r.getCategory().getId().toString().equals("tconstruct:casting_basin") && r.getOutputs().stream().anyMatch(s -> s.getItemStack().is(net.minecraft.world.item.Items.IRON_BLOCK))).findFirst().orElseThrow();
        EmiApi.displayRecipe(basin);
        actualScreen=true;
        return false;
      }
      if(++screenTicks<30) return false;
      if(!mc.screen.getClass().getName().equals("dev.emi.emi.screen.RecipeScreen")) throw new IllegalStateException("Actual EMI casting basin recipe screen did not open");
      try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
        image.writeToFile(Path.of("native-emi-casting-basin-gui.png"));
      } catch(Exception e) { throw new IllegalStateException(e); }
      TConstruct.LOG.info("TiC actual EMI iron block casting basin recipe screen rendering PASS");
      mc.setScreen(null);
      return true;
    }
    if (!started) {
      var manager = EmiApi.getRecipeManager();
      if (manager == null || manager.getRecipes().isEmpty()) {
        if (++waiting > 600) throw new IllegalStateException("EMI recipe reload did not complete");
        return false;
      }
      boolean jei=net.neoforged.fml.ModList.get().isLoaded("jei");
      boolean expected=Boolean.getBoolean("tconstruct.emiCheck.jei");
      if(jei!=expected) throw new IllegalStateException("Unexpected JEI runtime: "+jei+" expected "+expected);
      List<EmiRecipe> recipes=manager.getRecipes().stream().filter(r->r.getCategory().getId().getNamespace().equals("tconstruct")).toList();
      if(recipes.stream().anyMatch(r->!(r instanceof slimeknights.tconstruct.plugin.emi.TiCEmiRecipe))) throw new IllegalStateException("TiC category depends on JEMI instead of native EMI");
      var iron=dev.emi.emi.api.stack.EmiStack.of(net.minecraft.world.item.Items.IRON_BLOCK);
      if(manager.getRecipesByOutput(iron).stream().noneMatch(r->r.getCategory().getId().getPath().equals("casting_basin"))) throw new IllegalStateException("Iron block R lookup missing casting basin");
      if(manager.getRecipesByInput(dev.emi.emi.api.stack.EmiStack.of(net.minecraft.world.item.Items.IRON_INGOT)).stream().noneMatch(r->r.getCategory().getId().getPath().equals("melting"))) throw new IllegalStateException("Iron ingot U lookup missing melting");
      for(var recipe:recipes) for(var output:recipe.getOutputs()) {
        if(manager.getRecipesByOutput(output).stream().noneMatch(r->r==recipe)) throw new IllegalStateException("Native EMI R lookup lost output "+recipe.getId()+" "+output);
      }
      for(var recipe:recipes) for(var ingredient:java.util.stream.Stream.concat(recipe.getInputs().stream(),recipe.getOutputs().stream()).toList()) {
        for(var stack:ingredient.getEmiStacks()) if(stack instanceof slimeknights.tconstruct.plugin.emi.TiCEmiStack) {
          var serialized=dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getSerialized(stack);
          if(serialized==null) throw new IllegalStateException("Native ingredient cannot be bookmarked: "+stack);
          var restored=dev.emi.emi.api.stack.serializer.EmiIngredientSerializer.getDeserialized(serialized);
          if(restored.getEmiStacks().size()!=1 || !restored.getEmiStacks().get(0).isEqual(stack) || restored.getAmount()!=stack.getAmount()) throw new IllegalStateException("Native ingredient bookmark roundtrip lost data: "+stack);
        }
      }
      TConstruct.LOG.info("TiC native EMI all output recipe lookup and iron ingot usage PASS JEI={}",jei);
      if (recipes.isEmpty()) throw new IllegalStateException("No TiC recipes registered in EMI");
      var ids=new java.util.HashSet<String>();
      var categories=new java.util.HashSet<String>();
      for(var recipe:recipes) {
        if(recipe.getId()!=null && !ids.add(recipe.getId().toString())) throw new IllegalStateException("Duplicate audited EMI display recipe ID: "+recipe.getId());
        if(recipe.getCategory().getId().getNamespace().equals("tconstruct")) categories.add(recipe.getCategory().getId().getPath());
      }
      if(categories.size()!=15) throw new IllegalStateException("Missing TiC EMI category: "+categories);
      started = true;
      mc.setScreen(new CheckScreen(recipes));
    }
    return false;
  }
  private static final class CheckScreen extends Screen {
    private final List<EmiRecipe> recipes;
    private final List<String> findings = new ArrayList<>();
    private final Map<String,Integer> counts = new TreeMap<>();
    private int index;
    private final java.util.Set<String> logged=new java.util.HashSet<>();
    CheckScreen(List<EmiRecipe> recipes) { super(Component.literal("Native EMI audit"));this.recipes=recipes; }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
      super.render(g,mouseX,mouseY,delta);
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
            var bounds=widget.getBounds();
            int hoverX=bounds.x()+bounds.width()/2;
            int hoverY=bounds.y()+bounds.height()/2;
            widget.render(g,hoverX,hoverY,delta);
            widget.getTooltip(hoverX,hoverY);
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
        String report="recipes="+recipes.size()+"\nrendered="+counts.values().stream().mapToInt(Integer::intValue).sum()+"\ncategories="+counts+"\nfindings="+findings.size()+"\n"+String.join("\n",findings)+"\n";
        try { Files.writeString(Path.of("native-emi-report.txt"),report); }
        catch (Exception e) { throw new IllegalStateException(e); }
        TConstruct.LOG.info("TiC native EMI all registered recipe audit COMPLETE {}",report);
        if (!findings.isEmpty()) failure=new IllegalStateException("Actual recipe viewer audit: "+findings.size()+" failures; first "+findings.get(0));
        else complete=true;
      }
    }
    @Override public boolean isPauseScreen() { return false; }
  }
}
