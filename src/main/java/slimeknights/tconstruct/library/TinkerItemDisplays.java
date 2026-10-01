package slimeknights.tconstruct.library;

import net.minecraft.world.item.ItemDisplayContext;

/** Display contexts registered by META-INF/enumextensions.json before enum initialization. */
public final class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  /** Used by the melter and smeltery for items being melted. */
  public static final ItemDisplayContext MELTER = ItemDisplayContext.valueOf("TCONSTRUCT_MELTER");
  /** Used by the part builder, crafting station, tinkers station and anvil. */
  public static final ItemDisplayContext TABLE = ItemDisplayContext.valueOf("TCONSTRUCT_TABLE");
  /** Used by the casting table. Falls back to the fixed item transform. */
  public static final ItemDisplayContext CASTING_TABLE = ItemDisplayContext.valueOf("TCONSTRUCT_CASTING_TABLE");
  /** Used by the casting basin. */
  public static final ItemDisplayContext CASTING_BASIN = ItemDisplayContext.valueOf("TCONSTRUCT_CASTING_BASIN");
  /** Used by the fluid cannon. Falls back to the fixed item transform. */
  public static final ItemDisplayContext FLUID_CANNON = ItemDisplayContext.valueOf("TCONSTRUCT_FLUID_CANNON");
  /** Used by thrown tools. Falls back to the fixed item transform. */
  public static final ItemDisplayContext THROWN = ItemDisplayContext.valueOf("TCONSTRUCT_THROWN");
}
