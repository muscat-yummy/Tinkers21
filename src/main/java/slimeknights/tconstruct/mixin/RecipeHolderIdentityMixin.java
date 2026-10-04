package slimeknights.tconstruct.mixin;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.mantle.recipe.helper.RecipeHolderCodec;

/** Keeps native recipe synchronization bytes while exposing the real holder ID during payload decode. */
@Mixin(RecipeHolder.class)
public abstract class RecipeHolderIdentityMixin {
  @Shadow @Final @Mutable
  public static StreamCodec<RegistryFriendlyByteBuf,RecipeHolder<?>> STREAM_CODEC;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void tconstruct$installIdentityScope(CallbackInfo ci) {
    STREAM_CODEC = RecipeHolderCodec.create(Recipe.STREAM_CODEC);
  }
}
