package slimeknights.tconstruct.plugin.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.material.MaterialCastingLookup;
import slimeknights.tconstruct.library.recipe.melting.IDisplayableMeltingRecipe;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.severing.SeveringRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.material.IDisplayMaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayToolTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayTinkerStationRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Native EMI entrypoint: no JEI API or runtime is used, including when both viewers are installed. */
@EmiEntrypoint
public final class TConstructEmiPlugin implements EmiPlugin {
  private final Map<String,Integer> variants=new HashMap<>();
  @Override public void initialize(EmiInitRegistry registry) { registry.addIngredientSerializer(TiCEmiStack.class,TiCEmiStack.SERIALIZER); }
  @Override public void register(EmiRegistry registry) {
    variants.clear();
    var level=Minecraft.getInstance().level;
    if(level==null) throw new IllegalStateException("Native EMI registration requires joined client world");
    RegistryAccess access=level.registryAccess();RecipeManager manager=registry.getRecipeManager();
    var basin=category(registry,"casting_basin","jei.tconstruct.casting.basin",TinkerSmeltery.searedBasin.get());
    var table=category(registry,"casting_table","jei.tconstruct.casting.table",TinkerSmeltery.searedTable.get());
    var melting=category(registry,"melting","jei.tconstruct.melting.title",TinkerSmeltery.searedMelter.get());
    var foundry=category(registry,"foundry","jei.tconstruct.foundry.title",TinkerSmeltery.foundryController.get());
    var alloy=category(registry,"alloy","jei.tconstruct.alloy.title",TinkerSmeltery.smelteryController.get());
    var fuel=category(registry,"fuel","jei.tconstruct.fuel.title",Items.LAVA_BUCKET);
    var entity=category(registry,"entity_melting","jei.tconstruct.entity_melting.title",TinkerSmeltery.smelteryController.get());
    var molding=category(registry,"molding","jei.tconstruct.molding.title",TinkerSmeltery.searedTable.get());
    var modifiers=category(registry,"modifiers","jei.tconstruct.modifiers.title",TinkerTables.tinkerStation.get());
    var severing=category(registry,"severing","jei.tconstruct.severing.title",Items.IRON_SWORD);
    var building=category(registry,"tool_recipes","jei.tconstruct.tinkering.tool_building",TinkerTables.tinkerStation.get());
    var tinkering=category(registry,"tool_modification","jei.tconstruct.tool_tinkering.title",TinkerTables.tinkerStation.get());
    var materials=category(registry,"materials","jei.tconstruct.materials.title",TinkerTables.partBuilder.get());
    var parts=category(registry,"part_builder","jei.tconstruct.part_builder.title",TinkerTables.partBuilder.get());
    var worktable=category(registry,"worktable","jei.tconstruct.modifier_worktable.title",TinkerTables.modifierWorktable.get());
    registry.addWorkstation(basin,EmiStack.of(TinkerSmeltery.scorchedBasin.get()));
    registry.addWorkstation(table,EmiStack.of(TinkerSmeltery.scorchedTable.get()));
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.CASTING_BASIN.get(),IDisplayableCastingRecipe.class)) casting(registry,basin,r);
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.CASTING_TABLE.get(),IDisplayableCastingRecipe.class)) casting(registry,table,r);
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.MELTING.get(),IDisplayableMeltingRecipe.class)) {
      var a=recipe(melting,r.getRecipeId()).input(items(r.getInputs())).note(temperature(r.getTemperature())).note(Component.translatable("jei.tconstruct.melting.time",r.getTime()/4));
      a.outputChoice(r.getOutputs().stream().map(TConstructEmiPlugin::fluid).toList());publish(registry,a);
      var b=recipe(foundry,r.getRecipeId()).input(items(r.getInputs())).note(temperature(r.getTemperature())).note(Component.translatable("jei.tconstruct.melting.time",r.getTime()/4));
      // Each list is an alternative for one output slot, not a list of simultaneously produced fluids.
      for(var alternatives:r.getOutputWithByproducts()) if(!alternatives.isEmpty()) {
        b.outputChoice(alternatives.stream().map(TConstructEmiPlugin::fluid).toList());
      }
      publish(registry,b);
    }
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.ALLOYING.get(),AlloyRecipe.class)) {
      var a=recipe(alloy,r.getId()).output(fluid(r.getOutput())).note(temperature(r.getTemperature()));
      r.getInputs().forEach(i->{if(i.catalyst())a.catalyst(fluids(i.fluid().getFluids()));else a.input(fluids(i.fluid().getFluids()));});publish(registry,a);
    }
    for(var r:RecipeHelper.getRecipes(manager,TinkerRecipeTypes.FUEL.get(),MeltingFuel.class)) {
      var a=recipe(fuel,r.getId()).note(temperature(r.getTemperature())).note(Component.translatable("jei.tconstruct.melting.multiplier",r.getRate()/10f));
      if(!r.getInputs().isEmpty())a.input(fluids(r.getInputs())).note(Component.translatable("jei.tconstruct.fuel.duration.liquid",r.getDuration()/5,r.getInputs().get(0).getAmount()));
      else {
        var solid=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(i->i.getBurnTime(TinkerRecipeTypes.FUEL.get())>0).toList();
        a.input(items(solid)).note(Component.translatable("jei.tconstruct.fuel.solid"));
      }
      publish(registry,a);
    }
    var entities=new ArrayList<>(RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.ENTITY_MELTING.get(),EntityMeltingRecipe.class));
    entities.add(new slimeknights.tconstruct.plugin.jei.entity.DefaultEntityMeltingRecipe(entities));
    for(var r:entities)publish(registry,recipe(entity,r.getId()).input(entityIngredients(r.getIngredient().getTypes())).output(fluid(r.getOutput())).note(Component.literal(r.getDamage()/2f+" ♥")));
    for(var type:List.of(TinkerRecipeTypes.MOLDING_TABLE.get(),TinkerRecipeTypes.MOLDING_BASIN.get())) for(var r:RecipeHelper.getJEIRecipes(access,manager,type,MoldingRecipe.class)) {
      var a=recipe(molding,r.getId()).input(EmiIngredient.of(r.getMaterial())).output(EmiStack.of(r.getResultItem(access)));
      if(!r.getPattern().isEmpty()){if(r.isPatternConsumed())a.input(EmiIngredient.of(r.getPattern()));else a.catalyst(EmiIngredient.of(r.getPattern()));}publish(registry,a);
    }
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.SEVERING.get(),SeveringRecipe.class))publish(registry,recipe(severing,r.getId()).input(entityIngredients(r.getIngredient().getTypes())).output(EmiStack.of(r.getOutput())));
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.TINKER_STATION.get(),IDisplayModifierRecipe.class)) {
      var a=station(modifiers,r).output(new TiCEmiStack("modifier",r.getDisplayResult().getId().toString())).note(r.getDisplayResult().getDisplayName());
      if(r.getSlots()!=null)a.note(Component.translatable("jei.tconstruct.modifiers.slots",r.getSlots().count(),r.getSlots().type().getDisplayName()));
      if(r.getLevel().min()>0)a.note(Component.translatable("jei.tconstruct.modifiers.level.min",r.getLevel().min()));
      if(r.getLevel().max()<Integer.MAX_VALUE)a.note(Component.translatable("jei.tconstruct.modifiers.level.max",r.getLevel().max()));
      if(r.isIncremental())a.note(Component.translatable("jei.tconstruct.modifiers.incremental"));publish(registry,a);
    }
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.TINKER_STATION.get(),IDisplayToolTinkering.class)) {
      var a=station(tinkering,r).note(r.getTitle());publish(registry,a);
    }
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.TINKER_STATION.get(),ToolBuildingRecipe.class)) {
      var a=recipe(building,r.getId());r.getAllToolParts().forEach(i->a.input(items(i)));r.getExtraRequirements().forEach(i->a.input(EmiIngredient.of(i)));
      a.outputChoice(r.getDisplayOutput().stream().map(EmiStack::of).toList());r.getHiddenInputs().forEach(i->a.focusInput(EmiStack.of(i)));publish(registry,a);
    }
    Stream.<IDisplayMaterialRecipe>concat(MaterialRecipeCache.getSortedRecipes().stream(),Stream.concat(MaterialCastingLookup.getSortedCastingFluids().stream(),MaterialCastingLookup.getSortedCompositeFluids().stream())).filter(IDisplayMaterialRecipe::isVisible).forEach(r->{
      var a=recipe(materials,r.getRecipeId()).input(items(r.getDisplayItems()));if(!r.getFluids().isEmpty())a.input(fluids(r.getFluids()));
      if(r.getInput()!=null && !r.getInput().isEmpty())a.input(new TiCEmiStack("material",r.getInput().getVariant().toString()));
      a.output((EmiStack)new TiCEmiStack("material",r.getMaterial().getVariant().toString()).setAmount(Math.max(1,r.getValue())));
      if(r.hasLeftover())a.output(EmiStack.of(r.getLeftover()));publish(registry,a);
    });
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.PART_BUILDER.get(),IDisplayPartBuilderRecipe.class)) {
      var a=recipe(parts,r.getId());
      if(r.getPatternItems().stream().allMatch(i->i.is(slimeknights.tconstruct.common.TinkerTags.Items.REUSABLE_PATTERNS)))a.catalyst(items(r.getPatternItems()));else a.input(items(r.getPatternItems()));
      r.getPatterns().forEach(p->a.catalyst(new TiCEmiStack("pattern",p.toString())));
      var materialList=r.getMaterials().stream().map(m->(EmiIngredient)new TiCEmiStack("material",m.getVariant().toString()).setAmount(Math.max(1,r.getCost()))).toList();
      if(!materialList.isEmpty())a.input(EmiIngredient.of(materialList));else a.input(items(r.getMaterialItems()));
      r.getMaterialItems().forEach(i->a.focusInput(EmiStack.of(i)));a.outputChoice(r.getResultItems().stream().map(EmiStack::of).toList());
      r.getHiddenInputs().forEach(i->a.focusInput(EmiStack.of(i)));r.getHiddenOutputs().forEach(i->a.focusOutput(EmiStack.of(i)));
      a.note(r.getDisplayTitle()).note(Component.translatable("jei.tconstruct.part_builder.cost",r.getCost()));publish(registry,a);
    }
    for(var r:RecipeHelper.getJEIRecipes(access,manager,TinkerRecipeTypes.MODIFIER_WORKTABLE.get(),IModifierWorktableRecipe.class)) {
      if(r.getModifierOptions(null).isEmpty())continue;
      var a=recipe(worktable,r.getId()).note(r.getTitle());if(r.isToolInput())a.input(items(r.getInputTools()));else a.catalyst(items(r.getInputTools()));
      for(int i=0;i<r.getInputCount();i++)a.input(items(r.getDisplayItems(i)));
      var choices=r.getModifierOptions(null).stream().map(m->(EmiStack)new TiCEmiStack("modifier",m.getId().toString())).toList();
      if(r.isModifierOutput())a.outputChoice(choices);else a.catalyst(EmiIngredient.of(choices));
      a.note(r.getDescription(null));publish(registry,a);
    }
    TConstruct.LOG.info("TiC native EMI registration without JEI PASS categories=15 displays={}",variants.values().stream().mapToInt(Integer::intValue).sum());
  }
  private EmiRecipeCategory category(EmiRegistry registry,String path,String title,ItemLike workstation) {
    var result=new EmiRecipeCategory(TConstruct.getResource(path),EmiStack.of(workstation)) {
      @Override public Component getName(){return Component.translatable(title);}
    };
    registry.addCategory(result);registry.addWorkstation(result,EmiStack.of(workstation));return result;
  }
  private TiCEmiRecipe recipe(EmiRecipeCategory category,ResourceLocation original) {
    String root="/emi/"+category.getId().getPath()+"/"+(original==null?"synthetic":original.getNamespace()+"/"+original.getPath());
    int index=variants.merge(root,1,Integer::sum)-1;
    return new TiCEmiRecipe(category,TConstruct.getResource(root+(index==0?"":"/variant_"+index)));
  }
  private static EmiStack fluid(FluidStack stack){return NeoForgeEmiStack.of(stack.copy());}
  private static EmiIngredient fluids(List<FluidStack> stacks){return EmiIngredient.of(stacks.stream().filter(s->!s.isEmpty()).map(TConstructEmiPlugin::fluid).toList());}
  private static EmiIngredient items(List<ItemStack> stacks){return EmiIngredient.of(stacks.stream().filter(s->!s.isEmpty()).map(s->EmiStack.of(s.copy())).toList());}
  private static EmiIngredient entityIngredients(java.util.Collection<net.minecraft.world.entity.EntityType<?>> types){return EmiIngredient.of(types.stream().map(t->new TiCEmiStack("entity",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t).toString())).toList());}
  private static Component temperature(int temperature){return Component.translatable("jei.tconstruct.temperature",temperature);}
  private void casting(EmiRegistry registry,EmiRecipeCategory category,IDisplayableCastingRecipe r){
    var a=recipe(category,r.getRecipeId()).input(fluids(r.getFluids()));if(r.isConsumed())a.input(items(r.getCastItems()));else a.catalyst(items(r.getCastItems()));
    a.outputChoice(r.getOutputs().stream().map(s->EmiStack.of(s.copy())).toList());a.note(Component.translatable("jei.tconstruct.casting.time",r.getCoolingTime()/20));publish(registry,a);
  }
  private TiCEmiRecipe station(EmiRecipeCategory category,IDisplayTinkerStationRecipe r){
    var a=recipe(category,r.getRecipeId());for(int i=0;i<r.getInputCount();i++)a.input(items(r.getDisplayItems(i)));
    if(r instanceof IDisplayToolTinkering t && t.isToolCatalyst())a.catalyst(items(r.getToolWithoutModifier()));else a.input(items(r.getToolWithoutModifier()));
    a.outputChoice(r.getToolWithModifier().stream().map(i->EmiStack.of(i.copy())).toList());return a;
  }
  private static void publish(EmiRegistry registry,TiCEmiRecipe recipe){registry.addRecipe(recipe.finish());}
}
