package slimeknights.tconstruct.library.utils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.function.Function;

/** Converts tool modifier output to native attributes without broadening its equipment slots. */
public final class ItemAttributeData {
  private ItemAttributeData() {}

  public static ItemAttributeModifiers build(Iterable<EquipmentSlot> slots,
      Function<EquipmentSlot,Multimap<Attribute,AttributeModifier>> attributes) {
    ItemAttributeModifiers.Builder result = ItemAttributeModifiers.builder();
    for (EquipmentSlot slot : slots) {
      EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
      attributes.apply(slot).forEach((attribute, modifier) ->
        result.add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), modifier, group));
    }
    return result.build();
  }

  public static Multimap<Holder<Attribute>,AttributeModifier> forSlot(
      ItemAttributeModifiers attributes, EquipmentSlot slot) {
    ImmutableMultimap.Builder<Holder<Attribute>,AttributeModifier> result = ImmutableMultimap.builder();
    attributes.forEach(slot, result::put);
    return result.build();
  }
}
