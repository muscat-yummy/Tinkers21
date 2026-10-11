package slimeknights.tconstruct.port;

import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import slimeknights.tconstruct.library.utils.BannerPatternData;
import slimeknights.tconstruct.library.utils.BannerTextureData;
import java.util.List;
import java.util.Map;

public final class BannerPatternDataSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    var registries = VanillaRegistries.createLookup();
    var cross = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(BannerPatterns.CROSS);
    var layers = new BannerPatternLayers(List.of(
      new BannerPatternLayers.Layer(cross, DyeColor.RED),
      new BannerPatternLayers.Layer(cross, DyeColor.BLUE)));
    var ops = registries.createSerializationContext(JsonOps.INSTANCE);
    var encoded = BannerPatternLayers.CODEC.encodeStart(ops, layers).getOrThrow();
    var decoded = BannerPatternLayers.CODEC.parse(ops, encoded).getOrThrow();
    check(decoded.equals(layers), "Registry-aware native banner codec changed layer order, holders or colors");
    ItemStack banner = new ItemStack(Items.WHITE_BANNER);
    banner.set(DataComponents.BANNER_PATTERNS, layers);
    ItemStack restored = ItemStack.parseOptional(registries, (net.minecraft.nbt.CompoundTag)banner.save(registries));
    check(restored.get(DataComponents.BANNER_PATTERNS).equals(layers), "Banner component did not survive native stack persistence");
    var snapshot = BannerPatternData.copy(DyeColor.WHITE, decoded);
    check(snapshot.size() == 3 && BannerPatternData.asset(snapshot.getCompound(0)).equals(ResourceLocation.withDefaultNamespace("base")),
      "Solid base and ordered layers were not preserved");
    check(BannerPatternData.asset(snapshot.getCompound(1)).equals(cross.value().assetId())
      && snapshot.getCompound(1).getString(BannerPatternData.TRANSLATION).equals(cross.value().translationKey()),
      "Native asset and translation identity were lost");
    check(BannerPatternData.copy(null, decoded).size() == 2
      && BannerPatternData.copy(null, BannerPatternLayers.EMPTY).isEmpty(), "Clear background acquired an extra base layer");
    for (DyeColor dye : DyeColor.values()) {
      var colored = BannerPatternData.copy(null, new BannerPatternLayers(List.of(new BannerPatternLayers.Layer(cross, dye))));
      check(colored.getCompound(0).getInt(BannerPatternData.COLOR) == (dye.getTextureDiffuseColor() | 0xFF000000),
        "Native packed diffuse color was changed");
    }
    var customAsset = ResourceLocation.fromNamespaceAndPath("addon", "nested/custom_overlay");
    var custom = new BannerPatternLayers(List.of(new BannerPatternLayers.Layer(
      Holder.direct(new BannerPattern(customAsset, "addon.pattern.custom")), DyeColor.GREEN)));
    var customSnapshot = BannerPatternData.copy(null, custom);
    check(BannerPatternData.asset(customSnapshot.getCompound(0)).equals(customAsset)
      && customSnapshot.getCompound(0).getString(BannerPatternData.TRANSLATION).equals("addon.pattern.custom"),
      "Custom native pattern was replaced by a registry-key or vanilla-only texture");
    snapshot.getCompound(1).putString(BannerPatternData.PATTERN, "addon:changed");
    snapshot.getCompound(1).putInt(BannerPatternData.COLOR, 0);
    check(BannerPatternData.copy(null, layers).getCompound(0).getInt(BannerPatternData.COLOR)
      == (DyeColor.RED.getTextureDiffuseColor() | 0xFF000000)
      && banner.get(DataComponents.BANNER_PATTERNS).equals(layers), "Mutable tool snapshot changed immutable banner input");
    check(BannerTextureData.asset(ResourceLocation.fromNamespaceAndPath("addon", "textures/entity/shield/nested/custom_overlay.png")).equals(customAsset),
      "Resource-pack namespace or nested texture path was lost");
    check(BannerTextureData.asset(ResourceLocation.withDefaultNamespace("textures/entity/banner/cross.png")) == null
      && BannerTextureData.asset(ResourceLocation.withDefaultNamespace("textures/entity/shield/cross.png.mcmeta")) == null
      && BannerTextureData.asset(ResourceLocation.withDefaultNamespace("textures/entity/shield/.png")) == null,
      "Non-shield resources were admitted as pattern assets");
    System.out.println("Native banner holders, component persistence, layer order, clear base, packed dyes and resource-pack asset identities: PASS");
  }
}
