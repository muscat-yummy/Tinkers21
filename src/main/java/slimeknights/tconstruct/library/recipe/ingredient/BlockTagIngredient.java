package slimeknights.tconstruct.library.recipe.ingredient;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.util.RegistryHelper;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Matches item forms of blocks in the given tag using NeoForge's native ingredient wrapper. */
public class BlockTagIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "block_tag");
  public static final MapCodec<BlockTagIngredient> CODEC =
    TagKey.codec(Registries.BLOCK).fieldOf("tag").xmap(BlockTagIngredient::new, value -> value.tag);
  public static final IngredientType<BlockTagIngredient> TYPE = new IngredientType<>(CODEC);
  private final TagKey<Block> tag;

  public BlockTagIngredient(TagKey<Block> tag) {
    this.tag = Objects.requireNonNull(tag);
  }

  /** Read the currently bound tag; no removed Forge invalidation cache is retained. */
  private Set<Item> getMatchingItems() {
    return RegistryHelper.getTagValueStream(BuiltInRegistries.BLOCK, tag)
      .map(Block::asItem)
      .filter(item -> item != Items.AIR)
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && getMatchingItems().contains(stack.getItem());
  }

  @Override
  public Stream<ItemStack> getItems() {
    return getMatchingItems().stream().map(ItemStack::new);
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<BlockTagIngredient> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object object) {
    return object == this || object instanceof BlockTagIngredient other && tag.equals(other.tag);
  }

  @Override
  public int hashCode() {
    return tag.hashCode();
  }
}
