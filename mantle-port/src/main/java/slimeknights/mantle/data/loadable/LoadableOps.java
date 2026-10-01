package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;

import java.util.function.Supplier;

/** Carries registry-aware JSON operations through nested legacy loadables. */
public final class LoadableOps {
  private static final ThreadLocal<DynamicOps<JsonElement>> CURRENT = new ThreadLocal<>();

  private LoadableOps() {}

  public static DynamicOps<JsonElement> current() {
    DynamicOps<JsonElement> ops = CURRENT.get();
    return ops == null ? JsonOps.INSTANCE : ops;
  }

  /** Always restores the enclosing scope, including when decoding fails. */
  public static <T> T with(DynamicOps<?> ops, Supplier<T> operation) {
    DynamicOps<JsonElement> previous = CURRENT.get();
    CURRENT.set(ops instanceof RegistryOps<?> registryOps ? registryOps.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
    try {
      return operation.get();
    } finally {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    }
  }
}
