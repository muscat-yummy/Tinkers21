package slimeknights.tconstruct.mixin;

import java.util.HashMap;
import java.util.Map;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Give TiC and JEI tag category/variant displays distinct EMI IDs, retaining the real backing ID. */
@Pseudo
@Mixin(targets="dev.emi.emi.jemi.JemiRecipe",remap=false)
public abstract class EmiRecipeIdentityMixin {
  @Shadow public ResourceLocation id;
  @Shadow public ResourceLocation originalId;
  @Shadow public IRecipeCategory<?> category;
  @Unique private static final Map<String,Integer> tconstruct$variants=new HashMap<>();
  @Unique private static Object tconstruct$runtime;
  @Inject(method="<init>",at=@At("RETURN"),remap=false)
  private void tconstruct$distinctDisplayIdentity(CallbackInfo callback) {
    var uid=category.getRecipeType().getUid();
    boolean tic=uid.getNamespace().equals("tconstruct");
    boolean tags=uid.getNamespace().equals("minecraft") && uid.getPath().startsWith("tag_recipes/");
    if(originalId==null || !tic && !tags) return;
    // Runtime instance changes on JEI reload. Constructor order is the actual registration order.
    try {
      Object runtime=Class.forName("dev.emi.emi.jemi.JemiPlugin").getField("runtime").get(null);
      synchronized(tconstruct$variants) {
        if(tconstruct$runtime!=runtime) { tconstruct$runtime=runtime; tconstruct$variants.clear(); }
        String path="/"+originalId.getNamespace()+"/jei/"+category.getRecipeType().getUid().getPath()+"/"+originalId.getPath();
        int variant=tconstruct$variants.merge(path,1,Integer::sum)-1;
        id=ResourceLocation.fromNamespaceAndPath("jei",path+(variant==0?"":"/variant_"+variant));
      }
    } catch(ReflectiveOperationException error) { throw new IllegalStateException("EMI runtime identity unavailable",error); }
  }
  /** EMI passed null to its extras builder; expose the already-created actual JEI slot drawables. */
  @org.spongepowered.asm.mixin.injection.ModifyArg(method="addWidgets", index=0, remap=false,
      at=@At(value="INVOKE", target="Ldev/emi/emi/jemi/impl/extras/JemiRecipeExtrasBuilder;<init>(Lmezz/jei/api/gui/ingredient/IRecipeSlotDrawablesView;)V", remap=false))
  private mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView tconstruct$extrasSlots(
      mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView existing,
      @com.llamalad7.mixinextras.sugar.Local java.util.Optional<mezz.jei.api.gui.IRecipeLayoutDrawable<?>> layout) {
    if(existing!=null) return existing;
    java.util.List<mezz.jei.api.gui.ingredient.IRecipeSlotDrawable> slots=layout.map(value ->
      value.getRecipeSlotsView().getSlotViews().stream()
        .filter(mezz.jei.api.gui.ingredient.IRecipeSlotDrawable.class::isInstance)
        .map(mezz.jei.api.gui.ingredient.IRecipeSlotDrawable.class::cast).toList()).orElse(java.util.List.of());
    return () -> slots;
  }

}
