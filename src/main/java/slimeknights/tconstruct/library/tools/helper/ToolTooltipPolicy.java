package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Read-only tooltip policy, retaining explicit legacy HideFlags while using native display hooks. */
public final class ToolTooltipPolicy {
  private ToolTooltipPolicy() {}

  private static int flags(ItemStack stack, int defaults) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag tag = data.copyTag();
      if (tag.contains("HideFlags", Tag.TAG_ANY_NUMERIC)) {
        return tag.getInt("HideFlags");
      }
    }
    return defaults;
  }

  public static boolean hideEnchantments(ItemStack stack) {
    return (flags(stack, 1) & 1) != 0;
  }

  public static boolean hideAttributes(ItemStack stack, boolean detailedTooltip) {
    return (flags(stack, detailedTooltip ? 3 : 1) & 2) != 0;
  }
}
