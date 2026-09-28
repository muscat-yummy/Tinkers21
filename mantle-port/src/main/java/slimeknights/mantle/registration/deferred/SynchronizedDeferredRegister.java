package slimeknights.mantle.registration.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Deferred register instance that synchronizes register calls */
public class SynchronizedDeferredRegister<T> {
  private final DeferredRegister<T> internal;

  private SynchronizedDeferredRegister(DeferredRegister<T> internal) {
    this.internal = internal;
  }

  /** Creates a new instance for the given resource key */
  public static <T> SynchronizedDeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String modid) {
    return new SynchronizedDeferredRegister<>(DeferredRegister.create(key, modid));
  }

  /** Creates a new instance for the given forge registry */
  public static <B> SynchronizedDeferredRegister<B> create(Registry<B> registry, String modid) {
    return new SynchronizedDeferredRegister<>(DeferredRegister.create(registry, modid));
  }

  /** Wraps an existing deferred register while retaining synchronized registration. */
  public static <T> SynchronizedDeferredRegister<T> create(DeferredRegister<T> register) {
    return new SynchronizedDeferredRegister<>(register);
  }

  /** Registers the given object, synchronized over the internal register */
  public <I extends T> RegistryObject<I> register(final String name, final Supplier<? extends I> sup) {
    synchronized (internal) {
      var holder = internal.register(name, sup);
      return new RegistryObject<>(holder.getId(), holder);
    }
  }

  /**
   * Registers the internal register with the event bus
   */
  public void register(IEventBus bus) {
    internal.register(bus);
  }
}
