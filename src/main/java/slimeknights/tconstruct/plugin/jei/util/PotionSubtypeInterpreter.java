package slimeknights.tconstruct.plugin.jei.util;

import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;

/** The same potion subtype identity for item and fluid ingredients, including custom effects and color. */
@SuppressWarnings("removal")
public interface PotionSubtypeInterpreter<T> extends IIngredientSubtypeInterpreter<T> {
  PotionContents getContents(T ingredient);

  @Override
  default String apply(T ingredient, UidContext context) {
    PotionContents contents = getContents(ingredient);
    if (contents.equals(PotionContents.EMPTY)) {
      return NONE;
    }
    StringBuilder identity = new StringBuilder();
    contents.potion().flatMap(net.minecraft.core.Holder::unwrapKey)
      .ifPresent(key -> identity.append(key.location()));
    contents.customColor().ifPresent(color -> identity.append(";color=").append(color));
    for (MobEffectInstance effect : contents.customEffects()) {
      identity.append(";effect=").append(effect.save());
    }
    return identity.toString();
  }
}
