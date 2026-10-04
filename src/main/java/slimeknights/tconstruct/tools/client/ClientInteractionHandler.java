package slimeknights.tconstruct.tools.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.network.InteractWithAirPacket;

/** Modifier air interactions use NeoForge's cancellable input boundary. */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public class ClientInteractionHandler {
  private static boolean targetsAir(Minecraft minecraft) {
    return minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.MISS;
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  static void chestplateToolUse(InputEvent.InteractionKeyMappingTriggered event) {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;
    if (!event.isUseItem() || player == null || player.isSpectator() || !targetsAir(minecraft)) {
      return;
    }
    InteractionHand hand = event.getHand();
    // This replaces RightClickEmpty, which fires only for an empty hand.
    if (!player.getItemInHand(hand).isEmpty()) {
      return;
    }
    ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
    if (chestplate.is(TinkerTags.Items.INTERACTABLE_ARMOR)) {
      TinkerNetwork.getInstance().sendToServer(InteractWithAirPacket.fromChestplate(hand));
      InteractionResult result = InteractionHandler.onChestplateUse(player, chestplate, hand);
      if (result.consumesAction()) {
        event.setSwingHand(result.shouldSwing());
        event.setCanceled(true);
        minecraft.gameRenderer.itemInHandRenderer.itemUsed(hand);
        // The native canceled use-input path returns from the hand loop, preventing a second interaction.
      }
    }
  }

  @SubscribeEvent
  static void leftClickAir(InputEvent.InteractionKeyMappingTriggered event) {
    Minecraft minecraft = Minecraft.getInstance();
    Player player = minecraft.player;
    if (!event.isAttack() || player == null || player.isSpectator() || !targetsAir(minecraft)) {
      return;
    }
    ItemStack tool = player.getMainHandItem();
    if (tool.is(TinkerTags.Items.INTERACTABLE_LEFT)) {
      // Empty-left-click notification previously ran after vanilla reset this ticker.
      player.resetAttackStrengthTicker();
      TinkerNetwork.getInstance().sendToServer(InteractWithAirPacket.LEFT_CLICK);
      InteractionResult result = InteractionHandler.onLeftClickInteraction(player, tool, InteractionHand.MAIN_HAND);
      if (result.consumesAction()) {
        event.setSwingHand(result.shouldSwing());
        event.setCanceled(true);
        minecraft.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND);
      }
    }
  }
}
