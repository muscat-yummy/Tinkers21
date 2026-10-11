package slimeknights.tconstruct.port;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.library.utils.BlockInteractionUtil;

import java.util.concurrent.atomic.AtomicInteger;

public final class BlockInteractionSmoke {
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    AtomicInteger calls = new AtomicInteger();
    check(BlockInteractionUtil.withItem(ItemStack.EMPTY, () -> {
      calls.incrementAndGet();
      throw new AssertionError("Empty hand should reach useWithoutItem, not the item callback");
    }) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, "Empty hand was suppressed");
    ItemStack held = new ItemStack(Items.HONEYCOMB);
    held.setCount(3);
    check(BlockInteractionUtil.withItem(held, () -> {
      calls.incrementAndGet();
      return InteractionResult.PASS;
    }) == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
      "An already attempted held-item action could be repeated by the default block callback");
    check(calls.get() == 1 && held.getCount() == 3, "Interaction forwarding changed the item or invoked twice");
    check(BlockInteractionUtil.withItem(held, () -> InteractionResult.sidedSuccess(true)).result()
      == InteractionResult.SUCCESS, "Client swing result lost");
    check(BlockInteractionUtil.withItem(held, () -> InteractionResult.sidedSuccess(false)).result()
      == InteractionResult.CONSUME, "Server consumption result lost");
    check(BlockInteractionUtil.withItem(held, () -> InteractionResult.FAIL) == ItemInteractionResult.FAIL,
      "Explicit refusal lost");
    check(BlockInteractionUtil.withItem(held, () -> InteractionResult.CONSUME_PARTIAL).result()
      == InteractionResult.CONSUME_PARTIAL, "Partial consumption result lost");
    System.out.println("BlockInteractionSmoke passed");
  }
}
