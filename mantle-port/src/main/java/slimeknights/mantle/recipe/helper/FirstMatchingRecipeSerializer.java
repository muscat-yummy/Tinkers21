package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Decodes directly to the selected concrete recipe; no runtime wrapper changes recipe type or identity. */
public final class FirstMatchingRecipeSerializer implements RecipeSerializer<Recipe<?>> {
  private static final MapCodec<Recipe<?>> CODEC = FirstMatchingCodec.create(
      Recipe.CONDITIONAL_CODEC.xmap(value -> value.map(conditional -> conditional.carrier()),
          value -> value.map(net.neoforged.neoforge.common.conditions.WithConditions::new)));

  @Override
  public MapCodec<Recipe<?>> codec() {
    return CODEC;
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> streamCodec() {
    // Normally the selected recipe's own serializer is used; this also handles explicit dispatch through this serializer.
    return Recipe.STREAM_CODEC;
  }
}
