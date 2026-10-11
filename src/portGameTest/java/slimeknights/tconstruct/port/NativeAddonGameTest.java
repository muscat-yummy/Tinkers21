package slimeknights.tconstruct.port;

import example.ticaddon.AddonCheck;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.TConstruct;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class NativeAddonGameTest {
  @GameTest(template = "bootstrap", timeoutTicks = 100)
  public static void independentAddonRegistrationAndPersistence(GameTestHelper helper) {
    AddonCheck.verify(AddonCheck.create(helper.getLevel().registryAccess()));
    helper.succeed();
    TConstruct.LOG.info("TiC independent addon registration material dynamic modifier hook and persistence PASS");
  }
}
