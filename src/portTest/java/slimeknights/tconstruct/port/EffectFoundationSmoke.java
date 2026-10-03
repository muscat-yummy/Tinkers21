package slimeknights.tconstruct.port;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.EffectCures;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.tools.modifiers.effect.NoMilkEffect;

public final class EffectFoundationSmoke {
  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    var effect = new TinkerEffect(MobEffectCategory.HARMFUL, true);
    String uuid = "68ee3026-1d50-4eb4-914e-a8b05fbfdb71";
    effect.addAttributeModifier(Attributes.MOVEMENT_SPEED, uuid, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    List<AttributeModifier> modifiers = new ArrayList<>();
    effect.createModifiers(2, (attribute, modifier) -> {
      if (!attribute.equals(Attributes.MOVEMENT_SPEED)) {
        throw new AssertionError("Native attribute holder changed");
      }
      modifiers.add(modifier);
    });
    if (modifiers.size() != 1 || modifiers.getFirst().amount() != -0.75
        || !modifiers.getFirst().id().toString().equals("tconstruct:legacy_effect/" + uuid)
        || modifiers.getFirst().operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
      throw new AssertionError("Legacy effect modifier ID, level scaling or native operation changed");
    }
    var same = new TinkerEffect(MobEffectCategory.HARMFUL, true);
    same.addAttributeModifier(Attributes.MOVEMENT_SPEED.value(), uuid, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    List<AttributeModifier> raw = new ArrayList<>();
    same.createModifiers(2, (attribute, modifier) -> raw.add(modifier));
    if (!raw.equals(modifiers)) {
      throw new AssertionError("Legacy raw attribute and native holder templates differ");
    }
    try {
      effect.addAttributeModifier(Attributes.MOVEMENT_SPEED, "invalid", 1, AttributeModifier.Operation.ADD_VALUE);
      throw new AssertionError("Invalid legacy UUID accepted");
    } catch (IllegalArgumentException expected) {
      // The legacy contract also required a UUID.
    }
    var attributeId = slimeknights.tconstruct.library.utils.AttributeIdUtil.fromUuid(java.util.UUID.fromString(uuid));
    if (!attributeId.equals(modifiers.getFirst().id())) {
      throw new AssertionError("Effect and equipment legacy UUIDs must identify the same native modifier");
    }
    var attributeInstance = new net.minecraft.world.entity.ai.attributes.AttributeInstance(
      Attributes.MOVEMENT_SPEED, changed -> {});
    attributeInstance.setBaseValue(10);
    attributeInstance.addTransientModifier(new AttributeModifier(attributeId, 2, AttributeModifier.Operation.ADD_VALUE));
    if (attributeInstance.getValue() != 12) {
      throw new AssertionError("Native legacy attribute modifier was not applied");
    }
    attributeInstance.removeModifier(attributeId);
    attributeInstance.addTransientModifier(new AttributeModifier(attributeId, 4, AttributeModifier.Operation.ADD_VALUE));
    if (attributeInstance.getValue() != 14 || attributeInstance.getModifiers().size() != 1) {
      throw new AssertionError("Legacy attribute update stacked instead of replacing its stable ID");
    }
    attributeInstance.removeModifier(attributeId);
    if (attributeInstance.getValue() != 10 || !attributeInstance.getModifiers().isEmpty()) {
      throw new AssertionError("Legacy attribute unequip did not restore the native base value");
    }
    System.out.println("Shared legacy effect/equipment IDs and native attribute replace/remove lifecycle: PASS");

    var normal = new MobEffectInstance(Holder.direct(new TinkerEffect(MobEffectCategory.HARMFUL, true)), 20);
    var noMilk = new MobEffectInstance(Holder.direct(new NoMilkEffect(MobEffectCategory.HARMFUL, 0xffffff, true)), 20);
    if (!normal.getCures().contains(EffectCures.MILK) || noMilk.getCures().contains(EffectCures.MILK)
        || !noMilk.getCures().contains(EffectCures.PROTECTED_BY_TOTEM)) {
      throw new AssertionError("Milk immunity must retain totem removal");
    }
    if (new MobEffectInstance(noMilk).getCures().contains(EffectCures.MILK)) {
      throw new AssertionError("Effect copy lost milk immunity");
    }
    var custom = new MobEffectInstance(normal);
    var addon = net.neoforged.neoforge.common.EffectCure.get("othermod:custom");
    custom.getCures().add(addon);
    slimeknights.tconstruct.library.utils.EffectCureUtil.setItemCures(custom, List.of(net.minecraft.world.item.Items.STICK));
    var stick = slimeknights.tconstruct.library.utils.EffectCureUtil.forItem(net.minecraft.world.item.Items.STICK);
    if (custom.getCures().contains(EffectCures.MILK) || !custom.getCures().contains(stick)
        || !custom.getCures().contains(EffectCures.PROTECTED_BY_TOTEM) || !custom.getCures().contains(addon)) {
      throw new AssertionError("Custom item cure lost non-item cures or retained milk");
    }
    var copied = new MobEffectInstance(custom);
    if (!copied.getCures().contains(stick)
        || net.neoforged.neoforge.common.EffectCure.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
          net.neoforged.neoforge.common.EffectCure.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, stick).getOrThrow()).getOrThrow() != stick) {
      throw new AssertionError("Custom item cure lost copy or persistent codec identity");
    }
    slimeknights.tconstruct.library.utils.EffectCureUtil.setItemCures(custom, List.of());
    if (custom.getCures().contains(stick) || !custom.getCures().contains(EffectCures.PROTECTED_BY_TOTEM)) {
      throw new AssertionError("Empty curative list must remove item cures and retain totem removal");
    }
    if (slimeknights.tconstruct.library.utils.EffectCureUtil.hasItemCures(custom)
        || !slimeknights.tconstruct.library.utils.EffectCureUtil.hasItemCures(copied)) {
      throw new AssertionError("Item-curable policy confused totem/addon cures with custom item cures");
    }
    System.out.println("Item-curable effect policy separates custom item cures from totem/addon cures: PASS");
    slimeknights.tconstruct.library.utils.EffectCureUtil.setItemCures(custom,
      List.of(net.minecraft.world.item.Items.MILK_BUCKET, net.minecraft.world.item.Items.HONEY_BOTTLE));
    if (!custom.getCures().contains(EffectCures.MILK) || !custom.getCures().contains(EffectCures.HONEY)) {
      throw new AssertionError("Vanilla curative items must use native cures");
    }
    var liquidTemplate = new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 20);
    var liquidEffect = new MobEffectInstance(liquidTemplate);
    slimeknights.tconstruct.library.utils.EffectCureUtil.setItemCures(liquidEffect, List.of());
    if (liquidEffect.getCures().contains(EffectCures.MILK) || liquidEffect.getCures().contains(EffectCures.HONEY)
        || !liquidEffect.getCures().contains(EffectCures.PROTECTED_BY_TOTEM)
        || !liquidTemplate.getCures().contains(EffectCures.MILK) || !liquidTemplate.getCures().contains(EffectCures.HONEY)) {
      throw new AssertionError("Liquid effect instance mutated its template or lost totem protection");
    }
    System.out.println("Liquid effect copy isolates item cures while retaining native totem removal: PASS");
    System.out.println("Custom item cures, empty lists, native milk/honey, copy/codec identity and retained totem/addon cures: PASS");
    System.out.println("Native effect holder templates, stable legacy IDs/scaling, milk immunity and totem removal: PASS");
  }
}
