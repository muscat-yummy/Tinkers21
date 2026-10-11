package slimeknights.mantle.recipe.helper;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.Mantle;

/** Logs recipe packet failures before forwarding them to the connection. */
public interface LoggingRecipeSerializer<T extends Recipe<?>> extends RecipeSerializer<T> {
  T fromNetworkSafe(RegistryFriendlyByteBuf buffer);
  void toNetworkSafe(RegistryFriendlyByteBuf buffer, T recipe);

  @Override
  default StreamCodec<RegistryFriendlyByteBuf,T> streamCodec() {
    return StreamCodec.of((buffer, recipe) -> {
      try {
        toNetworkSafe(buffer, recipe);
      } catch (RuntimeException e) {
        String error = getClass().getSimpleName() + ": Error writing recipe of class " + recipe.getClass().getSimpleName();
        Mantle.logger.error("{}", error, e);
        throw new EncoderException(error, e);
      }
    }, buffer -> {
      try {
        return fromNetworkSafe(buffer);
      } catch (RuntimeException e) {
        String error = getClass().getSimpleName() + ": Error reading recipe";
        Mantle.logger.error("{}", error, e);
        throw new DecoderException(error, e);
      }
    });
  }
}
