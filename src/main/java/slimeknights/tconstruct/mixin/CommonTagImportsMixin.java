package slimeknights.tconstruct.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import slimeknights.tconstruct.library.utils.CommonTagImports;
import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
public abstract class CommonTagImportsMixin {
  @ModifyVariable(method = "build(Ljava/util/Map;)Ljava/util/Map;", at = @At("HEAD"), argsOnly = true)
  private Map<ResourceLocation,List<TagLoader.EntryWithSource>> tconstruct$resolveCommonImports(
      Map<ResourceLocation,List<TagLoader.EntryWithSource>> entries) {
    return CommonTagImports.resolve(entries);
  }
}
