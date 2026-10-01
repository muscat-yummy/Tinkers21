package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;

/** Serializer for recipes with no payload. Identity is held by RecipeHolder. */
public record SimpleRecipeSerializer<T extends Recipe<?>>(Supplier<T> constructor) implements RecipeSerializer<T> {
  @Override
  public MapCodec<T> codec() {
    return MapCodec.unit(constructor);
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return StreamCodec.of((buffer, recipe) -> {}, buffer -> constructor.get());
  }
}
