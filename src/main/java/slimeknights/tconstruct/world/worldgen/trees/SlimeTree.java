package slimeknights.tconstruct.world.worldgen.trees;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Native tree growers, including the original 85 percent tall ender-tree selection. */
public final class SlimeTree {
  private static final Map<FoliageType,TreeGrower> GROWERS = new EnumMap<>(FoliageType.class);

  private SlimeTree() {}

  public static TreeGrower get(FoliageType foliage) {
    return GROWERS.computeIfAbsent(foliage, SlimeTree::create);
  }

  private static TreeGrower create(FoliageType foliage) {
    String name = "tconstruct:" + foliage.name().toLowerCase(Locale.ROOT) + "_slime";
    if (foliage == FoliageType.ENDER) {
      return new TreeGrower(name, 0.85F, Optional.empty(), Optional.empty(),
        Optional.of(TinkerStructures.enderSlimeTree), Optional.of(TinkerStructures.enderSlimeTreeTall),
        Optional.of(TinkerStructures.enderSlimeTree), Optional.of(TinkerStructures.enderSlimeTreeTall));
    }
    ResourceKey<ConfiguredFeature<?,?>> feature = switch (foliage) {
      case EARTH -> TinkerStructures.earthSlimeTree;
      case SKY -> TinkerStructures.skySlimeTree;
      case BLOOD -> TinkerStructures.bloodSlimeFungus;
      case ICHOR -> TinkerStructures.ichorSlimeFungus;
      case ENDER -> throw new IllegalStateException("Ender grower uses two features");
    };
    return new TreeGrower(name, Optional.empty(), Optional.of(feature), Optional.of(feature));
  }
}
