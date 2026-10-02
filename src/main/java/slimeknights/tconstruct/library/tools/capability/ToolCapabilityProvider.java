package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Native item capability dispatch for tool stacks. State is stored in the tool, not the provider. */
public class ToolCapabilityProvider {
  private static final List<BiFunction<ItemStack,Supplier<? extends IToolStackView>,IToolCapabilityProvider>> PROVIDER_CONSTRUCTORS = new ArrayList<>();
  private final ItemStack stack;
  @Nullable
  private ToolStack tool;
  private final List<IToolCapabilityProvider> providers;

  public ToolCapabilityProvider(ItemStack stack) {
    this.stack = stack;
    this.providers = PROVIDER_CONSTRUCTORS.stream()
      .map(constructor -> constructor.apply(stack, this::getTool))
      .filter(Objects::nonNull).toList();
  }

  /** Refresh the tool before both capability selection and subsequent handler operations. */
  private ToolStack getTool() {
    if (tool == null) {
      tool = ToolStack.from(stack);
    } else {
      tool.refreshTag(stack);
    }
    return tool;
  }

  @Nullable
  public <T> T getCapability(ItemCapability<T,Void> cap) {
    ToolStack current = getTool();
    for (IToolCapabilityProvider provider : providers) {
      provider.clearCache();
      T result = provider.getCapability(current, cap);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /** Register the built-in capabilities during the native mod-bus capability event. */
  public static void registerCapabilities(RegisterCapabilitiesEvent event) {
    registerCapability(event, Capabilities.FluidHandler.ITEM);
    registerCapability(event, Capabilities.ItemHandler.ITEM);
    registerCapability(event, Capabilities.EnergyStorage.ITEM);
    registerCapability(event, BlockItemProviderCapability.CAPABILITY);
  }

  /** Addons with custom tool capabilities call this from RegisterCapabilitiesEvent. */
  public static <T> void registerCapability(RegisterCapabilitiesEvent event, ItemCapability<T,Void> capability) {
    for (var item : BuiltInRegistries.ITEM) {
      if (item instanceof IModifiable) {
        event.registerItem(capability, (stack, context) -> new ToolCapabilityProvider(stack).getCapability(capability), item);
      }
    }
  }

  /**
   * Register a provider constructor before the capability event.
   * Native item lookups create providers per query; constructors must not store persistent state.
   * Return null to decline this stack. Persistent state belongs in the supplied tool's data.
   */
  public static void register(BiFunction<ItemStack,Supplier<? extends IToolStackView>,IToolCapabilityProvider> constructor) {
    PROVIDER_CONSTRUCTORS.add(Objects.requireNonNull(constructor));
  }

  @FunctionalInterface
  public interface IToolCapabilityProvider {
    @Nullable
    <T> T getCapability(IToolStackView tool, ItemCapability<T,Void> cap);
    default void clearCache() {}
  }
}
