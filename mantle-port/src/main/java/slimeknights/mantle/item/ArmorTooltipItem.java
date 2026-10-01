package slimeknights.mantle.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import slimeknights.mantle.util.TranslationHelper;

import javax.annotation.Nullable;
import java.util.List;

public class ArmorTooltipItem extends ArmorItem {

  public ArmorTooltipItem(Holder<ArmorMaterial> armorMaterial, ArmorItem.Type type, Properties builder) {
    super(armorMaterial, type, builder);
  }

  /** Legacy addon overload for a registered material value. */
  public ArmorTooltipItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
    this(BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(material), type, properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
    TranslationHelper.addOptionalTooltip(stack, tooltip);
    super.appendHoverText(stack, context, tooltip, flagIn);
  }
}
