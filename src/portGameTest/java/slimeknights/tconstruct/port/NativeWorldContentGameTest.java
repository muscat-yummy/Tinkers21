package slimeknights.tconstruct.port;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.world.block.FoliageType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.TinkerStructures;
import slimeknights.tconstruct.world.TinkerWorld;

@GameTestHolder("tconstruct")
@PrefixGameTestTemplate(false)
public final class NativeWorldContentGameTest {
  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualEarthSlimeTreePlacement(GameTestHelper helper) {
    var level = helper.getLevel();
    BlockPos origin = new BlockPos(0, 1, 0);
    var soil = TinkerWorld.earthSlimeGrass.get(FoliageType.EARTH);
    helper.setBlock(origin.below(), soil);
    helper.assertTrue(level.getBlockState(helper.absolutePos(origin.below())).is(TinkerTags.Blocks.SLIMY_SOIL),
      "Actual slime soil tag does not include the registered earth slime grass");
    var configured = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
      .getOrThrow(TinkerStructures.earthSlimeTree).value();
    boolean placed = configured.place(level, level.getChunkSource().getGenerator(),
      RandomSource.create(314159), helper.absolutePos(origin));
    helper.assertTrue(placed, "Actual registered earth slime tree feature rejected valid slime soil");
    helper.assertTrue(level.getBlockState(helper.absolutePos(origin)).is(TinkerWorld.greenheart.getLog()),
      "Actual feature did not place its registered greenheart trunk");
    TConstruct.LOG.info("TiC actual configured earth slime tree placement PASS");
    helper.succeed();
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualSkySlimeTreePlacement(GameTestHelper helper) {
    var level = helper.getLevel();
    BlockPos origin = new BlockPos(0, 1, 0);
    helper.setBlock(origin.below(), TinkerWorld.skySlimeGrass.get(FoliageType.SKY));
    helper.assertTrue(level.getBlockState(helper.absolutePos(origin.below())).is(TinkerTags.Blocks.SLIMY_SOIL),
      "Actual sky slime soil is not in the registered slimy soil tag");
    var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
      .getOrThrow(TinkerStructures.skySlimeTree).value();
    helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(),
        RandomSource.create(265358), helper.absolutePos(origin)),
      "Actual sky slime tree feature rejected registered slimy soil");
    helper.assertTrue(level.getBlockState(helper.absolutePos(origin)).is(TinkerWorld.skyroot.getLog()),
      "Actual sky slime tree did not place its registered skyroot trunk");
    TConstruct.LOG.info("TiC actual configured sky slime tree placement PASS");
    helper.succeed();
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualBloodSlimeFungusPlacement(GameTestHelper helper) {
    var level = helper.getLevel();
    BlockPos origin = new BlockPos(0, 1, 0);
    helper.setBlock(origin.below(), TinkerWorld.earthSlimeGrass.get(FoliageType.EARTH));
    helper.assertTrue(level.getBlockState(helper.absolutePos(origin.below())).is(TinkerTags.Blocks.SLIMY_SOIL),
      "Actual fungus soil is not in the registered slimy soil tag");
    var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
      .getOrThrow(TinkerStructures.bloodSlimeFungus).value();
    helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(),
        RandomSource.create(979323), helper.absolutePos(origin)),
      "Actual blood slime fungus feature rejected registered slimy soil");
    helper.assertTrue(level.getBlockState(helper.absolutePos(origin)).is(TinkerWorld.bloodshroom.getLog()),
      "Actual blood slime fungus did not place its registered bloodshroom stem");
    TConstruct.LOG.info("TiC actual configured blood slime fungus placement PASS");
    helper.succeed();
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualSlimeSpawnBiomeModifiers(GameTestHelper helper) {
    var loaded = helper.getLevel().registryAccess().lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
    var keys = List.of(TinkerWorld.spawnOverworldSlime, TinkerWorld.spawnTerracube, TinkerWorld.spawnEndSlime);
    var types = List.of(TinkerWorld.skySlimeEntity.get(), TinkerWorld.terracubeEntity.get(),
      TinkerWorld.enderSlimeEntity.get());
    for (int i = 0; i < keys.size(); i++) {
      var modifier = loaded.getOrThrow(keys.get(i)).value();
      helper.assertTrue(modifier instanceof AddSpawnsBiomeModifier,
        "Actual spawn biome modifier is not a native AddSpawns modifier: " + keys.get(i).location());
      var spawns = ((AddSpawnsBiomeModifier) modifier).spawners();
      var expected = types.get(i);
      helper.assertTrue(spawns.stream().anyMatch(spawn ->
          spawn.type == expected && spawn.minCount == 2 && spawn.maxCount == 4),
        "Actual biome modifier lost its configured slime spawn type and group: " + keys.get(i).location());
    }
    TConstruct.LOG.info("TiC actual slime biome spawn modifiers PASS");
    helper.succeed();
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualIslandTemplatesLoad(GameTestHelper helper) {
    var manager = helper.getLevel().getServer().getStructureManager();
    for (String island : List.of("earth", "sky", "blood", "ender")) {
      for (String size : List.of("0x1x0", "2x2x4", "4x1x6", "8x1x11", "11x1x11")) {
        var id = TConstruct.getResource("islands/" + island + "/" + size);
        helper.assertTrue(manager.get(id).isPresent(),
          "Actual structure template manager could not load island " + id);
      }
    }
    TConstruct.LOG.info("TiC actual twenty slime island structure templates PASS");
    helper.succeed();
  }

  @GameTest(template = "smeltery", timeoutTicks = 160)
  public static void actualEarthIslandTemplatePlacement(GameTestHelper helper) {
    var level = helper.getLevel();
    var id = TConstruct.getResource("islands/earth/2x2x4");
    var template = level.getServer().getStructureManager().get(id).orElseThrow();
    BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
    helper.assertTrue(template.placeInWorld(level, origin, origin,
        new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
          .setIgnoreEntities(true), RandomSource.create(24711), 2),
      "Actual earth slime island template failed native world placement");
    int ownedBlocks = 0;
    int nonAirBlocks = 0;
    StringBuilder observed = new StringBuilder();
    var size = template.getSize();
    for (int x = 0; x < size.getX(); x++) for (int y = 0; y < size.getY(); y++) for (int z = 0; z < size.getZ(); z++) {
      var block = level.getBlockState(origin.offset(x, y, z)).getBlock();
      if (block != Blocks.AIR) {
        nonAirBlocks++;
        var blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        if (observed.length() < 500) observed.append(blockId).append(' ');
        if (blockId.getNamespace().equals("tconstruct")) ownedBlocks++;
      }
    }
    TConstruct.LOG.info("Actual earth island template size {} blocks {} owned {} sample {}", size, nonAirBlocks, ownedBlocks, observed);
    helper.assertTrue(ownedBlocks > 0, "Placed island template contains no registered TiC blocks; size " + size + " sample " + observed);
    TConstruct.LOG.info("TiC actual earth slime island template world placement PASS");
    helper.succeed();
  }

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
    TConstruct.LOG.info("TiC actual registered world features PASS");
    helper.succeed();
  }

  private static void verifySlimeMob(GameTestHelper helper, EntityType<? extends Mob> type, String name) {
    var level = helper.getLevel();
    BlockPos relative = new BlockPos(0, 1, 0);
    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
      helper.setBlock(relative.below().offset(dx, 0, dz), Blocks.STONE);
    }
    Mob mob = type.create(level);
    helper.assertTrue(mob != null && mob.getType() == type,
      "Actual registered slime entity type did not construct its mob: " + name);
    mob.moveTo(helper.absolutePos(relative).getX() + 0.5,
      helper.absolutePos(relative).getY(), helper.absolutePos(relative).getZ() + 0.5, 0, 0);
    helper.assertTrue(level.addFreshEntity(mob), "Actual world rejected registered slime mob: " + name);
    helper.runAfterDelay(25, () -> {
      TConstruct.LOG.info("Actual mob {} difficulty {} alive {} removed {} ticks {} tracked {}",
        mob.getType(), level.getDifficulty(), mob.isAlive(), mob.getRemovalReason(),
        mob.tickCount, level.getEntity(mob.getId()) == mob);
      helper.assertTrue(mob.isAlive() && mob.tickCount >= 1 && level.getEntity(mob.getId()) == mob,
        "Actual slime mob did not remain alive through native world ticks: " + name);
      TConstruct.LOG.info("TiC actual " + name + " entity world ticks PASS");
      helper.succeed();
    });
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualSkySlimeEntity(GameTestHelper helper) {
    verifySlimeMob(helper, TinkerWorld.skySlimeEntity.get(), "sky slime");
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualEnderSlimeEntity(GameTestHelper helper) {
    verifySlimeMob(helper, TinkerWorld.enderSlimeEntity.get(), "ender slime");
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualTerracubeEntity(GameTestHelper helper) {
    verifySlimeMob(helper, TinkerWorld.terracubeEntity.get(), "terracube");
  }
}
