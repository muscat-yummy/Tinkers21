package slimeknights.tconstruct.library.utils;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/** Preserve hand-specific block actions while deferring empty hands to native default interaction. */
public final class BlockInteractionUtil {
  private BlockInteractionUtil() {}

  public static ItemInteractionResult withItem(ItemStack stack, Supplier<InteractionResult> interaction) {
    if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    InteractionResult result = interaction.get();
    if (result == InteractionResult.SUCCESS) return ItemInteractionResult.SUCCESS;
    if (result == InteractionResult.CONSUME) return ItemInteractionResult.CONSUME;
    if (result == InteractionResult.CONSUME_PARTIAL) return ItemInteractionResult.CONSUME_PARTIAL;
    if (result == InteractionResult.FAIL) return ItemInteractionResult.FAIL;
    return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
  }
}
