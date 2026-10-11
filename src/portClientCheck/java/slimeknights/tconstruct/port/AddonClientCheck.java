package slimeknights.tconstruct.port;

import example.ticaddon.AddonCheck;
import net.minecraft.client.Minecraft;

/** Sends a registry-aware serialized addon tool through actual server inventory packets. */
public final class AddonClientCheck {
  private static boolean started;
  private static int ticks;
  private static volatile boolean ready;
  private static volatile Throwable failure;
  private static long nonce;

  public static boolean tick(Minecraft minecraft) {
    if (failure != null) throw new IllegalStateException("Independent addon server setup failed", failure);
    if (!started) {
      started = true;
      var server = minecraft.getSingleplayerServer();
      var uuid = minecraft.player.getUUID();
      nonce = uuid.getMostSignificantBits();
      AddonCheck.NETWORK.sendToServer(new AddonCheck.Request(nonce));
      server.execute(() -> {
        try {
          var player = server.getPlayerList().getPlayer(uuid);
          AddonCheck.require(player != null, "Addon fixture player is absent");
          player.getInventory().setItem(8, AddonCheck.create(player.registryAccess()));
          player.inventoryMenu.broadcastChanges();
          ready = true;
        } catch (Throwable problem) {
          failure = problem;
        }
      });
      return false;
    }
    if (!ready || AddonCheck.receivedReply != nonce) return false;
    var stack = minecraft.player.getInventory().getItem(8);
    if (stack.isEmpty()) return false;
    AddonCheck.verify(stack);
    return ++ticks >= 20;
  }
}
