package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Loadable for vanilla and NeoForge custom ingredients. */
public enum IngredientLoadable implements Loadable<Ingredient> {
  ALLOW_EMPTY(true),
  DISALLOW_EMPTY(false);

  private final CodecLoadable<Ingredient> codec;

  IngredientLoadable(boolean allowEmpty) {
    this.codec = new CodecLoadable<>(allowEmpty ? Ingredient.CODEC : Ingredient.CODEC_NONEMPTY);
  }

  @Override
  public Ingredient convert(JsonElement element, String key, TypedMap context) {
    return codec.convert(element, key, context);
  }

  @Override
  public JsonElement serialize(Ingredient object) {
    if (object.isEmpty() && this == DISALLOW_EMPTY) {
      throw new IllegalArgumentException("Ingredient cannot be empty");
    }
    return codec.serialize(object);
  }

  @Override
  public Ingredient decode(FriendlyByteBuf buffer, TypedMap context) {
    return codec.decode(buffer, context);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, Ingredient object) {
    codec.encode(buffer, object);
  }
}
