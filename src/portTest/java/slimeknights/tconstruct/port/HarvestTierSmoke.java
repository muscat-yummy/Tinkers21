package slimeknights.tconstruct.port;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import slimeknights.tconstruct.library.utils.HarvestTierRegistry;

/** Saved tier IDs and composition ordering, independent of native tag-based drop restrictions. */
public final class HarvestTierSmoke {
  private static ResourceLocation id(String name) { return ResourceLocation.parse(name); }
  private static Tier custom() {
    return new Tier() {
      public int getUses() { return 100; }
      public float getSpeed() { return 5; }
      public float getAttackDamageBonus() { return 1; }
      public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
      public int getEnchantmentValue() { return 10; }
      public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };
  }
  public static void main(String[] args) {
    List<Tier> vanilla = HarvestTierRegistry.getSortedTiers();
    if (!vanilla.equals(List.of(Tiers.WOOD, Tiers.GOLD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE))) {
      throw new AssertionError("Legacy vanilla composition order");
    }
    for (Tier tier : vanilla) {
      if (HarvestTierRegistry.byName(HarvestTierRegistry.getId(tier)) != tier) throw new AssertionError("Native tier ID identity");
    }
    Tier addon = custom();
    HarvestTierRegistry.register(addon, id("port_test:between"), List.of(id("minecraft:iron")), List.of(id("minecraft:diamond")));
    List<Tier> order = HarvestTierRegistry.getSortedTiers();
    if (order.indexOf(addon) <= order.indexOf(Tiers.IRON) || order.indexOf(addon) >= order.indexOf(Tiers.DIAMOND)) {
      throw new AssertionError("Addon constraints");
    }
    if (HarvestTierRegistry.byName(id("port_test:between")) != addon || addon.getIncorrectBlocksForDrops() != BlockTags.INCORRECT_FOR_IRON_TOOL) {
      throw new AssertionError("Custom ID or native restriction changed");
    }
    Tier first = custom();
    HarvestTierRegistry.register(first, id("port_test:first"), List.of(id("port_test:later")), List.of());
    Tier later = custom();
    HarvestTierRegistry.register(later, id("port_test:later"), List.of(), List.of());
    if (HarvestTierRegistry.getSortedTiers().indexOf(later) >= HarvestTierRegistry.getSortedTiers().indexOf(first)) {
      throw new AssertionError("Forward addon constraint");
    }
    List<Tier> snapshot = HarvestTierRegistry.getSortedTiers();
    boolean rejected = false;
    try {
      HarvestTierRegistry.register(custom(), id("port_test:cycle"), List.of(id("minecraft:diamond")), List.of(id("minecraft:wood")));
    } catch (IllegalArgumentException expected) { rejected = true; }
    if (!rejected || HarvestTierRegistry.byName(id("port_test:cycle")) != null || !snapshot.equals(HarvestTierRegistry.getSortedTiers())) {
      throw new AssertionError("Cycle must reject without mutating registrations");
    }
    rejected = false;
    try { HarvestTierRegistry.register(addon, id("port_test:duplicate"), List.of(), List.of()); }
    catch (IllegalArgumentException expected) { rejected = true; }
    if (!rejected || HarvestTierRegistry.byName(id("port_test:duplicate")) != null) throw new AssertionError("Duplicate tier identity");
    if (HarvestTierRegistry.byName(id("port_test:missing")) != null) throw new AssertionError("Unknown tier must remain unknown");
    try { snapshot.clear(); throw new AssertionError("Mutable snapshot"); }
    catch (UnsupportedOperationException expected) {}
    System.out.println("Native tier IDs, custom composition constraints, forward references and transactional cycle rejection: PASS");
  }
}
