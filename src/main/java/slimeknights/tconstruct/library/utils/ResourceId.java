package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Typed resource IDs retained through a narrowly scoped access transformer.
 * All public construction and nullable factories validate with vanilla 1.21 factories.
 */
public abstract class ResourceId extends ResourceLocation {
  /** Called only after the namespace and path have been validated. */
  protected ResourceId(String namespace, String path, @Nullable Void validated) {
    super(namespace, path);
  }

  public ResourceId(ResourceLocation location) {
    this(location.getNamespace(), location.getPath(), null);
  }

  public ResourceId(String namespace, String path) {
    this(ResourceLocation.fromNamespaceAndPath(namespace, path));
  }

  public ResourceId(String location) {
    this(ResourceLocation.parse(location));
  }

  @Nullable
  protected static <T extends ResourceLocation> T tryParse(String string, BiFunction<String,String,T> constructor) {
    ResourceLocation parsed = ResourceLocation.tryParse(string);
    return parsed == null ? null : constructor.apply(parsed.getNamespace(), parsed.getPath());
  }

  @Nullable
  protected static <T extends ResourceLocation> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    ResourceLocation parsed = ResourceLocation.tryBuild(namespace, path);
    return parsed == null ? null : constructor.apply(parsed.getNamespace(), parsed.getPath());
  }
}
