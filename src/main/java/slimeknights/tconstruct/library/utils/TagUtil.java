package slimeknights.tconstruct.library.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/** Helpers related to Tag */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagUtil {
  /* Helper functions */

  /**
   * Reads a block position from Tag
   * @param parent  Parent tag
   * @param key     Position key
   * @param offset  Amount to offset position by
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readOptionalPos(CompoundTag parent, String key, BlockPos offset) {
    if (parent.contains(key, Tag.TAG_COMPOUND)) {
      CompoundTag legacy = parent.getCompound(key);
      if (legacy.contains("X", Tag.TAG_ANY_NUMERIC) && legacy.contains("Y", Tag.TAG_ANY_NUMERIC) && legacy.contains("Z", Tag.TAG_ANY_NUMERIC)) {
        return new BlockPos(legacy.getInt("X"), legacy.getInt("Y"), legacy.getInt("Z")).offset(offset);
      }
      return null;
    }
    return NbtUtils.readBlockPos(parent, key).map(pos -> pos.offset(offset)).orElse(null);
  }

  /** Read native int-array positions and legacy X/Y/Z compounds; zero entries remain excluded. */
  public static List<BlockPos> readPosList(CompoundTag parent, String key, BlockPos offset) {
    if (!(parent.get(key) instanceof ListTag list)) {
      return List.of();
    }
    List<BlockPos> positions = new ArrayList<>(list.size());
    for (Tag entry : list) {
      CompoundTag wrapper = new CompoundTag();
      wrapper.put("pos", entry);
      BlockPos pos = readOptionalPos(wrapper, "pos", BlockPos.ZERO);
      if (pos != null && !pos.equals(BlockPos.ZERO)) {
        positions.add(pos.offset(offset));
      }
    }
    return positions;
  }

  /**
   * Checks if the given tag is a numeric type
   * @param tag  Tag to check
   * @return  True if the type matches
   */
  public static boolean isNumeric(Tag tag) {
    byte type = tag.getId();
    return type == Tag.TAG_BYTE || type == Tag.TAG_SHORT || type == Tag.TAG_INT || type == Tag.TAG_LONG || type == Tag.TAG_FLOAT || type == Tag.TAG_DOUBLE;
  }
}
