package slimeknights.tconstruct.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.mantle.recipe.helper.RecipeDecodeContext;
import slimeknights.tconstruct.library.recipe.LegacyRecipeJson;

/** Supplies the real resource entry ID at native conditional recipe parsing. */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerIdentityMixin {
  @WrapOperation(
    method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
    at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
  @SuppressWarnings("unchecked")
  private DataResult<?> tconstruct$decodeWithHolderId(Codec<?> codec, DynamicOps<?> ops, Object input,
      Operation<DataResult<?>> original, @Local Map.Entry<ResourceLocation,JsonElement> entry) {
    return RecipeDecodeContext.withId(entry.getKey(), () -> {
      Object nativeInput = entry.getKey().getNamespace().equals("tconstruct")
        ? LegacyRecipeJson.migrate((JsonElement) input, (DynamicOps<JsonElement>) ops) : input;
      return original.call(codec, ops, nativeInput);
    });
  }
}
