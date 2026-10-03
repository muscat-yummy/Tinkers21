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
    var normal = new MobEffectInstance(Holder.direct(new TinkerEffect(MobEffectCategory.HARMFUL, true)), 20);
    var noMilk = new MobEffectInstance(Holder.direct(new NoMilkEffect(MobEffectCategory.HARMFUL, 0xffffff, true)), 20);
    if (!normal.getCures().contains(EffectCures.MILK) || noMilk.getCures().contains(EffectCures.MILK)
        || !noMilk.getCures().contains(EffectCures.PROTECTED_BY_TOTEM)) {
      throw new AssertionError("Milk immunity must retain totem removal");
    }
    if (new MobEffectInstance(noMilk).getCures().contains(EffectCures.MILK)) {
      throw new AssertionError("Effect copy lost milk immunity");
    }
    System.out.println("Native effect holder templates, stable legacy IDs/scaling, milk immunity and totem removal: PASS");
  }
}
