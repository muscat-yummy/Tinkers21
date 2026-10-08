package slimeknights.tconstruct.port;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import slimeknights.tconstruct.TConstruct;

/** Test-source-only check of the actual graphical client and its initial resource reload. */
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT)
public final class ClientBootstrapCheck {
  private static int titleTicks;
  private static int waitingTicks;
  private static boolean complete;

  @SubscribeEvent
  public static void clientTick(ClientTickEvent.Post event) {
    if (complete) return;
    Minecraft minecraft = Minecraft.getInstance();
    if (++waitingTicks > 2400) {
      throw new IllegalStateException("Client bootstrap did not reach the title screen: " +
        (minecraft.screen == null ? "none" : minecraft.screen.getClass().getName()));
    }
    if (minecraft.screen instanceof AccessibilityOnboardingScreen && minecraft.getOverlay() == null) {
      minecraft.options.onboardingAccessibilityFinished();
      minecraft.screen.onClose();
      return;
    }
    if (!(minecraft.screen instanceof TitleScreen) || minecraft.getOverlay() != null) {
      titleTicks = 0;
      return;
    }
    if (++titleTicks < 20) return;
    BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse("tconstruct:pickaxe")).orElseThrow();
    if (minecraft.getItemRenderer() == null || minecraft.getModelManager() == null ||
        minecraft.getWindow().getWidth() <= 0 || minecraft.getWindow().getHeight() <= 0) {
      throw new IllegalStateException("Actual client did not initialize its window, models and item renderer");
    }
    complete = true;
    TConstruct.LOG.info("TiC actual graphical client and initial resource reload PASS");
    minecraft.stop();
  }
}
