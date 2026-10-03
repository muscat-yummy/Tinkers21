package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Stable native IDs shared by legacy UUID attribute templates and equipment modifiers. */
public final class AttributeIdUtil {
  private AttributeIdUtil() {}

  public static ResourceLocation fromUuid(UUID uuid) {
    // Keep the IDs already used by the effect port; the same old UUID must remain one modifier.
    return ResourceLocation.fromNamespaceAndPath("tconstruct", "legacy_effect/" + uuid);
  }
}
