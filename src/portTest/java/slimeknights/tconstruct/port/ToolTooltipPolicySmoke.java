package slimeknights.tconstruct.port;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import slimeknights.tconstruct.library.tools.helper.ToolTooltipPolicy;

public final class ToolTooltipPolicySmoke {
  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    ItemStack stack = new ItemStack(Items.STICK);
    if (!ToolTooltipPolicy.hideEnchantments(stack) || ToolTooltipPolicy.hideAttributes(stack, false)
        || !ToolTooltipPolicy.hideAttributes(stack, true)) {
      throw new AssertionError("Default tool visibility and detailed tooltip behavior changed");
    }
    for (int flags : new int[] {0, 1, 2, 3, 64, 127}) {
      CompoundTag data = new CompoundTag();
      data.putInt("HideFlags", flags);
      data.putString("tool_data", "retained");
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
      var before = stack.getComponentsPatch();
      for (boolean detailed : new boolean[] {false, true}) {
        if (ToolTooltipPolicy.hideEnchantments(stack) != ((flags & 1) != 0)
            || ToolTooltipPolicy.hideAttributes(stack, detailed) != ((flags & 2) != 0)) {
          throw new AssertionError("Explicit legacy HideFlags must override tool defaults");
        }
      }
      if (!before.equals(stack.getComponentsPatch())) {
        throw new AssertionError("Tooltip queries changed item components");
      }
    }
    CompoundTag invalid = new CompoundTag();
    invalid.putString("HideFlags", "invalid");
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(invalid));
    if (!ToolTooltipPolicy.hideEnchantments(stack) || !ToolTooltipPolicy.hideAttributes(stack, true)) {
      throw new AssertionError("Nonnumeric legacy flags must use defaults");
    }
    System.out.println("Native read-only tool tooltip policy, explicit legacy flags and component snapshots: PASS");
  }
}
