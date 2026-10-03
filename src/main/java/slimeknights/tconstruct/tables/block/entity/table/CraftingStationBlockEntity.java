package slimeknights.tconstruct.tables.block.entity.table;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import slimeknights.tconstruct.tables.block.entity.inventory.CraftingGridMapping;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.shared.inventory.ConfigurableInvWrapperCapability;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.CraftingContainerWrapper;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer.ILazyCrafter;
import slimeknights.tconstruct.tables.menu.CraftingStationContainerMenu;
import slimeknights.tconstruct.tables.network.UpdateCraftingRecipePacket;

import javax.annotation.Nullable;
import java.util.Collections;

public class CraftingStationBlockEntity extends RetexturedTableBlockEntity implements ILazyCrafter {
  public static final Component UNCRAFTABLE = TConstruct.makeTranslation("gui", "crafting_station.uncraftable");
  private static final Component NAME = TConstruct.makeTranslation("gui", "crafting_station");

  /** Last crafted crafting recipe */
  @Nullable
  private RecipeHolder<CraftingRecipe> lastRecipe;
  /** Result inventory, lazy loads results */
  @Getter
  private final LazyResultContainer craftingResult;
  /** Crafting inventory for the recipe calls */
  private final CraftingContainerWrapper craftingInventory;

  public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
    super(TinkerTables.craftingStationTile.get(), pos, state, NAME, 9);
    this.itemHandler = new ConfigurableInvWrapperCapability(this, false, false);
    this.craftingInventory = new CraftingContainerWrapper(this, 3, 3);
    this.craftingResult = new LazyResultContainer(this);
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int menuId, Inventory playerInventory, Player playerEntity) {
    return new CraftingStationContainerMenu(menuId, playerInventory, this);
  }

  @Override
  public AABB getRenderBoundingBox() {
    return new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(worldPosition), net.minecraft.world.phys.Vec3.atLowerCornerOf(worldPosition.offset(1, 2, 1)));
  }

  /* Crafting */

  @Override
  public ItemStack calcResult(@Nullable Player player) {
    if (this.level == null || isEmpty()) {
      return ItemStack.EMPTY;
    }
    CraftingInput input = craftingInventory.asCraftInput();
    if (!this.level.isClientSide && this.level.getServer() != null) {
      RecipeManager manager = this.level.getServer().getRecipeManager();
      Player previousPlayer = CommonHooks.getCraftingPlayer();
      CommonHooks.setCraftingPlayer(player);
      try {
        RecipeHolder<CraftingRecipe> recipe = lastRecipe;
        if (recipe == null || !recipe.value().matches(input, this.level)) {
          recipe = manager.getRecipeFor(RecipeType.CRAFTING, input, this.level).orElse(null);
        }
        if (recipe != null) {
          ItemStack result = recipe.value().assemble(input, level.registryAccess());
          if (recipe != lastRecipe) {
            this.lastRecipe = recipe;
            this.syncToRelevantPlayers(this::syncRecipe);
          }
          return result;
        }
      } finally {
        CommonHooks.setCraftingPlayer(previousPlayer);
      }
    } else if (this.lastRecipe != null && this.lastRecipe.value().matches(input, this.level)) {
      Player previousPlayer = CommonHooks.getCraftingPlayer();
      CommonHooks.setCraftingPlayer(player);
      try {
        return this.lastRecipe.value().assemble(input, level.registryAccess());
      } finally {
        CommonHooks.setCraftingPlayer(previousPlayer);
      }
    }
    return ItemStack.EMPTY;
  }

  /**
   * Gets the player sensitive crafting result, also validating the player has access to this recipe
   * @param player  Player
   * @return  Player sensitive result
   */
  public ItemStack getResultForPlayer(Player player) {
    Player previousPlayer = CommonHooks.getCraftingPlayer();
    CommonHooks.setCraftingPlayer(player);
    try {
      RecipeHolder<CraftingRecipe> recipe = this.lastRecipe;
      if (recipe == null || this.level == null) {
        return ItemStack.EMPTY;
      }
      CraftingInput input = craftingInventory.asCraftInput();
      if (!recipe.value().matches(input, level)) {
        return ItemStack.EMPTY;
      }
      return recipe.value().assemble(input, level.registryAccess());
    } finally {
      CommonHooks.setCraftingPlayer(previousPlayer);
    }
  }

  /**
   * Removes the result from this inventory, updating inputs and triggering recipe hooks
   * @param player  Player taking result
   * @param result  Result removed
   * @param amount  Number of times crafted
   */
  public void takeResult(Player player, ItemStack result, int amount) {
    RecipeHolder<CraftingRecipe> recipe = this.lastRecipe; // preserve the identity used for recipe awards and synchronization
    if (recipe == null || this.level == null) {
      return;
    }

    // fire crafting events
    if (!recipe.value().isSpecial()) {
      // unlock the recipe if it was not unlocked, so it shows in the recipe book
      player.awardRecipes(Collections.singleton(recipe));
    }
    result.onCraftedBy(this.level, player, amount);
    EventHooks.firePlayerCraftingEvent(player, result, this.craftingInventory);

    // update all slots in the inventory
    // remove remaining items
    CraftingInput.Positioned positioned = craftingInventory.asPositionedCraftInput();
    NonNullList<ItemStack> remaining;
    Player previousPlayer = CommonHooks.getCraftingPlayer();
    CommonHooks.setCraftingPlayer(player);
    try {
      remaining = recipe.value().getRemainingItems(positioned.input());
    } finally {
      CommonHooks.setCraftingPlayer(previousPlayer);
    }
    for (int i = 0; i < remaining.size(); ++i) {
      int gridSlot = CraftingGridMapping.slotIndex(positioned, craftingInventory.getWidth(), i);
      ItemStack original = this.getItem(gridSlot);
      ItemStack newStack = remaining.get(i);

      // if empty or size 1, set directly (decreases by 1)
      if (original.isEmpty() || original.getCount() == 1) {
        this.setItem(gridSlot, newStack);
      }
      else if (ItemStack.isSameItemSameComponents(original, newStack)) {
        // if matching, merge (decreasing by 1
        newStack.grow(original.getCount() - 1);
        this.setItem(gridSlot, newStack);
      }
      else {
        // directly update the slot
        this.setItem(gridSlot, original.copyWithCount(original.getCount() - 1));
        // otherwise, drop the item as the player
        if (!newStack.isEmpty() && !player.getInventory().add(newStack)) {
          player.drop(newStack, false);
        }
      }
    }
  }

  /** Sends a message alerting the player this item is currently uncraftable, typically due to gamerules */
  public void notifyUncraftable(Player player) {
    // if empty, send a message so the player is more aware of why they cannot craft it, sent to chat as status bar is not visible
    // TODO: consider moving into the UI somewhere
    if (level != null && !level.isClientSide) {
      player.displayClientMessage(CraftingStationBlockEntity.UNCRAFTABLE, false);
    }
  }

  @Override
  public void onCraft(Player player, ItemStack result, int amount) {
    // update the inputs and trigger recipe hooks
    if (amount != 0 && !result.isEmpty()) {
      takeResult(player, result, amount);
    }
  }

  @Override
  public void setItem(int slot, ItemStack itemstack) {
    super.setItem(slot, itemstack);
    // clear the crafting result when the matrix changes so we recalculate the result
    this.craftingResult.clearContent();
  }


  /* Syncing */

  /**
   * Sends the current recipe to the given player
   * @param player  Player to send an update to
   */
  public void syncRecipe(Player player) {
    // must have a last recipe and a server world
    if (this.lastRecipe != null && this.level != null && !this.level.isClientSide && player instanceof ServerPlayer) {
      TinkerNetwork.getInstance().sendTo(new UpdateCraftingRecipePacket(this.worldPosition, this.lastRecipe), (ServerPlayer) player);
    }
  }

  /**
   * Updates the recipe from the server
   * @param recipe  New recipe
   */
  public void updateRecipe(RecipeHolder<CraftingRecipe> recipe) {
    this.lastRecipe = recipe;
    this.craftingResult.clearContent();
  }
}
