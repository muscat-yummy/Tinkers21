package slimeknights.tconstruct.tables.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import slimeknights.mantle.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.tables.block.entity.table.CraftingStationBlockEntity;

/**
 * Packet to send the current crafting recipe to a player who opens the crafting station.
 * TODO 1.21: make record.
 */
public class UpdateCraftingRecipePacket implements BlockEntityPacket<CraftingStationBlockEntity> {
  private final BlockPos pos;
  private final ResourceLocation recipe;
  public UpdateCraftingRecipePacket(BlockPos pos, RecipeHolder<CraftingRecipe> recipe) {
    this.pos = pos;
    this.recipe = recipe.id();
  }

  public UpdateCraftingRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.recipe = buffer.readResourceLocation();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeResourceLocation(recipe);
  }

  @Override
  public BlockPos pos() {
    return pos;
  }

  @Override
  public Class<CraftingStationBlockEntity> type() {
    return CraftingStationBlockEntity.class;
  }

  @Override
  public void handleBlockEntity(Context context, CraftingStationBlockEntity be) {
    Level level = be.getLevel();
    assert level != null;
    RecipeHolder<?> holder = level.getRecipeManager().byKey(this.recipe).orElse(null);
    if (holder != null && holder.value() instanceof CraftingRecipe recipe) {
      be.updateRecipe(new RecipeHolder<>(holder.id(), recipe));
    } else {
      TConstruct.LOG.error("Failed to update Crafting Station Recipe at {}: unknown recipe {}", pos, this.recipe);
    }
  }
}
