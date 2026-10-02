package slimeknights.tconstruct.shared.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;
import java.util.Optional;

/** Criteria that trigger when a specific block-entity container is opened. */
public class BlockContainerOpenedTrigger extends SimpleCriterionTrigger<BlockContainerOpenedTrigger.Instance> {
  public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "block_container_opened");
  public static final BlockContainerOpenedTrigger INSTANCE = new BlockContainerOpenedTrigger();

  @Override
  public Codec<Instance> codec() {
    return Instance.CODEC;
  }

  /** Triggers this criterion for server players only. */
  public void trigger(@Nullable BlockEntity blockEntity, @Nullable Inventory inventory) {
    if (blockEntity != null && inventory != null && inventory.player instanceof ServerPlayer player) {
      trigger(player, instance -> instance.test(blockEntity.getType()));
    }
  }

  public record Instance(Optional<ContextAwarePredicate> player, BlockEntityType<?> type) implements SimpleCriterionTrigger.SimpleInstance {
    public static final Codec<Instance> CODEC = RecordCodecBuilder.create(builder -> builder.group(
      EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
      BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(Instance::type)
    ).apply(builder, Instance::new));

    public Instance(@Nullable ContextAwarePredicate player, BlockEntityType<?> type) {
      this(Optional.ofNullable(player), type);
    }

    public static Criterion<Instance> container(BlockEntityType<?> type) {
      return INSTANCE.createCriterion(new Instance(Optional.empty(), type));
    }

    public boolean test(BlockEntityType<?> type) {
      return this.type == type;
    }
  }
}
