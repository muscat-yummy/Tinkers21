package slimeknights.tconstruct.port;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
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
  private static boolean complete;

  @SubscribeEvent
  public static void clientTick(ClientTickEvent.Post event) {
    if (complete) return;
    Minecraft minecraft = Minecraft.getInstance();
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
