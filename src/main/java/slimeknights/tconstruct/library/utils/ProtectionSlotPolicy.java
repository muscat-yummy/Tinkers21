package slimeknights.tconstruct.library.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Configured slot extensions, resolved against the caller's actual enchantment holders. */
public final class ProtectionSlotPolicy {
  private static final Set<ResourceKey<Enchantment>> EXTENDED = ConcurrentHashMap.newKeySet();

  private ProtectionSlotPolicy() {}

  public static void extend(ResourceKey<Enchantment> key) {
    EXTENDED.add(key);
  }

  public static boolean extendsSlots(Holder<Enchantment> enchantment) {
    return enchantment.unwrapKey().map(EXTENDED::contains).orElse(false);
  }

  public static boolean extendsSlots(Enchantment enchantment, LivingEntity entity) {
    var enchantments = entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    for (ResourceKey<Enchantment> key : EXTENDED) {
      if (enchantments.get(key).filter(holder -> holder.value() == enchantment).isPresent()) {
        return true;
      }
    }
    return false;
  }
}
