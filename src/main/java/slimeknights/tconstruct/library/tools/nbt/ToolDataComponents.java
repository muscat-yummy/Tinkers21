package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import slimeknights.tconstruct.TConstruct;

/** Native component registration for registry-aware tool item/fluid storage. */
public final class ToolDataComponents {
  private static final DeferredRegister<DataComponentType<?>> REGISTER =
    DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TConstruct.MOD_ID);
  public static final DeferredHolder<DataComponentType<?>,DataComponentType<ToolStorage>> STORAGE =
    REGISTER.register("tool_storage", () -> DataComponentType.<ToolStorage>builder()
      .persistent(ToolStorage.CODEC).networkSynchronized(ToolStorage.STREAM_CODEC).build());

  private ToolDataComponents() {}
  public static void register(IEventBus bus) { REGISTER.register(bus); }

  public static ToolStorage storage(IToolStackView tool) {
    ToolStorage storage = tool.getComponent(STORAGE.get());
    return storage == null ? ToolStorage.EMPTY : storage;
  }

  public static void setStorage(IToolStackView tool, ToolStorage storage) {
    tool.setComponent(STORAGE.get(), storage.isEmpty() ? null : storage);
  }
}
