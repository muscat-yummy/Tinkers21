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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import slimeknights.tconstruct.world.worldgen.islands.IslandPiece;
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

  @GameTest(template = "island", timeoutTicks = 160)
  public static void actualEarthIslandTemplatePlacement(GameTestHelper helper) {
    verifyIslandTemplatePlacement(helper, "earth");
  }

  @GameTest(template = "island", timeoutTicks = 160)
  public static void actualSkyIslandTemplatePlacement(GameTestHelper helper) {
    verifyIslandTemplatePlacement(helper, "sky");
  }

  @GameTest(template = "island", timeoutTicks = 160)
  public static void actualBloodIslandTemplatePlacement(GameTestHelper helper) {
    verifyIslandTemplatePlacement(helper, "blood");
  }

  @GameTest(template = "island", timeoutTicks = 160)
  public static void actualEnderIslandTemplatePlacement(GameTestHelper helper) {
    verifyIslandTemplatePlacement(helper, "ender");
  }

  private static void verifyIslandTemplatePlacement(GameTestHelper helper, String island) {
    var level = helper.getLevel();
    var id = TConstruct.getResource("islands/" + island + "/2x2x4");
    var template = level.getServer().getStructureManager().get(id).orElseThrow();
    BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
    helper.assertTrue(template.placeInWorld(level, origin, origin,
        new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings()
          .setIgnoreEntities(true), RandomSource.create(24711), 2),
      "Actual slime island template failed native world placement: " + id);
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
    TConstruct.LOG.info("Actual {} island template size {} blocks {} owned {} sample {}", island, size, nonAirBlocks, ownedBlocks, observed);
    helper.assertTrue(ownedBlocks >= 500, "Placed island template lost its registered TiC terrain: " + id + "; size " + size + " owned " + ownedBlocks + " sample " + observed);
    TConstruct.LOG.info("TiC actual {} slime island template world placement PASS", island);
    helper.succeed();
  }

  @GameTest(template = "island", timeoutTicks = 160)
  public static void actualIslandGenerationAndPersistence(GameTestHelper helper) {
    var level = helper.getLevel();
    var generator = level.getChunkSource().getGenerator();
    var randomState = level.getChunkSource().randomState();
    var manager = level.getServer().getStructureManager();
    var context = StructurePieceSerializationContext.fromLevel(level);
    var chunk = new ChunkPos(helper.absolutePos(new BlockPos(1, 1, 1)));
    for (var key : List.of(TinkerStructures.earthSlimeIsland, TinkerStructures.skySlimeIsland,
        TinkerStructures.bloodIsland, TinkerStructures.endSlimeIsland)) {
      var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(key);
      var rejected = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
        randomState, manager, 24711L, chunk, 0, level, biome -> false);
      helper.assertTrue(!rejected.isValid(), "Island generation ignored the native biome predicate: " + key.location());
      var start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
        randomState, manager, 24711L, chunk, 0, level, biome -> true);
      helper.assertTrue(start.isValid() && start.getPieces().size() == 1 &&
        start.getPieces().getFirst() instanceof IslandPiece,
        "Native generation did not build its registered island piece: " + key.location());
      var saved = start.createTag(context, chunk);
      var restored = StructureStart.loadStaticStart(context, saved, level.getSeed());
      helper.assertTrue(restored != null && restored.isValid() && restored.getStructure() == structure &&
        restored.getPieces().getFirst() instanceof IslandPiece,
        "Native structure start reload lost the island piece: " + key.location());
      helper.assertTrue(saved.equals(restored.createTag(context, chunk)),
        "Island reload lost template, tree, rotation, mirror or bounds: " + key.location());
    }
    TConstruct.LOG.info("TiC actual island generation biome rejection and structure persistence PASS");
    helper.succeed();
  }

  @GameTest(template = "island", timeoutTicks = 200)
  public static void actualReloadedIslandPieceWorldPlacement(GameTestHelper helper) {
    var level = helper.getLevel();
    var generator = level.getChunkSource().getGenerator();
    var manager = level.getServer().getStructureManager();
    var context = StructurePieceSerializationContext.fromLevel(level);
    var origin = helper.absolutePos(new BlockPos(1, 1, 1));
    var chunk = new ChunkPos(origin);
    for (var key : List.of(TinkerStructures.earthSlimeIsland, TinkerStructures.skySlimeIsland,
        TinkerStructures.bloodIsland, TinkerStructures.endSlimeIsland)) {
      // A fresh footprint prevents a previous island from satisfying the next
      // variant's terrain assertion, and keeps all growth inside this fixture.
      for (int x = 0; x < 33; x++) for (int y = 0; y < 30; y++) for (int z = 0; z < 33; z++) {
        level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
      }
      var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(key);
      var generated = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
        level.getChunkSource().randomState(), manager, 24711L, chunk, 0, level, biome -> true);
      var restored = StructureStart.loadStaticStart(context, generated.createTag(context, chunk), level.getSeed());
      helper.assertTrue(restored != null && restored.isValid(), "Island start could not reload for placement");
      var piece = restored.getPieces().getFirst();
      var oldBounds = piece.getBoundingBox();
      piece.move(origin.getX() - oldBounds.minX(), origin.getY() - oldBounds.minY(), origin.getZ() - oldBounds.minZ());
      var bounds = piece.getBoundingBox();
      restored.placeInChunk(level, level.structureManager(), generator, RandomSource.create(24711), bounds, chunk);
      int owned = 0;
      int markers = 0;
      for (int x = bounds.minX(); x <= bounds.maxX(); x++) for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
          var block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
          if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("tconstruct")) owned++;
          if (block == Blocks.STRUCTURE_BLOCK) markers++;
        }
      }
      helper.assertTrue(owned >= 500 && markers == 0,
        "Actual reloaded island piece lost terrain or placed raw structure markers: " + key.location());
      TConstruct.LOG.info("Actual reloaded {} native piece placement owned {} raw markers {}", key.location(), owned, markers);
    }
    TConstruct.LOG.info("TiC actual reloaded four island pieces native world placement PASS");
    helper.succeed();
  }

  @GameTest(template = "bootstrap", timeoutTicks = 160)
  public static void actualAppliedSlimeBiomeSpawns(GameTestHelper helper) {
    var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
    for (var type : List.of(TinkerWorld.skySlimeEntity.get(), TinkerWorld.terracubeEntity.get(),
        TinkerWorld.enderSlimeEntity.get())) {
      long matchingBiomes = biomes.stream().filter(biome -> biome.getMobSettings()
        .getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(entry -> entry.type == type &&
          entry.minCount == 2 && entry.maxCount == 4)).count();
      helper.assertTrue(matchingBiomes > 0,
        "Loaded biome modifiers did not apply actual natural spawn entries for " + type);
      TConstruct.LOG.info("Actual slime {} has natural spawn entries in {} loaded biomes", type, matchingBiomes);
    }
    TConstruct.LOG.info("TiC actual applied slime natural biome spawn entries PASS");
    helper.succeed();
  }

  @GameTest(template = "smeltery", timeoutTicks = 160)
  public static void actualSlimeNaturalSpawnPlacement(GameTestHelper helper) {
    var level = helper.getLevel();
    var relative = new BlockPos(1, 2, 1);
    var pos = helper.absolutePos(relative);
    helper.setBlock(relative.below(), TinkerWorld.skySlimeGrass.get(FoliageType.SKY));
    helper.assertTrue(SpawnPlacements.checkSpawnRules(TinkerWorld.skySlimeEntity.get(), level,
      MobSpawnType.NATURAL, pos, RandomSource.create(24711)),
      "Native sky slime spawn placement rejected its tagged island soil");
    helper.setBlock(relative.below(), TinkerWorld.enderSlimeGrass.get(FoliageType.ENDER));
    helper.assertTrue(SpawnPlacements.checkSpawnRules(TinkerWorld.enderSlimeEntity.get(), level,
      MobSpawnType.NATURAL, pos, RandomSource.create(24711)),
      "Native ender slime spawn placement rejected its tagged island soil");
    helper.setBlock(relative.below(), Blocks.STONE);
    helper.assertTrue(!SpawnPlacements.checkSpawnRules(TinkerWorld.skySlimeEntity.get(), level,
        MobSpawnType.NATURAL, pos, RandomSource.create(24711)) &&
      !SpawnPlacements.checkSpawnRules(TinkerWorld.enderSlimeEntity.get(), level,
        MobSpawnType.NATURAL, pos, RandomSource.create(24711)),
      "Native island slime spawn placement accepted untagged stone");
    helper.setBlock(relative, Blocks.WATER);
    helper.setBlock(relative.below(), Blocks.WATER);
    helper.assertTrue(SpawnPlacements.checkSpawnRules(TinkerWorld.terracubeEntity.get(), level,
      MobSpawnType.NATURAL, pos, RandomSource.create(24711)),
      "Native terracube spawn placement rejected its actual water habitat");
    TConstruct.LOG.info("TiC actual native slime natural spawn habitat predicates PASS");
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
