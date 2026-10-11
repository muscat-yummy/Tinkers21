package slimeknights.tconstruct.port;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;
import slimeknights.tconstruct.library.json.condition.TagDifferencePresentCondition;
import slimeknights.tconstruct.library.json.condition.TagIntersectionPresentCondition;
import slimeknights.tconstruct.library.json.condition.TagNotEmptyCondition;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public final class TagConditionSmoke {
  private static final ResourceKey<Registry<String>> REGISTRY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("tconstruct", "test_values"));
  private static final Holder<String> A = Holder.direct("a");
  private static final Holder<String> B = Holder.direct("b");

  private static JsonObject json(String body) {
    return JsonParser.parseString(body).getAsJsonObject();
  }

  private static ICondition.IContext context(Map<ResourceLocation, Collection<Holder<String>>> tags) {
    return new ICondition.IContext() {
      @Override
      @SuppressWarnings({"unchecked", "rawtypes"})
      public <T> Map<ResourceLocation, Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> registry) {
        return registry.equals(REGISTRY) ? (Map) tags : Map.of();
      }
    };
  }

  public static void main(String[] args) {
    ResourceLocation left = ResourceLocation.fromNamespaceAndPath("tconstruct", "left");
    ResourceLocation right = ResourceLocation.fromNamespaceAndPath("tconstruct", "right");
    var overlap = context(Map.of(left, List.of(A, B), right, List.of(B)));
    var disjoint = context(Map.of(left, List.of(A), right, List.of(B)));
    var covered = context(Map.of(left, List.of(A), right, List.of(A)));
    var empty = context(Map.of());
    JsonObject intersectionJson = json("{\"registry\":\"tconstruct:test_values\",\"tags\":[\"tconstruct:left\",\"tconstruct:right\"]}");
    var intersection = TagIntersectionPresentCondition.CODEC.codec().parse(JsonOps.INSTANCE, intersectionJson).getOrThrow();
    if (!intersection.test(overlap) || intersection.test(disjoint) || intersection.test(empty)) {
      throw new AssertionError("Intersection semantics changed");
    }
    if (!TagIntersectionPresentCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, intersection).getOrThrow().equals(intersectionJson)) {
      throw new AssertionError("Intersection JSON fields changed");
    }
    JsonObject differenceJson = json("{\"registry\":\"tconstruct:test_values\",\"base\":\"tconstruct:left\",\"subtracted\":[\"tconstruct:right\"]}");
    var difference = TagDifferencePresentCondition.CODEC.codec().parse(JsonOps.INSTANCE, differenceJson).getOrThrow();
    if (!difference.test(overlap) || !difference.test(disjoint) || difference.test(covered) || difference.test(empty)) {
      throw new AssertionError("Difference semantics changed");
    }
    if (!TagDifferencePresentCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, difference).getOrThrow().equals(differenceJson)) {
      throw new AssertionError("Difference JSON fields changed");
    }
    JsonObject filledJson = json("{\"registry\":\"tconstruct:test_values\",\"tag\":\"tconstruct:left\"}");
    var filled = TagNotEmptyCondition.CODEC.codec().parse(JsonOps.INSTANCE, filledJson).getOrThrow();
    if (!filled.test(overlap) || filled.test(empty)
        || !TagNotEmptyCondition.CODEC.codec().encodeStart(JsonOps.INSTANCE, filled).getOrThrow().equals(filledJson)) {
      throw new AssertionError("Tag-filled semantics or JSON changed");
    }
    intersectionJson.add("tags", json("{\"tags\":[]}").get("tags"));
    differenceJson.add("subtracted", json("{\"tags\":[]}").get("tags"));
    if (TagIntersectionPresentCondition.CODEC.codec().parse(JsonOps.INSTANCE, intersectionJson).error().isEmpty()
        || TagDifferencePresentCondition.CODEC.codec().parse(JsonOps.INSTANCE, differenceJson).error().isEmpty()) {
      throw new AssertionError("Empty tag combinations accepted");
    }
    try {
      ResourceKey<Registry<String>> other = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("tconstruct", "other_values"));
      TagIntersectionPresentCondition.ofKeys(net.minecraft.tags.TagKey.create(REGISTRY, left), net.minecraft.tags.TagKey.create(other, right));
      throw new AssertionError("Mixed registries accepted");
    } catch (IllegalArgumentException expected) {
      // Different registries cannot be faithfully represented by the legacy JSON schema.
    }
    System.out.println("Tag condition intersection/difference/empty semantics and legacy JSON: PASS");
  }
}
