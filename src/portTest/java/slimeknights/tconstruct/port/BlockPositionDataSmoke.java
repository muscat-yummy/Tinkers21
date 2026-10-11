package slimeknights.tconstruct.port;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.StringTag;
import slimeknights.tconstruct.library.utils.TagUtil;

import java.util.List;

public final class BlockPositionDataSmoke {
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    BlockPos offset = new BlockPos(-1000, 63, 4000);
    BlockPos relative = new BlockPos(-7, -12, 15);
    CompoundTag nativeRoot = new CompoundTag();
    ListTag nativeList = new ListTag();
    nativeList.add(NbtUtils.writeBlockPos(relative));
    nativeList.add(NbtUtils.writeBlockPos(BlockPos.ZERO));
    nativeList.add(new IntArrayTag(new int[]{1, 2}));
    nativeRoot.put("extra", nativeList);
    CompoundTag snapshot = nativeRoot.copy();
    check(TagUtil.readPosList(nativeRoot, "extra", offset).equals(List.of(relative.offset(offset))),
      "Native relative positions or invalid/zero filtering changed");
    check(nativeRoot.equals(snapshot), "Reading native positions changed input data");

    ListTag legacyList = new ListTag();
    CompoundTag legacy = new CompoundTag();
    legacy.putInt("X", relative.getX());
    legacy.putInt("Y", relative.getY());
    legacy.putInt("Z", relative.getZ());
    legacyList.add(legacy);
    legacyList.add(new CompoundTag());
    CompoundTag legacyRoot = new CompoundTag();
    legacyRoot.put("extra", legacyList);
    check(TagUtil.readPosList(legacyRoot, "extra", offset).equals(TagUtil.readPosList(nativeRoot, "extra", offset)),
      "Legacy saved positions differ from native positions");
    legacyRoot.put("extra", StringTag.valueOf("broken"));
    check(TagUtil.readPosList(legacyRoot, "extra", offset).isEmpty(), "Malformed list was accepted");
    check(TagUtil.readPosList(new CompoundTag(), "extra", offset).isEmpty(), "Absent positions were invented");

    nativeRoot.put("min", NbtUtils.writeBlockPos(relative));
    check(relative.offset(offset).equals(TagUtil.readOptionalPos(nativeRoot, "min", offset)),
      "Native bounds did not retain the controller offset");
    System.out.println("BlockPositionDataSmoke passed");
  }
}
