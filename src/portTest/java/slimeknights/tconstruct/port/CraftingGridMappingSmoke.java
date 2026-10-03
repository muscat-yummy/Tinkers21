package slimeknights.tconstruct.port;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import slimeknights.tconstruct.tables.block.entity.inventory.CraftingContainerWrapper;
import slimeknights.tconstruct.tables.block.entity.inventory.CraftingGridMapping;

public final class CraftingGridMappingSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    var recipe = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.IRON_INGOT),
      NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.WATER_BUCKET), Ingredient.of(Items.STICK)));
    int cases = 0;
    for (int width = 1; width <= 2; width++) {
      for (int left = 0; left + width <= 3; left++) {
        for (int top = 0; top < 2; top++) {
          var inventory = new SimpleContainer(9);
          int bucketSlot = left + top * 3;
          int stickSlot = left + width - 1 + (top + 1) * 3;
          var bucket = new ItemStack(Items.WATER_BUCKET);
          bucket.set(DataComponents.CUSTOM_NAME, Component.literal("Named input"));
          inventory.setItem(bucketSlot, bucket);
          inventory.setItem(stickSlot, new ItemStack(Items.STICK, 7));
          var wrapper = new CraftingContainerWrapper(inventory, 3, 3);
          var positioned = wrapper.asPositionedCraftInput();
          var input = positioned.input();
          check(positioned.left() == left && positioned.top() == top
            && input.width() == width && input.height() == 2, "Native trim did not retain grid offsets");
          check(recipe.matches(input, null), "Native shapeless recipe did not match offset inputs");
          var remaining = recipe.getRemainingItems(input);
          var originalBucket = inventory.getItem(bucketSlot).copy();
          int buckets = 0;
          for (int i = 0; i < remaining.size(); i++) {
            int gridSlot = CraftingGridMapping.slotIndex(positioned, 3, i);
            check(ItemStack.matches(input.getItem(i), inventory.getItem(gridSlot)), "Compact recipe input mapped to the wrong original slot");
            if (remaining.get(i).is(Items.BUCKET)) {
              check(gridSlot == bucketSlot, "Crafting remainder moved to the wrong inventory slot");
              buckets++;
            }
          }
          check(buckets == 1 && ItemStack.matches(originalBucket, inventory.getItem(bucketSlot))
            && inventory.getItem(stickSlot).getCount() == 7, "Recipe remainder lookup mutated inputs or duplicated buckets");
          cases++;
        }
      }
    }
    var empty = new CraftingContainerWrapper(new SimpleContainer(9), 3, 3).asPositionedCraftInput();
    boolean rejected = false;
    try { CraftingGridMapping.slotIndex(empty, 3, 0); } catch (IndexOutOfBoundsException expected) { rejected = true; }
    check(rejected, "Empty trimmed grid must reject nonexistent recipe slots");
    System.out.println("Native trimmed crafting input and remainder slot mapping, offsets, components and unchanged inputs (" + cases + " layouts): PASS");
  }
}
