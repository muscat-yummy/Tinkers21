package slimeknights.tconstruct.library.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Owner-aware native capability lookup, cached on the server and queried on the client. */
public final class BlockCapabilitySource<T> implements Supplier<T> {
  private final BlockEntity owner;
  private final BlockCapability<T,Direction> capability;
  private final BlockPos pos;
  @Nullable
  private final Direction side;
  @Nullable
  private BlockCapabilityCache<T,Direction> cache;

  public BlockCapabilitySource(BlockEntity owner, BlockCapability<T,Direction> capability, BlockPos pos, @Nullable Direction side) {
    this.owner = owner;
    this.capability = capability;
    this.pos = pos.immutable();
    this.side = side;
  }

  private boolean isValid(ServerLevel level) {
    if (owner.isRemoved() || owner.getLevel() != level) {
      if (cache != null && cache.level() == level) {
        cache = null;
      }
      return false;
    }
    return true;
  }

  @Nullable
  @Override
  public T get() {
    Level level = owner.getLevel();
    if (level == null || owner.isRemoved()) {
      cache = null;
      return null;
    }
    if (level instanceof ServerLevel serverLevel) {
      if (cache == null || cache.level() != serverLevel) {
        cache = BlockCapabilityCache.create(capability, serverLevel, pos, side,
          () -> isValid(serverLevel), () -> {});
      }
      return cache.getCapability();
    }
    // Client-side gauges must observe replacement handlers too; server caches are not available here.
    return level.hasChunkAt(pos) ? level.getCapability(capability, pos, side) : null;
  }

  public T getOrDefault(T fallback) {
    T result = get();
    return result != null ? result : fallback;
  }
}
