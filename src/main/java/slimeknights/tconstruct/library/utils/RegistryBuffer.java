package slimeknights.tconstruct.library.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Native play payload codecs require the registry lookup carried by their real buffer. */
public final class RegistryBuffer {
  private RegistryBuffer() {}

  public static RegistryFriendlyByteBuf require(FriendlyByteBuf buffer) {
    if (buffer instanceof RegistryFriendlyByteBuf registryBuffer) {
      return registryBuffer;
    }
    throw new IllegalArgumentException("Play payload requires a registry-aware buffer");
  }
}
