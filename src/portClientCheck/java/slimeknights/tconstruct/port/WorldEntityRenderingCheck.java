package slimeknights.tconstruct.port;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.world.client.TerracubeRenderer;
import slimeknights.tconstruct.world.client.TinkerSlimeRenderer;
import slimeknights.tconstruct.world.entity.ArmoredSlimeEntity;

/** Actual server-spawned slime synchronization and native client rendering, isolated to test sources. */
public final class WorldEntityRenderingCheck {
  private record Expected(UUID uuid, EntityType<?> type, boolean metal) {}
  private static volatile List<Expected> expected = List.of();
  private static volatile RuntimeException failure;
  private static boolean requested;
  private static int frames;

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  public static boolean tick(Minecraft minecraft) {
    if (failure != null) throw failure;
    if (!requested) {
      requested = true;
      var server = minecraft.getSingleplayerServer();
      var playerId = minecraft.player.getUUID();
      server.execute(() -> {
        try {
          server.setDifficulty(Difficulty.NORMAL, true);
          var player = server.getPlayerList().getPlayer(playerId);
          var level = player.serverLevel();
          List<Expected> spawned = new ArrayList<>();
          List<EntityType<? extends Slime>> types = List.of(TinkerWorld.skySlimeEntity.get(),
            TinkerWorld.enderSlimeEntity.get(), TinkerWorld.terracubeEntity.get());
          for (var type : types) {
            int variants = type == TinkerWorld.terracubeEntity.get() ? 1 : 2;
            for (int variant = 0; variant < variants; variant++) {
              Slime entity = type.create(level);
              require(entity != null, "Registered slime could not be created in the actual server world");
              entity.setSize(2, true);
              if (variant == 1) {
                CompoundTag saved = entity.saveWithoutId(new CompoundTag());
                saved.putBoolean(ArmoredSlimeEntity.TAG_METAL, true);
                entity.load(saved);
                require(((ArmoredSlimeEntity) entity).isMetal(), "Native slime persistence lost its metal flag");
              }
              entity.setNoAi(true);
              entity.setNoGravity(true);
              entity.moveTo(player.getX() + spawned.size() * 2 - 4, player.getY() + 1, player.getZ() + 4, 0, 0);
              require(level.addFreshEntity(entity), "Actual server rejected its registered slime entity");
              spawned.add(new Expected(entity.getUUID(), type, variant == 1));
            }
          }
          expected = List.copyOf(spawned);
        } catch (Throwable problem) {
          failure = new IllegalStateException("Actual slime client fixture failed", problem);
        }
      });
      return false;
    }
    if (expected.size() != 5 || minecraft.screen != null || minecraft.getOverlay() != null) return false;
    var buffers = minecraft.renderBuffers().bufferSource();
    for (var value : expected) {
      Slime entity = null;
      for (var actual : minecraft.level.entitiesForRendering()) {
        if (actual.getUUID().equals(value.uuid()) && actual instanceof Slime slime) entity = slime;
      }
      if (entity == null || entity.tickCount < 20) { frames = 0; return false; }
      require(entity.isAlive() && entity.getType() == value.type() && entity.getSize() == 2,
        "Actual slime spawn packets lost type, life or tracked size");
      if (entity instanceof ArmoredSlimeEntity armored) {
        require(armored.isMetal() == value.metal(), "Actual slime tracked-data packets lost their metal variant");
      }
      var renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
      require(renderer instanceof TinkerSlimeRenderer || renderer instanceof TerracubeRenderer,
        "Actual slime renderer registration fell back to an unrelated renderer");
      var texture = renderer.getTextureLocation(entity);
      String name = value.type() == TinkerWorld.skySlimeEntity.get() ?
        (value.metal() ? "steel_slime" : "sky_slime") :
        value.type() == TinkerWorld.enderSlimeEntity.get() ?
        (value.metal() ? "knightmetal_slime" : "ender_slime") : "terracube";
      require(texture.equals(TConstruct.getResource("textures/entity/" + name + ".png")) &&
        minecraft.getResourceManager().getResource(texture).isPresent(),
        "Actual slime renderer texture is absent from the reloaded resources");
      renderer.render(entity, 0, 1, new PoseStack(), buffers, 0xF000F0);
    }
    buffers.endBatch();
    if (++frames < 60) return false;
    TConstruct.LOG.info("TiC actual slime spawn packets tracked variants and native rendering PASS");
    return true;
  }
}
