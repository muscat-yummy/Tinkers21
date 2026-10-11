package slimeknights.tconstruct.mixin;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.helper.ToolTooltipPolicy;

/** TiC renders enchantments through its modifier tooltip, without mutating component data. */
@Mixin(ItemStack.class)
public abstract class ToolTooltipMixin {
  @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
  private <T extends TooltipProvider> void tconstruct$hideEnchantments(
      DataComponentType<T> type, Item.TooltipContext context, Consumer<Component> output,
      TooltipFlag flags, CallbackInfo ci) {
    if ((type == DataComponents.ENCHANTMENTS || type == DataComponents.STORED_ENCHANTMENTS)
        && ((ItemStack) (Object) this).getItem() instanceof IModifiableDisplay
        && ToolTooltipPolicy.hideEnchantments((ItemStack) (Object) this)) {
      ci.cancel();
    }
  }
}
