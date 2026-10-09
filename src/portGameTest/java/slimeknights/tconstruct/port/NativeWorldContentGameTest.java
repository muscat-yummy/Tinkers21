package slimeknights.tconstruct.port;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class NativeWorldContentGameTest {
  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualSlimeEntitiesAndWorldFeatures(GameTestHelper helper) {
    var level = helper.getLevel();
    var configured = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
    for (var key : List.of(TinkerStructures.earthSlimeTree, TinkerStructures.skySlimeTree,
      TinkerStructures.enderSlimeTree, TinkerStructures.bloodSlimeFungus)) {
      helper.assertTrue(configured.get(key).isPresent(), "Actual world registry is missing slime feature " + key.location());
    }
    var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
    for (var key : List.of(TinkerStructures.earthSlimeIsland, TinkerStructures.skySlimeIsland,
      TinkerStructures.bloodIsland, TinkerStructures.endSlimeIsland)) {
      helper.assertTrue(structures.get(key).isPresent(), "Actual world registry is missing slime structure " + key.location());
    }
    List<EntityType<? extends Mob>> types = List.of(TinkerWorld.skySlimeEntity.get(),
      TinkerWorld.enderSlimeEntity.get(), TinkerWorld.terracubeEntity.get());
    java.util.List<Mob> spawned = new java.util.ArrayList<>();
    for (int i = 0; i < types.size(); i++) {
      BlockPos relative = new BlockPos(i * 3, 1, 0);
      helper.setBlock(relative.below(), Blocks.STONE);
      Mob mob = types.get(i).create(level);
      helper.assertTrue(mob != null && mob.getType() == types.get(i),
        "Actual registered slime entity type did not construct its mob");
      mob.moveTo(helper.absolutePos(relative).getX() + 0.5,
        helper.absolutePos(relative).getY(), helper.absolutePos(relative).getZ() + 0.5, 0, 0);
      helper.assertTrue(level.addFreshEntity(mob), "Actual world rejected a registered slime mob");
      spawned.add(mob);
    }
    helper.runAfterDelay(25, () -> {
      for (Mob mob : spawned) {
        helper.assertTrue(mob.isAlive() && mob.tickCount >= 20 && level.getEntity(mob.getId()) == mob,
          "Actual slime mob did not remain alive through native world ticks: " + mob.getType());
      }
      TConstruct.LOG.info("TiC actual slime mobs and registered world features PASS");
      helper.succeed();
    });
  }
}
