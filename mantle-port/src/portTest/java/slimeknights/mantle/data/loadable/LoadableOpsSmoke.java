package slimeknights.mantle.data.loadable;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import java.util.stream.Stream;

/** Independently runnable while the imported Mantle source is still being ported. */
public final class LoadableOpsSmoke {
  public static void main(String[] args) {
    var registryOps = RegistryOps.create(JsonOps.INSTANCE, HolderLookup.Provider.create(Stream.empty()));
    check(LoadableOps.current() == JsonOps.INSTANCE, "initial plain scope");
    LoadableOps.with(registryOps, () -> {
      var outer = LoadableOps.current();
      check(outer instanceof RegistryOps<?>, "registry lookup survives conversion to JSON");
      try {
        LoadableOps.with(JsonOps.INSTANCE, () -> {
          check(LoadableOps.current() == JsonOps.INSTANCE, "nested plain scope");
          throw new IllegalStateException("expected decode failure");
        });
        throw new AssertionError("nested exception was swallowed");
      } catch (IllegalStateException expected) {
        check(LoadableOps.current() == outer, "failed nested decode restores outer registry context");
      }
      var separateFailure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
      Thread separate = new Thread(() -> {
        try {
          check(LoadableOps.current() == JsonOps.INSTANCE, "registry context must not cross threads");
        } catch (Throwable failure) {
          separateFailure.set(failure);
        }
      });
      separate.start();
      try {
        separate.join();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      check(separateFailure.get() == null, "thread isolation");
      return null;
    });
    check(LoadableOps.current() == JsonOps.INSTANCE, "completed decode releases registry context");
    System.out.println("PASS: registry scope, nested failure restoration, thread isolation and cleanup");
  }

  private static void check(boolean result, String message) {
    if (!result) {
      throw new AssertionError(message);
    }
  }
}
