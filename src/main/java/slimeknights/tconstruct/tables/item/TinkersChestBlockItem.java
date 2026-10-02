package slimeknights.tconstruct.tables.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.tables.block.entity.chest.TinkersChestBlockEntity;

import javax.annotation.Nullable;

/** Dyeable chest block */
public class TinkersChestBlockItem extends BlockItem {
  public TinkersChestBlockItem(Block blockIn, Properties builder) {
    super(blockIn, builder);
  }

  public int getColor(ItemStack stack) {
    DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
    return color == null ? TinkersChestBlockEntity.DEFAULT_COLOR : color.rgb();
  }

  public boolean hasCustomColor(ItemStack stack) {
    return stack.has(DataComponents.DYED_COLOR);
  }

  public void setColor(ItemStack stack, int color) {
    stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
  }

  public void clearColor(ItemStack stack) {
    stack.remove(DataComponents.DYED_COLOR);
  }

  @Override
  protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
    boolean result = super.updateCustomBlockEntityTag(pos, world, player, stack, state);
    if (hasCustomColor(stack) && world.getBlockEntity(pos) instanceof TinkersChestBlockEntity te) {
      te.setColor(getColor(stack));
    }
    return result;
  }
}
