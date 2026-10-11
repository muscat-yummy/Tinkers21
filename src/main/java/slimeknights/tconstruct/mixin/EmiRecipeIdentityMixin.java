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

/** Give TiC's category/variant display recipes distinct EMI IDs, retaining the real backing ID. */
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
    if(originalId==null || !category.getRecipeType().getUid().getNamespace().equals("tconstruct")) return;
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
}
