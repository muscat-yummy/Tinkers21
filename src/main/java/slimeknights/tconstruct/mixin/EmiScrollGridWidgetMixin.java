package slimeknights.tconstruct.mixin;

import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Restore JEI 19's covariant aligned positioning ABI on EMI's older scroll grid. */
@Pseudo
@Mixin(targets="dev.emi.emi.jemi.impl.extras.JemiScrollGridWidget",remap=false)
public abstract class EmiScrollGridWidgetMixin {
  @Unique
  public IScrollGridWidget setPosition(int x,int y,int width,int height,
                                       HorizontalAlignment horizontal,VerticalAlignment vertical) {
    IScrollGridWidget widget=(IScrollGridWidget)(Object)this;
    widget.setPosition(x+horizontal.getXPos(width,widget.getWidth()),
                       y+vertical.getYPos(height,widget.getHeight()));
    return (IScrollGridWidget)(Object)this;
  }
}
