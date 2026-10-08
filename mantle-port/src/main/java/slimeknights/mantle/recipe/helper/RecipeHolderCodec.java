package slimeknights.mantle.recipe.helper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Native ID-then-payload wire format, with the actual ID available to legacy payload constructors. */
public final class RecipeHolderCodec {
  private RecipeHolderCodec() {}

  public static StreamCodec<RegistryFriendlyByteBuf,RecipeHolder<?>> create(
      StreamCodec<RegistryFriendlyByteBuf,Recipe<?>> payloadCodec) {
    return StreamCodec.of((buffer, holder) -> {
      ResourceLocation.STREAM_CODEC.encode(buffer, holder.id());
      try {
        payloadCodec.encode(buffer, holder.value());
      } catch (RuntimeException error) {
        throw new io.netty.handler.codec.EncoderException("Failed to encode actual recipe " + holder.id(), error);
      }
    }, buffer -> {
      ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buffer);
      return RecipeDecodeContext.withId(id, () -> new RecipeHolder<>(id, payloadCodec.decode(buffer)));
    });
  }
}
