package slimeknights.tconstruct.tables.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import slimeknights.mantle.network.NetworkEvent.Context;
import slimeknights.mantle.network.packet.BlockEntityPacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import net.minecraft.world.item.crafting.RecipeHolder;
import javax.annotation.Nullable;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;

/**
 * Packet to send the current crafting recipe to a player who opens the tinker station.
 * TODO 1.21: make record.
 */
public class UpdateTinkerStationRecipePacket implements IThreadsafePacket {
  private final BlockPos pos;
  @Nullable
  private final ResourceLocation recipe;
  public UpdateTinkerStationRecipePacket(BlockPos pos, @Nullable RecipeHolder<ITinkerStationRecipe> recipe) {
    this.pos = pos;
    this.recipe = recipe == null ? null : recipe.id();
  }

  public UpdateTinkerStationRecipePacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.recipe = buffer.readBoolean() ? buffer.readResourceLocation() : null;
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    buffer.writeBoolean(recipe != null);
    if (recipe != null) buffer.writeResourceLocation(recipe);
  }

  @Override
  public void handleThreadsafe(Context context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(UpdateTinkerStationRecipePacket packet) {
      Minecraft mc = Minecraft.getInstance();
      Level world = mc.level;
      if (world != null) {
        // start by fetching the recipe, no further work if it's missing
        RecipeHolder<ITinkerStationRecipe> recipe = null;
        if (packet.recipe != null) {
          RecipeHolder<?> holder = world.getRecipeManager().byKey(packet.recipe).orElse(null);
          if (holder != null && holder.value() instanceof ITinkerStationRecipe stationRecipe) {
            recipe = new RecipeHolder<>(holder.id(), stationRecipe);
          }
        }
        if (packet.recipe != null && recipe == null) {
          TConstruct.LOG.error("Failed to update Tinker Station Recipe at {}: unknown recipe {}", packet.pos, packet.recipe);
          return;
        }
        // if the screen is open, use that to get the TE and update the screen as we want to update the screen too
        if (mc.screen instanceof TinkerStationScreen stationScreen) {
          TinkerStationBlockEntity te = stationScreen.getTileEntity();
          if (te != null && te.getBlockPos().equals(packet.pos)) {
            te.updateRecipeHolder(recipe);
            stationScreen.updateDisplay();
            return;
          }
        }
        // if the wrong screen is open or no screen, use the tile directly
        if (BlockEntityPacket.getBlockEntity(world, packet.pos, packet) instanceof TinkerStationBlockEntity be) {
          be.updateRecipeHolder(recipe);
        } else {
          TConstruct.LOG.error("Failed to update Tinker Station Recipe at {} to {}: unable to find Tinker Station", packet.pos, packet.recipe);
        }
      }
    }
  }
}
