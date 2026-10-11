package slimeknights.tconstruct.mixin;

import java.util.List;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;

/** EMI 1.1.24 indexes trimmed native crafting remainders using untrimmed grid coordinates. */
@Pseudo
@Mixin(targets = "dev.emi.emi.recipe.EmiShapedRecipe", remap = false)
public abstract class EmiShapedRemainderMixin {
  @Inject(method = "setRemainders", at = @At("HEAD"), cancellable = true, remap = false)
  private static void tconstruct$skipEmptyDefaultRemainders(List<?> inputs, CraftingRecipe recipe, CallbackInfo callback) {
    // Exact class only: addon overrides and recipes with bucket/bottle remainders keep EMI's path.
    if (recipe.getClass() == ShapedMaterialsRecipe.class && recipe.getIngredients().stream()
        .flatMap(ingredient -> java.util.Arrays.stream(ingredient.getItems()))
        .noneMatch(stack -> stack.hasCraftingRemainingItem())) {
      callback.cancel();
    }
  }
}
