package slimeknights.tconstruct.library.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import javax.annotation.Nullable;

/** Snapshot native banner assets and translation keys without retaining a mutable registry-backed stack. */
public final class BannerPatternData {
  public static final String PATTERN = "pattern";
  public static final String COLOR = "color";
  public static final String DYE = "dye";
  public static final String TRANSLATION = "translation";
  private BannerPatternData() {}

  private static CompoundTag entry(ResourceLocation asset, String translation, DyeColor dye) {
    CompoundTag entry = new CompoundTag();
    entry.putString(PATTERN, asset.toString());
    entry.putString(TRANSLATION, translation);
    entry.putInt(DYE, dye.getId());
    entry.putInt(COLOR, dye.getTextureDiffuseColor() | 0xFF000000);
    return entry;
  }

  public static ListTag copy(@Nullable DyeColor base, BannerPatternLayers layers) {
    ListTag result = new ListTag();
    // The base is the intrinsic shield overlay asset, not a dynamic banner-pattern registry entry.
    if (base != null) {
      result.add(entry(ResourceLocation.withDefaultNamespace("base"), "block.minecraft.banner.base", base));
    }
    for (BannerPatternLayers.Layer layer : layers.layers()) {
      var pattern = layer.pattern().value();
      result.add(entry(pattern.assetId(), pattern.translationKey(), layer.color()));
    }
    return result;
  }

  @Nullable
  public static ResourceLocation asset(CompoundTag entry) {
    return ResourceLocation.tryParse(entry.getString(PATTERN));
  }
}
