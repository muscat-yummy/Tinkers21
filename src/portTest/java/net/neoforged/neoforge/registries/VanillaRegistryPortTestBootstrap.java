package net.neoforged.neoforge.registries;

import net.neoforged.bus.api.BusBuilder;

/**
 * Runs NeoForge's real built-in registry modification listeners in isolated port tests.
 * Package access is needed only to construct the native lifecycle event.
 */
public final class VanillaRegistryPortTestBootstrap {
  private VanillaRegistryPortTestBootstrap() {}

  public static void modifyRegistries() {
    var bus = BusBuilder.builder().build();
    NeoForgeRegistriesSetup.setup(bus);
    bus.post(new ModifyRegistriesEvent());
  }
}
