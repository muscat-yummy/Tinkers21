package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves the two-way compatibility references before native tag dependency sorting. */
public final class CommonTagImports {
  private CommonTagImports() {}

  public static Map<ResourceLocation,List<TagLoader.EntryWithSource>> resolve(
      Map<ResourceLocation,List<TagLoader.EntryWithSource>> tags) {
    Map<ResourceLocation,List<TagLoader.EntryWithSource>> resolved = null;
    for (var canonical : tags.entrySet()) {
      ResourceLocation id = canonical.getKey();
      if (!"c".equals(id.getNamespace())) continue;
      List<TagLoader.EntryWithSource> entries = null;
      for (int i = 0; i < canonical.getValue().size(); i++) {
        var source = canonical.getValue().get(i);
        var entry = source.entry();
        List<TagLoader.EntryWithSource> legacy = null;
        if (!source.remove() && entry.isTag() && !entry.isRequired() &&
            "forge".equals(entry.getId().getNamespace())) {
          legacy = tags.get(entry.getId());
          if (legacy != null && legacy.stream().noneMatch(value -> !value.remove() &&
              value.entry().isTag() && !value.entry().isRequired() && value.entry().getId().equals(id))) {
            legacy = null;
          }
        }
        if (legacy == null) {
          if (entries != null) entries.add(source);
        } else {
          if (entries == null) entries = new ArrayList<>(canonical.getValue().subList(0, i));
          // The canonical tag already owns its contents. Import the actual legacy
          // contributors, preserving their order, source and native removal operations,
          // without re-importing the edge back to this same canonical tag.
          for (var contribution : legacy) {
            if (!contribution.entry().isTag() || !contribution.entry().getId().equals(id)) {
              entries.add(contribution);
            }
          }
        }
      }
      if (entries != null) {
        if (resolved == null) resolved = new LinkedHashMap<>(tags);
        resolved.put(id, entries);
      }
    }
    return resolved == null ? tags : resolved;
  }
}
