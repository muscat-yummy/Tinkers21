package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;
import java.util.function.Function;

/** Serializer for recipes with no payload. Identity is held by RecipeHolder. */
public record SimpleRecipeSerializer<T extends Recipe<?>>(Supplier<T> constructor) implements RecipeSerializer<T> {
  /** Compatibility for constructors needing the real enclosing holder ID, never a generated default. */
  public SimpleRecipeSerializer(Function<ResourceLocation,T> constructor) {
    this((Supplier<T>) () -> constructor.apply(RecipeDecodeContext.requireId()));
  }

  @Override
  public MapCodec<T> codec() {
    return MapCodec.unit(constructor);
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return StreamCodec.of((buffer, recipe) -> {}, buffer -> constructor.get());
  }
}
