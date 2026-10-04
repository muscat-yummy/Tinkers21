package slimeknights.tconstruct.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;

/** Run custom harvest only after native break cancellation and player restrictions are resolved. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ToolHarvestGameModeMixin {
  @Shadow @Final protected ServerPlayer player;

  @SuppressWarnings("deprecation")
  @Inject(method = "destroyBlock", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"), cancellable = true)
  private void tconstruct$harvestAfterPermission(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
    ItemStack stack = player.getMainHandItem();
    boolean handled = stack.getItem() instanceof ModifiableItem tool
      ? tool.onBlockStartBreak(stack, pos, player)
      : stack.getItem() instanceof ModifiableLauncherItem launcher
        && launcher.onBlockStartBreak(stack, pos, player);
    if (handled) ci.setReturnValue(true);
  }
}
