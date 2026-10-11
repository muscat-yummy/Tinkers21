package slimeknights.tconstruct.library.tools.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Delegates the native item schema and carries its actual decoding ops into legacy nested storage. */
public final class ToolStorageCodec {
  private ToolStorageCodec() {}

  public static Codec<ItemStack> wrap(Codec<ItemStack> nativeCodec) {
    return new Codec<>() {
      @Override
      public <T> DataResult<T> encode(ItemStack value, DynamicOps<T> ops, T prefix) {
        return nativeCodec.encode(value, ops, prefix);
      }

      @Override
      public <T> DataResult<Pair<ItemStack,T>> decode(DynamicOps<T> ops, T input) {
        return nativeCodec.decode(ops, input).flatMap(pair -> {
          ItemStack stack = pair.getFirst();
          if (stack.isEmpty()) return DataResult.success(pair);
          var custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
          if (!custom.contains("tic_persistent", Tag.TAG_COMPOUND)) return DataResult.success(pair);
          try {
            ToolStorage oldStorage = stack.getOrDefault(ToolDataComponents.STORAGE.get(), ToolStorage.EMPTY);
            ToolStorageLegacy.Migration migrated = ToolStorageLegacy.read(custom, oldStorage, ops);
            if (migrated.changed()) {
              stack.set(DataComponents.CUSTOM_DATA, CustomData.of(migrated.custom()));
              if (migrated.storage().isEmpty()) stack.remove(ToolDataComponents.STORAGE.get());
              else stack.set(ToolDataComponents.STORAGE.get(), migrated.storage());
            }
            return DataResult.success(pair);
          } catch (RuntimeException exception) {
            return DataResult.error(() -> "Cannot migrate legacy tool storage: " + exception.getMessage());
          }
        });
      }
    };
  }
}
