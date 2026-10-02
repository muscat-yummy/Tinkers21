package net.neoforged.neoforge.registries;

import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.BusBuilder;
import slimeknights.tconstruct.library.tools.item.armor.DummyArmorMaterial;

import java.util.List;
import java.util.Map;

/** Exercise actual native registry events, including a sound registered later. */
public final class ArmorMaterialRegistrationSmoke {
  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    // Use the normal registry mutation window in this isolated bootstrap process.
    ((BaseMappedRegistry<?>) BuiltInRegistries.ARMOR_MATERIAL).unfreeze();
    ((BaseMappedRegistry<?>) BuiltInRegistries.SOUND_EVENT).unfreeze();
    var firstBus = BusBuilder.builder().build();
    var addonBus = BusBuilder.builder().build();
    var soundId = ResourceLocation.fromNamespaceAndPath("port_armor_test", "equip");
    var sound = SoundEvent.createVariableRangeEvent(soundId);
    var firstId = ResourceLocation.fromNamespaceAndPath("port_armor_test", "travelers");
    var addonId = ResourceLocation.fromNamespaceAndPath("port_armor_addon_test", "plate");
    var first = new DummyArmorMaterial(firstId, sound);
    var addon = new DummyArmorMaterial(addonId, sound);
    DummyArmorMaterial.register(firstId.getNamespace(), firstBus);
    DummyArmorMaterial.register(addonId.getNamespace(), addonBus);
    check(!BuiltInRegistries.ARMOR_MATERIAL.containsKey(firstId), "Material registered before its event");
    firstBus.post(new RegisterEvent(Registries.ARMOR_MATERIAL, BuiltInRegistries.ARMOR_MATERIAL));
    check(BuiltInRegistries.ARMOR_MATERIAL.containsKey(firstId), "Native material registration failed");
    check(!BuiltInRegistries.ARMOR_MATERIAL.containsKey(addonId), "Addon material registered on another mod's bus");
    addonBus.post(new RegisterEvent(Registries.ARMOR_MATERIAL, BuiltInRegistries.ARMOR_MATERIAL));
    check(first.getHolder().unwrapKey().orElseThrow().location().equals(firstId), "Material registry key changed");
    check(addon.getHolder().unwrapKey().orElseThrow().location().equals(addonId), "Addon key changed");
    var material = first.getHolder().value();
    check(material.enchantmentValue() == 0 && material.toughness() == 0 && material.knockbackResistance() == 0,
      "Native material overrode tool definition stats");
    check(material.repairIngredient().get().isEmpty(), "Vanilla repair bypassed the tool station");
    for (var type : ArmorItem.Type.values()) {
      check(material.defense().get(type) == 0, "Missing native armor type");
    }
    check(material.layers().size() == 1, "Armor texture layer missing");
    Registry.register(BuiltInRegistries.SOUND_EVENT, soundId, sound);
    check(material.equipSound().value() == sound, "Deferred sound did not bind to the real registry");
    for (var type : List.of(ArmorItem.Type.BOOTS, ArmorItem.Type.LEGGINGS, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.HELMET)) {
      var item = new ArmorItem(first.getHolder(), type, new Item.Properties().stacksTo(1));
      check(item.getMaterial().is(first.getHolder().getKey()), "Armor item lost its registered material");
    }
    System.out.println("Native armor material events, addon bus ownership, deferred sound and player armor constructors: PASS");
  }
}
