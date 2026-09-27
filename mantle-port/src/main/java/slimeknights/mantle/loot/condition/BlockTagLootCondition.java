package slimeknights.mantle.loot.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import slimeknights.mantle.loot.MantleLoot;

import java.util.Optional;
import java.util.Set;

/** Loot condition matching a block tag and an optional state predicate. */
public class BlockTagLootCondition implements LootItemCondition {
  public static final MapCodec<BlockTagLootCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    ResourceLocation.CODEC.fieldOf("tag").forGetter((BlockTagLootCondition value) -> value.tag.location()),
    StatePropertiesPredicate.CODEC.optionalFieldOf("properties", Optional.empty()).forGetter(value -> value.properties)
  ).apply(instance, (id, properties) -> new BlockTagLootCondition(TagKey.create(Registries.BLOCK, id), properties)));

  private final TagKey<Block> tag;
  private final Optional<StatePropertiesPredicate> properties;

  public BlockTagLootCondition(TagKey<Block> tag, Optional<StatePropertiesPredicate> properties) {
    this.tag = tag;
    this.properties = properties;
  }

  public BlockTagLootCondition(TagKey<Block> tag) {
    this(tag, Optional.empty());
  }

  public BlockTagLootCondition(TagKey<Block> tag, StatePropertiesPredicate properties) {
    this(tag, Optional.of(properties));
  }

  public BlockTagLootCondition(TagKey<Block> tag, StatePropertiesPredicate.Builder builder) {
    this(tag, builder.build());
  }

  @Override
  public boolean test(LootContext context) {
    BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
    return state != null && state.is(tag) && properties.map(predicate -> predicate.matches(state)).orElse(true);
  }

  @Override
  public Set<LootContextParam<?>> getReferencedContextParams() {
    return Set.of(LootContextParams.BLOCK_STATE);
  }

  @Override
  public LootItemConditionType getType() {
    return MantleLoot.BLOCK_TAG_CONDITION;
  }
}
