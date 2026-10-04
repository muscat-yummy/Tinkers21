package slimeknights.tconstruct.port;

import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import slimeknights.tconstruct.library.utils.EnchantmentData;

import java.util.List;
import java.util.Map;

public final class EnchantmentDataSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = VanillaRegistries.createLookup();
    var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
    Holder<Enchantment> fortune = lookup.getOrThrow(Enchantments.FORTUNE);
    Holder<Enchantment> silk = lookup.getOrThrow(Enchantments.SILK_TOUCH);
    ItemEnchantments.Mutable initial = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY.withTooltip(false));
    initial.set(fortune, 3);
    ItemEnchantments original = initial.toImmutable();
    var levels = EnchantmentData.mutable(original);
    levels.merge(fortune, -4, Integer::sum);
    check(levels.get(fortune) == -1, "Signed intermediate correction was lost");
    levels.merge(fortune, 2, Integer::sum);
    levels.put(silk, -2);
    ItemEnchantments result = EnchantmentData.build(original, levels);
    check(result.getLevel(fortune) == 1 && result.getLevel(silk) == 0 && result.size() == 1,
      "Final native levels did not apply cumulative corrections");
    check(original.getLevel(fortune) == 3, "Immutable original enchantments changed");
    check(result.equals(result.withTooltip(false)), "Native tooltip policy was not retained");
    levels.put(fortune, 1000);
    check(EnchantmentData.build(original, levels).getLevel(fortune) == EnchantmentData.clampLevel(1000),
      "Single-level query and native component limits differ");
    check(EnchantmentData.clampLevel(-1) == 0, "Negative final level survived");

    ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
    tool.set(DataComponents.ENCHANTMENTS, original);
    var saved = EnchantmentData.Snapshot.capture(tool);
    tool.set(DataComponents.ENCHANTMENTS, result);
    saved.restore(tool);
    check(tool.get(DataComponents.ENCHANTMENTS).equals(original), "Exact original component was not restored");
    tool.remove(DataComponents.ENCHANTMENTS);
    var absent = EnchantmentData.Snapshot.capture(tool);
    tool.set(DataComponents.ENCHANTMENTS, result);
    absent.restore(tool);
    check(!tool.has(DataComponents.ENCHANTMENTS), "Absent original component was replaced by empty data");
    tool.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY.withTooltip(false));
    var empty = EnchantmentData.Snapshot.capture(tool);
    tool.set(DataComponents.ENCHANTMENTS, result);
    empty.restore(tool);
    check(tool.has(DataComponents.ENCHANTMENTS)
      && tool.get(DataComponents.ENCHANTMENTS).equals(ItemEnchantments.EMPTY.withTooltip(false)),
      "Explicit empty component and tooltip policy were not restored");

    var ops = registries.createSerializationContext(JsonOps.INSTANCE);
    var encoded = ItemEnchantments.CODEC.encodeStart(ops, result).getOrThrow();
    var decoded = ItemEnchantments.CODEC.parse(ops, encoded).getOrThrow();
    check(decoded.equals(result), "Registry-aware native enchantment codec changed holders or tooltip policy");
    System.out.println("Native enchantment registry holders, signed corrections, clamps, exact restoration and codec: PASS");
  }
}
