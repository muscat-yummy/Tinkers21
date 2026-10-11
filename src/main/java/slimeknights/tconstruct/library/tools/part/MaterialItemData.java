package slimeknights.tconstruct.library.tools.part;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/** Keeps the owned material NBT format inside native immutable custom-data components. */
public final class MaterialItemData {
  public static final String KEY = "Material";
  private MaterialItemData() {}

  /** Returns a detached read snapshot; callers cannot mutate the stack through this tag. */
  public static CompoundTag read(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  /** Commits a copied tag through the native component API, retaining unrelated data. */
  public static void update(ItemStack stack, Consumer<CompoundTag> change) {
    if (!stack.isEmpty()) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
    }
  }

  public static void set(ItemStack stack, String material) {
    update(stack, tag -> tag.putString(KEY, material));
  }
}
