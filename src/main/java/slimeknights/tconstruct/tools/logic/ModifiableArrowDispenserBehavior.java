package slimeknights.tconstruct.tools.logic;

import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.item.Item;

/** Uses the arrow item's native ProjectileItem contract, preserving its tool data on creation. */
public class ModifiableArrowDispenserBehavior extends ProjectileDispenseBehavior {
  public ModifiableArrowDispenserBehavior(Item item) {
    super(item);
  }
}
