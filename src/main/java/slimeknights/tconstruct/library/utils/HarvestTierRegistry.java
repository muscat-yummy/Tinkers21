package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Named tiers for TiC's saved tool stats and composition.
 * Drop correctness belongs to Tier.getIncorrectBlocksForDrops; this ordering only selects stats.
 * Addons can register native tiers with stable IDs and before/after constraints.
 */
public final class HarvestTierRegistry {
  private record Entry(Tier tier, List<ResourceLocation> after, List<ResourceLocation> before) {}
  private static final Map<ResourceLocation,Entry> ENTRIES = new LinkedHashMap<>();
  private static final Map<Tier,ResourceLocation> IDS = new IdentityHashMap<>();
  private static List<Tier> sorted = List.of();

  static {
    ResourceLocation previous = null;
    for (Tiers tier : new Tiers[] {Tiers.WOOD, Tiers.GOLD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE}) {
      ResourceLocation id = ResourceLocation.withDefaultNamespace(tier.name().toLowerCase(java.util.Locale.ROOT));
      register(tier, id, previous == null ? List.of() : List.of(previous), List.of());
      previous = id;
    }
  }

  private HarvestTierRegistry() {}

  public static synchronized <T extends Tier> T register(T tier, ResourceLocation id, List<ResourceLocation> after, List<ResourceLocation> before) {
    Objects.requireNonNull(tier);
    Objects.requireNonNull(id);
    if (ENTRIES.containsKey(id) || IDS.containsKey(tier)) {
      throw new IllegalArgumentException("Duplicate harvest tier " + id);
    }
    Map<ResourceLocation,Entry> candidate = new LinkedHashMap<>(ENTRIES);
    candidate.put(id, new Entry(tier, List.copyOf(after), List.copyOf(before)));
    List<Tier> order = sort(candidate);
    ENTRIES.putAll(candidate);
    IDS.put(tier, id);
    sorted = order;
    return tier;
  }

  private static List<Tier> sort(Map<ResourceLocation,Entry> entries) {
    Map<ResourceLocation,LinkedHashSet<ResourceLocation>> dependencies = new LinkedHashMap<>();
    entries.forEach((id, entry) -> dependencies.put(id, new LinkedHashSet<>()));
    entries.forEach((id, entry) -> {
      // Constraints may name a tier that another addon registers later.
      for (ResourceLocation lower : entry.after()) {
        if (entries.containsKey(lower)) dependencies.get(id).add(lower);
      }
      for (ResourceLocation higher : entry.before()) {
        if (entries.containsKey(higher)) dependencies.get(higher).add(id);
      }
    });
    List<Tier> order = new ArrayList<>(entries.size());
    while (!dependencies.isEmpty()) {
      ResourceLocation next = dependencies.entrySet().stream()
        .filter(entry -> entry.getValue().isEmpty()).map(Map.Entry::getKey).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Cyclic harvest tier order " + dependencies.keySet()));
      order.add(entries.get(next).tier());
      dependencies.remove(next);
      dependencies.values().forEach(values -> values.remove(next));
    }
    return List.copyOf(order);
  }

  public static synchronized Tier byName(ResourceLocation id) {
    Entry entry = ENTRIES.get(id);
    return entry == null ? null : entry.tier();
  }

  public static synchronized ResourceLocation getId(Tier tier) {
    return IDS.get(tier);
  }

  public static synchronized List<Tier> getSortedTiers() {
    return sorted;
  }
}
