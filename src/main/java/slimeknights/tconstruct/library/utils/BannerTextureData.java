package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;

/** Native shield texture files use the banner pattern's asset ID, which can differ from its registry key. */
public final class BannerTextureData {
  public static final String DIRECTORY = "textures/entity/shield";
  private static final String PREFIX = DIRECTORY + "/";
  private BannerTextureData() {}
  @Nullable
  public static ResourceLocation asset(ResourceLocation file) {
    String path = file.getPath();
    if (!path.startsWith(PREFIX) || !path.endsWith(".png")) return null;
    String asset = path.substring(PREFIX.length(), path.length() - 4);
    return asset.isEmpty() ? null : ResourceLocation.fromNamespaceAndPath(file.getNamespace(), asset);
  }
}
