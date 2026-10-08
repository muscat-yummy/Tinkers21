package slimeknights.tconstruct.library.tools.nbt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

/** Tool stack instance in a context that does not have a proper tool, used for projectils */
@SuppressWarnings("ClassCanBeRecord")
@Getter
public class DummyToolStack implements IToolStackView {
  private final Item item;
  private final ModifierNBT modifiers;
  private final ModDataNBT persistentData;
  private final net.minecraft.core.component.PatchedDataComponentMap components;

  public DummyToolStack(Item item, ModifierNBT modifiers, ModDataNBT persistentData) {
    this.item = item;
    this.modifiers = modifiers;
    this.persistentData = persistentData;
    this.components = new net.minecraft.core.component.PatchedDataComponentMap(item.components());
  }

  @Override
  public <T> T getComponent(net.minecraft.core.component.DataComponentType<T> type) {
    return components.get(type);
  }

  @Override
  public <T> void setComponent(net.minecraft.core.component.DataComponentType<T> type, T value) {
    if (value == null) components.remove(type);
    else components.set(type, value);
  }

  @Override
  public ToolDefinition getDefinition() {
    return ToolDefinition.EMPTY;
  }

  @Override
  public MaterialNBT getMaterials() {
    return MaterialNBT.EMPTY;
  }

  @Override
  public ModifierNBT getUpgrades() {
    return ModifierNBT.EMPTY;
  }

  @Override
  public IModDataView getVolatileData() {
    return IModDataView.EMPTY;
  }

  @Override
  public int getDamage() {
    return 0;
  }

  @Override
  public int getCurrentDurability() {
    return 0;
  }

  @Override
  public boolean isBroken() {
    return false;
  }

  @Override
  public boolean isUnbreakable() {
    return false;
  }

  @Override
  public void setDamage(int damage) {}

  @Override
  public StatsNBT getStats() {
    return StatsNBT.EMPTY;
  }

  @Override
  public MultiplierNBT getMultipliers() {
    return MultiplierNBT.EMPTY;
  }
}
