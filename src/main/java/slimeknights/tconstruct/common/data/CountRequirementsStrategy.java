package slimeknights.tconstruct.common.data;

import net.minecraft.advancements.AdvancementRequirements.Strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CountRequirementsStrategy implements Strategy {
  private final int[] sizes;
  public CountRequirementsStrategy(int... sizes) {
    this.sizes = sizes;
  }

  @Override
  public net.minecraft.advancements.AdvancementRequirements create(Collection<String> strings) {
    return new net.minecraft.advancements.AdvancementRequirements(
      java.util.Arrays.stream(createRequirements(strings)).map(group -> List.of(group)).toList());
  }

  /** Legacy grouped-array view retained for callers; native builders use create(). */
  @Deprecated
  public String[][] createRequirements(Collection<String> strings) {
    String[][] requirements = new String[sizes.length][];
    List<String> list = new ArrayList<>(strings);
    int nextIndex = 0;
    for (int i = 0; i < sizes.length; i++) {
      requirements[i] = new String[sizes[i]];
      for (int j = 0; j < sizes[i]; j++) {
        requirements[i][j] = list.get(nextIndex);
        nextIndex++;
      }
    }
    return requirements;
  }
}
