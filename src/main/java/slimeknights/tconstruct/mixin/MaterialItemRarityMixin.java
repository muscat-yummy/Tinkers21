package slimeknights.tconstruct.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

/** Resolve material rarity before native enchantment promotion, including after material reloads. */
@Mixin(ItemStack.class)
public abstract class MaterialItemRarityMixin {
  @ModifyExpressionValue(method = "getRarity", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
  private Object tconstruct$materialRarity(Object nativeRarity) {
    ItemStack stack = (ItemStack) (Object) this;
    if (stack.getItem() instanceof IMaterialItem materialItem) {
      return materialItem.getMaterialRarity(stack);
    }
    return nativeRarity;
  }
}
