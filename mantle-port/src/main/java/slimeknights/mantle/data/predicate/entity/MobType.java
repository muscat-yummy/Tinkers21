package slimeknights.mantle.data.predicate.entity;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;

/** Compatibility categories for the legacy mob type predicate. */
public enum MobType {
  UNDEFINED, UNDEAD, ARTHROPOD, ILLAGER, WATER;

  public boolean matches(LivingEntity entity) {
    var type = entity.getType();
    return switch (this) {
      case UNDEAD -> type.is(EntityTypeTags.UNDEAD);
      case ARTHROPOD -> type.is(EntityTypeTags.ARTHROPOD);
      case ILLAGER -> type.is(EntityTypeTags.ILLAGER);
      case WATER -> type.is(EntityTypeTags.AQUATIC);
      case UNDEFINED -> !type.is(EntityTypeTags.UNDEAD)
          && !type.is(EntityTypeTags.ARTHROPOD)
          && !type.is(EntityTypeTags.ILLAGER)
          && !type.is(EntityTypeTags.AQUATIC);
    };
  }
}
