package slimeknights.mantle.registration.deferred;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Mantle's single-type view of a NeoForge deferred registry holder.
 * The underlying holder still performs the actual registry resolution.
 */
public final class RegistryObject<T> implements Supplier<T> {
  private final ResourceLocation id;
  private final Supplier<? extends T> holder;

  RegistryObject(ResourceLocation id, Supplier<? extends T> holder) {
    this.id = Objects.requireNonNull(id);
    this.holder = Objects.requireNonNull(holder);
  }

  /** Wrap an actual NeoForge holder while retaining Mantle's single-type API. */
  public static <R,T extends R> RegistryObject<T> from(DeferredHolder<R,T> holder) {
    Objects.requireNonNull(holder);
    return new RegistryObject<>(holder.getId(), holder);
  }

  public ResourceLocation getId() {
    return id;
  }

  @Override
  public T get() {
    return holder.get();
  }
}
