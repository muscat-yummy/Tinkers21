package slimeknights.tconstruct.port;

import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.AdvancementRequirements;
import slimeknights.tconstruct.common.data.CountRequirementsStrategy;
import java.util.List;
import java.util.Set;

public final class CountRequirementsSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    var groups = new CountRequirementsStrategy(2, 1).create(List.of("a", "b", "c"));
    check(groups.requirements().equals(List.of(List.of("a", "b"), List.of("c"))), "Ordered alternatives changed");
    check(groups.test(Set.of("a", "c")::contains) && groups.test(Set.of("b", "c")::contains),
      "An alternative and the mandatory group did not complete the advancement");
    check(!groups.test(Set.of("a", "b")::contains) && !groups.test(Set.of("c")::contains),
      "An incomplete group completed the advancement");
    check(groups.validate(Set.of("a", "b", "c")).error().isEmpty(), "Native validation rejected grouped criteria");
    var encoded = AdvancementRequirements.CODEC.encodeStart(JsonOps.INSTANCE, groups).getOrThrow();
    check(AdvancementRequirements.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(groups),
      "Native requirement codec changed grouping");
    var mandatory = new CountRequirementsStrategy(1, 1).create(List.of("a", "b"));
    check(mandatory.test(Set.of("a", "b")::contains) && !mandatory.test(Set.of("a")::contains),
      "All mandatory groups were not required");
    System.out.println("Native grouped advancement alternatives, mandatory groups, validation and codec: PASS");
  }
}
