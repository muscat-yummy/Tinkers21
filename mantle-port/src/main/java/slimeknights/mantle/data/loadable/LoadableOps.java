package slimeknights.mantle.data.loadable;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.function.Supplier;

/** Carries registry-aware JSON operations through nested legacy loadables. */
public final class LoadableOps {
  private static final ThreadLocal<DynamicOps<JsonElement>> CURRENT = new ThreadLocal<>();

  private LoadableOps() {}

  public static DynamicOps<JsonElement> current() {
    DynamicOps<JsonElement> ops = CURRENT.get();
    return ops == null ? JsonOps.INSTANCE : ops;
  }

  private static <T> ICondition.IContext conditionContext(DynamicOps<T> ops) {
    return ConditionalOps.retrieveContext().codec().parse(ops, ops.emptyMap()).getOrThrow();
  }

  /** Always restores the enclosing scope, including when decoding fails. */
  public static <T> T with(DynamicOps<?> ops, Supplier<T> operation) {
    DynamicOps<JsonElement> previous = CURRENT.get();
    DynamicOps<JsonElement> jsonOps = JsonOps.INSTANCE;
    if (ops instanceof RegistryOps<?> registryOps) {
      RegistryOps<JsonElement> normalized = registryOps.withParent(JsonOps.INSTANCE);
      jsonOps = ops instanceof ConditionalOps<?> ? new ConditionalOps<>(normalized, conditionContext(ops)) : normalized;
    }
    CURRENT.set(jsonOps);
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
