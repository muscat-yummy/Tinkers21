package slimeknights.tconstruct.mixin;

import mezz.jei.api.gui.placement.IPlaceable;
import mezz.jei.api.gui.widgets.ITextWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Restore JEI 19's covariant text positioning ABI on the older EMI bridge. */
@Pseudo
@Mixin(targets="dev.emi.emi.jemi.impl.extras.JemiTextWidget",remap=false)
public abstract class EmiTextWidgetMixin {
  @Unique
  public ITextWidget setPosition(int x,int y) {
    ((IPlaceable<?>)(Object)this).setPosition(x,y);
    return (ITextWidget)(Object)this;
  }
}
