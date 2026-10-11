package slimeknights.mantle.registration;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import slimeknights.mantle.block.entity.InventoryBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Item providers for block entity types registered through Mantle's helpers. */
public final class MantleBlockEntityCapabilities {
  private static final List<Supplier<? extends BlockEntityType<?>>> TYPES = new ArrayList<>();

  private MantleBlockEntityCapabilities() {}

  /** Track a type before RegisterCapabilitiesEvent; suppliers are not resolved during construction. */
  public static void track(Supplier<? extends BlockEntityType<?>> type) {
    synchronized (TYPES) {
      TYPES.add(type);
    }
  }

  public static void register(RegisterCapabilitiesEvent event) {
    List<Supplier<? extends BlockEntityType<?>>> types;
    synchronized (TYPES) {
      types = List.copyOf(TYPES);
    }
    Set<BlockEntityType<?>> registered = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Supplier<? extends BlockEntityType<?>> supplier : types) {
      BlockEntityType<?> type = supplier.get();
      if (registered.add(type)) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
          (entity, side) -> entity instanceof InventoryBlockEntity inventory ? inventory.getItemHandler(side) : null);
      }
    }
  }
}
