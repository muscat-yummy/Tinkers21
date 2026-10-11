package slimeknights.tconstruct.port;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import slimeknights.tconstruct.library.utils.ItemAttributeData;

import java.util.List;
import java.util.Map;

public final class ItemAttributeDataSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    check(ItemAttributeData.forSlot(Items.DIAMOND_HELMET.getDefaultAttributeModifiers(), EquipmentSlot.HEAD)
      .get(Attributes.ARMOR).stream().anyMatch(modifier ->
        modifier.id().equals(ResourceLocation.parse("minecraft:armor.helmet"))),
      "Armor ID no longer matches the native per-type identity");
    var damage = new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 9, AttributeModifier.Operation.ADD_VALUE);
    var offhand = new AttributeModifier(ResourceLocation.parse("tconstruct:test_offhand_armor"), 2,
      AttributeModifier.Operation.ADD_VALUE);
    var helmet = new AttributeModifier(ResourceLocation.parse("minecraft:armor.helmet"), 3,
      AttributeModifier.Operation.ADD_VALUE);
    var attributes = ItemAttributeData.build(
      List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD), slot -> switch (slot) {
        case MAINHAND -> ImmutableMultimap.of(Attributes.ATTACK_DAMAGE.value(), damage);
        case OFFHAND -> ImmutableMultimap.of(Attributes.ARMOR.value(), offhand);
        case HEAD -> ImmutableMultimap.of(Attributes.ARMOR.value(), helmet);
        default -> ImmutableMultimap.of();
      }).withTooltip(false);
    check(ItemAttributeData.forSlot(attributes, EquipmentSlot.MAINHAND)
      .get(Attributes.ATTACK_DAMAGE).contains(damage), "Main-hand damage or registry holder lost");
    check(ItemAttributeData.forSlot(attributes, EquipmentSlot.OFFHAND).get(Attributes.ARMOR).contains(offhand),
      "Off-hand modifier lost");
    check(ItemAttributeData.forSlot(attributes, EquipmentSlot.HEAD).get(Attributes.ARMOR).contains(helmet),
      "Armor modifier lost");
    check(ItemAttributeData.forSlot(attributes, EquipmentSlot.MAINHAND).size() == 1
      && ItemAttributeData.forSlot(attributes, EquipmentSlot.OFFHAND).size() == 1
      && ItemAttributeData.forSlot(attributes, EquipmentSlot.CHEST).isEmpty()
      && ItemAttributeData.forSlot(attributes, EquipmentSlot.BODY).isEmpty(),
      "Native equipment slot scope broadened");
    check(ItemAttributeData.build(List.of(EquipmentSlot.MAINHAND), slot -> ImmutableMultimap.of())
      .modifiers().isEmpty(), "Broken/empty tool attributes did not stay empty");
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    var ops = registries.createSerializationContext(JsonOps.INSTANCE);
    var encoded = ItemAttributeModifiers.CODEC.encodeStart(ops, attributes).getOrThrow();
    check(ItemAttributeModifiers.CODEC.parse(ops, encoded).getOrThrow().equals(attributes),
      "Native attribute JSON changed stable IDs, holders, groups or tooltip policy");
    ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
    stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
    try {
      ItemStack.STREAM_CODEC.encode(buffer, stack);
      var decoded = ItemStack.STREAM_CODEC.decode(buffer);
      check(buffer.readableBytes() == 0 && decoded.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(attributes),
        "Native attribute item wire changed IDs, groups, amounts or tooltip policy");
    } finally { buffer.release(); }
    System.out.println("Native tool attributes, registered holders, exact hand/armor slots, stable IDs and JSON/wire: PASS");
  }
}
