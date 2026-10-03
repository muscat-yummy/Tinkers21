package slimeknights.tconstruct.tables.block.entity.inventory;

import net.minecraft.world.item.crafting.CraftingInput;

/** Maps native trimmed crafting inputs back to their original inventory slots. */
public final class CraftingGridMapping {
  private CraftingGridMapping() {}

  public static int slotIndex(CraftingInput.Positioned positioned, int gridWidth, int recipeSlot) {
    CraftingInput input = positioned.input();
    if (gridWidth <= 0 || recipeSlot < 0 || recipeSlot >= input.size()
        || positioned.left() < 0 || positioned.top() < 0
        || positioned.left() + input.width() > gridWidth) {
      throw new IndexOutOfBoundsException("Invalid positioned crafting slot " + recipeSlot);
    }
    return positioned.left() + recipeSlot % input.width()
      + (positioned.top() + recipeSlot / input.width()) * gridWidth;
  }
}
