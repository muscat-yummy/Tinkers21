package slimeknights.tconstruct.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import slimeknights.tconstruct.library.tools.nbt.ToolStorageCodec;

/** Wrap both native lazy item codecs before strict/optional codecs capture them. */
@Mixin(ItemStack.class)
public abstract class ItemStackStorageCodecMixin {
  @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE",
    target = "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;"), require = 2)
  private static Codec<ItemStack> tconstruct$storageCodec(Codec<ItemStack> nativeCodec) {
    return ToolStorageCodec.wrap(nativeCodec);
  }
}
