// Credit to Immersive Engineering and blusunrize for this class
// See: https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18/src/main/java/blusunrize/immersiveengineering/common/util/fakeworld/FakeSpawnInfo.java
package slimeknights.mantle.client.book.structure.level;

import net.minecraft.world.Difficulty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.WritableLevelData;

public class FakeLevelData implements WritableLevelData {

  private static final GameRules RULES = new GameRules();

  private int spawnX;
  private int spawnY;
  private int spawnZ;
  private float spawnAngle;

  public void setXSpawn(int x) {
    this.spawnX = x;
  }

  public void setYSpawn(int y) {
    this.spawnY = y;
  }

  public void setZSpawn(int z) {
    this.spawnZ = z;
  }

  public void setSpawnAngle(float angle) {
    this.spawnAngle = angle;
  }

  public int getXSpawn() {
    return this.spawnX;
  }

  public int getYSpawn() {
    return this.spawnY;
  }

  public int getZSpawn() {
    return this.spawnZ;
  }

  @Override
  public BlockPos getSpawnPos() {
    return new BlockPos(spawnX, spawnY, spawnZ);
  }

  @Override
  public void setSpawn(BlockPos pos, float angle) {
    this.spawnX = pos.getX();
    this.spawnY = pos.getY();
    this.spawnZ = pos.getZ();
    this.spawnAngle = angle;
  }

  @Override
  public float getSpawnAngle() {
    return this.spawnAngle;
  }

  @Override
  public long getGameTime() {
    return 0;
  }

  @Override
  public long getDayTime() {
    return 0;
  }

  @Override
  public boolean isThundering() {
    return false;
  }

  @Override
  public boolean isRaining() {
    return false;
  }

  @Override
  public void setRaining(boolean isRaining) {

  }

  @Override
  public boolean isHardcore() {
    return false;
  }

  @Override
  public GameRules getGameRules() {
    return RULES;
  }

  @Override
  public Difficulty getDifficulty() {
    return Difficulty.PEACEFUL;
  }

  @Override
  public boolean isDifficultyLocked() {
    return false;
  }
}
