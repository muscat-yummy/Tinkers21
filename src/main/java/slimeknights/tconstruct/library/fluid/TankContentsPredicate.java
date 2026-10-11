package slimeknights.tconstruct.library.fluid;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.tconstruct.library.utils.NBTTags;

/** Matches the fluid and exact per-item amount in a tank, including legacy saved tanks. */
public record TankContentsPredicate(StoredFluid expected) implements ItemSubPredicate {
  public static final Codec<TankContentsPredicate> CODEC = StoredFluid.CODEC
    .fieldOf("fluid").xmap(TankContentsPredicate::new, TankContentsPredicate::expected).codec();

  public boolean matchesFluid(FluidStack actual) {
    // Old partial tank NBT did not restrict additional fluid metadata.
    return !actual.isEmpty() && actual.getFluid() == expected.fluid().value()
      && actual.getAmount() == expected.amount();
  }

  @Override
  public boolean matches(ItemStack stack) {
    return matchesFluid(FluidItemData.getTank(stack, NBTTags.TANK));
  }
}
