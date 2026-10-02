package slimeknights.tconstruct.library.json.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.conditions.ICondition;

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagFilledCondition} */
@Deprecated(forRemoval = true)
public class TagNotEmptyCondition<T> implements LootItemCondition, ICondition {
  public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "tag_not_empty");
  public static final MapCodec<TagNotEmptyCondition<?>> CODEC = RecordCodecBuilder.<TagNotEmptyCondition<?>>mapCodec(instance -> instance.group(
    ResourceLocation.CODEC.fieldOf("registry").forGetter(value -> value.tag.registry().location()),
    ResourceLocation.CODEC.fieldOf("tag").forGetter(value -> value.tag.location())
  ).apply(instance, TagNotEmptyCondition::fromName));
  public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);
  private final TagKey<T> tag;

  public TagNotEmptyCondition(TagKey<T> tag) {
    this.tag = java.util.Objects.requireNonNull(tag);
  }

  private static TagNotEmptyCondition<?> fromName(ResourceLocation registry, ResourceLocation name) {
    ResourceKey<Registry<Object>> key = ResourceKey.createRegistryKey(registry);
    return new TagNotEmptyCondition<>(TagKey.create(key, name));
  }

  @Override
  public LootItemConditionType getType() {
    return TYPE;
  }

  public ResourceLocation getID() {
    return ID;
  }

  @Override
  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }

  @Override
  public boolean test(IContext context) {
    return !context.getTag(tag).isEmpty();
  }

  @Override
  public boolean test(LootContext context) {
    return context.getLevel().registryAccess().registry(tag.registry())
      .map(registry -> registry.getTagOrEmpty(tag).iterator().hasNext()).orElse(false);
  }
}
