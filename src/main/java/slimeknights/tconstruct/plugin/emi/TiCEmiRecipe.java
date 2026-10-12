package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Native EMI presentation. Index-only focuses are kept separate from visible consumed inputs. */
public final class TiCEmiRecipe extends BasicEmiRecipe {
  private final List<EmiIngredient> visibleInputs=new ArrayList<>(), visibleCatalysts=new ArrayList<>();
  private final List<EmiIngredient> visibleOutputs=new ArrayList<>();
  private final List<Component> notes=new ArrayList<>();
  private boolean indexedOnly;
  public TiCEmiRecipe(EmiRecipeCategory category,ResourceLocation id) { super(category,id,174,94); }
  public TiCEmiRecipe input(EmiIngredient stack) { if(!stack.isEmpty()){ inputs.add(stack); visibleInputs.add(stack); }return this; }
  public TiCEmiRecipe catalyst(EmiIngredient stack) { if(!stack.isEmpty()){ catalysts.add(stack); visibleCatalysts.add(stack); }return this; }
  public TiCEmiRecipe output(EmiStack stack) { if(!stack.isEmpty()){ outputs.add(stack); visibleOutputs.add(stack); }return this; }
  public TiCEmiRecipe outputChoice(List<EmiStack> alternatives) {
    var valid=alternatives.stream().filter(s->!s.isEmpty()).toList();
    if(!valid.isEmpty()){outputs.addAll(valid);visibleOutputs.add(EmiIngredient.of(valid));}return this;
  }
  public TiCEmiRecipe focusInput(EmiIngredient stack) { if(!stack.isEmpty()){ inputs.add(stack); indexedOnly=true; }return this; }
  public TiCEmiRecipe focusOutput(EmiStack stack) { if(!stack.isEmpty()){ outputs.add(stack); indexedOnly=true; }return this; }
  public TiCEmiRecipe note(Component note) { if(note!=null)notes.add(note);return this; }
  public TiCEmiRecipe finish() {
    int rows=Math.max((visibleInputs.size()+3)/4,(visibleOutputs.size()+2)/3);
    height=Math.max(76,28+rows*20+((visibleCatalysts.size()+7)/8)*20+notes.size()*10);
    return this;
  }
  @Override public boolean supportsRecipeTree() {
    return !indexedOnly && outputs.size()==1 && !(outputs.get(0) instanceof TiCEmiStack)
      && inputs.stream().flatMap(i->i.getEmiStacks().stream()).noneMatch(TiCEmiStack.class::isInstance);
  }
  @Override public void addWidgets(WidgetHolder widgets) {
    int rows=Math.max((visibleInputs.size()+3)/4,(visibleOutputs.size()+2)/3);
    for(int i=0;i<visibleInputs.size();i++)widgets.addSlot(visibleInputs.get(i),4+(i%4)*20,6+(i/4)*20);
    for(int i=0;i<visibleOutputs.size();i++)widgets.addSlot(visibleOutputs.get(i),110+(i%3)*20,6+(i/3)*20).recipeContext(this);
    widgets.addDrawable(86,8,20,16,(g,mx,my,d)->g.drawString(net.minecraft.client.Minecraft.getInstance().font,Component.literal("→"),0,3,0xff808080,false));
    int y=12+rows*20;
    for(int i=0;i<visibleCatalysts.size();i++)widgets.addSlot(visibleCatalysts.get(i),4+(i%8)*20,y+(i/8)*20).catalyst(true);
    y+=((visibleCatalysts.size()+7)/8)*20+3;
    for(Component note:notes) {
      final Component text=note;
      widgets.addDrawable(4,y,166,10,(g,mx,my,d)->g.drawString(net.minecraft.client.Minecraft.getInstance().font,
        net.minecraft.client.Minecraft.getInstance().font.plainSubstrByWidth(text.getString(),164),0,0,0xff606060,false));
      y+=10;
    }
  }
}
