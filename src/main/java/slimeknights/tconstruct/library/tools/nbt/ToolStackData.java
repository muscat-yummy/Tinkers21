package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Own mutable working data; every attached item receives immutable native component snapshots. */
public final class ToolStackData {
  private static final String DAMAGE = "Damage";
  private static final String UNBREAKABLE = "Unbreakable";
  private static final String HIDE_FLAGS = "HideFlags";
  /** Internal working snapshots are rebuilt explicitly, never through the native load hook. */
  private static final ThreadLocal<Boolean> CREATING_SNAPSHOT = ThreadLocal.withInitial(() -> false);

  public static boolean isCreatingSnapshot() {
    return CREATING_SNAPSHOT.get();
  }

  private static ItemStack snapshot(java.util.function.Supplier<ItemStack> factory) {
    boolean previous = CREATING_SNAPSHOT.get();
    CREATING_SNAPSHOT.set(true);
    try {
      return factory.get();
    } finally {
      if (previous) CREATING_SNAPSHOT.set(true);
      else CREATING_SNAPSHOT.remove();
    }
  }

  private final CompoundTag data;
  private CompoundTag committed;
  private final ItemStack template;
  private Rarity ownedRarity;
  private final Map<ItemStack, State> attached = new IdentityHashMap<>();

  private record State(CustomData customData, Integer damage, Unbreakable unbreakable, DataComponentPatch components) {
    static State read(ItemStack stack) {
      return new State(stack.get(DataComponents.CUSTOM_DATA), stack.get(DataComponents.DAMAGE),
        stack.get(DataComponents.UNBREAKABLE), stack.getComponentsPatch());
    }
    boolean matches(ItemStack stack) {
      return customData == stack.get(DataComponents.CUSTOM_DATA)
        && Objects.equals(damage, stack.get(DataComponents.DAMAGE))
        && Objects.equals(unbreakable, stack.get(DataComponents.UNBREAKABLE))
        && components.equals(stack.getComponentsPatch());
    }
  }

  public ToolStackData(Item item, CompoundTag data) {
    this.data = data;
    this.committed = data.copy();
    this.template = snapshot(() -> new ItemStack(item));
    this.ownedRarity = template.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
    applyFull(template);
    attached.put(template, State.read(template));
  }

  private ToolStackData(ItemStack source, boolean attach) {
    this.data = read(source);
    this.committed = data.copy();
    this.template = snapshot(source::copy);
    this.ownedRarity = source.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
    applyFull(template);
    attached.put(template, State.read(template));
    if (attach && !source.isEmpty()) {
      attached.put(source, State.read(source));
    }
  }

  public static ToolStackData from(ItemStack source, boolean attach) {
    return new ToolStackData(source, attach);
  }

  public static CompoundTag read(ItemStack stack) {
    CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    Integer damage = stack.get(DataComponents.DAMAGE);
    if (damage != null) data.putInt(DAMAGE, damage);
    Unbreakable unbreakable = stack.get(DataComponents.UNBREAKABLE);
    if (unbreakable != null) {
      data.putBoolean(UNBREAKABLE, true);
      int flags = data.getInt(HIDE_FLAGS);
      if (!unbreakable.showInTooltip()) data.putInt(HIDE_FLAGS, flags | 4);
      else if (data.contains(HIDE_FLAGS)) data.putInt(HIDE_FLAGS, flags & ~4);
    }
    return data;
  }

  public CompoundTag data() {
    return data;
  }

  /** Copy includes native components such as custom name and potion contents, but no live bindings. */
  public ToolStackData copy() {
    ToolStackData copy = new ToolStackData(template, false);
    copy.data.getAllKeys().clear();
    copy.data.merge(data.copy());
    copy.committed = copy.data.copy();
    copy.ownedRarity = ownedRarity;
    copy.applyFull(copy.template);
    copy.attached.put(copy.template, State.read(copy.template));
    return copy;
  }

  public void detach() {
    attached.keySet().removeIf(stack -> stack != template);
  }

  public boolean isSameStack(ItemStack stack) {
    State state = attached.get(stack);
    return state != null && stack != ItemStack.EMPTY && state.matches(stack);
  }

  public ItemStack createStack(int count) {
    if (count <= 0 || template.isEmpty()) return ItemStack.EMPTY;
    ItemStack stack = template.copyWithCount(count);
    applyFull(stack);
    attached.put(stack, State.read(stack));
    return stack;
  }

  public ItemStack updateStack(ItemStack stack, boolean copyData) {
    if (stack.isEmpty()) return stack;
    if (stack.getItem() != template.getItem()) throw new IllegalArgumentException("Wrong item in stack");
    stack.applyComponents(template.getComponentsPatch());
    applyFull(stack);
    if (copyData) attached.remove(stack);
    else if (!stack.isEmpty()) attached.put(stack, State.read(stack));
    return stack;
  }

  /** Native component values are already immutable snapshots owned by their type. */
  public <T> T getComponent(net.minecraft.core.component.DataComponentType<T> type) {
    return template.get(type);
  }

  /** Propagate an owned component without hiding edits made by other item users. */
  public <T> void setComponent(net.minecraft.core.component.DataComponentType<T> type, T value) {
    if (type == DataComponents.CUSTOM_DATA || type == DataComponents.DAMAGE || type == DataComponents.UNBREAKABLE || type == DataComponents.RARITY) {
      throw new IllegalArgumentException("Use the owned tool data and durability/rarity APIs for " + type);
    }
    for (Map.Entry<ItemStack, State> entry : attached.entrySet()) {
      ItemStack stack = entry.getKey();
      if (stack.isEmpty()) continue;
      boolean externallyChanged = !entry.getValue().matches(stack);
      if (value == null) stack.remove(type);
      else stack.set(type, value);
      if (!externallyChanged) entry.setValue(State.read(stack));
    }
  }

  /** Owned tool display state is synchronized to every explicitly attached native item. */
  public void setRarity(Rarity rarity) {
    ownedRarity = rarity;
    for (Map.Entry<ItemStack, State> entry : attached.entrySet()) {
      ItemStack stack = entry.getKey();
      if (stack.isEmpty()) continue;
      boolean externallyChanged = !entry.getValue().matches(stack);
      stack.set(DataComponents.RARITY, rarity);
      if (!externallyChanged) entry.setValue(State.read(stack));
    }
  }

  private static boolean vanillaKey(String key) {
    return key.equals(DAMAGE) || key.equals(UNBREAKABLE);
  }

  private void applyFull(ItemStack stack) {
    if (stack.isEmpty()) return;
    writeData(stack, data);
    if (ownedRarity != null) stack.set(DataComponents.RARITY, ownedRarity);
  }

  /** Commit verified tool data directly without constructing another native item stack. */
  public static void writeData(ItemStack stack, CompoundTag data) {
    CompoundTag custom = data.copy();
    custom.remove(DAMAGE);
    custom.remove(UNBREAKABLE);
    if (custom.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    writeDamage(stack, data);
    writeUnbreakable(stack, data);
  }

  private void writeDamage(ItemStack stack) {
    writeDamage(stack, data);
  }

  private static void writeDamage(ItemStack stack, CompoundTag data) {
    if (data.contains(DAMAGE, Tag.TAG_ANY_NUMERIC)) {
      stack.set(DataComponents.DAMAGE, Math.max(0, data.getInt(DAMAGE)));
    }
  }

  private void writeUnbreakable(ItemStack stack) {
    writeUnbreakable(stack, data);
  }

  private static void writeUnbreakable(ItemStack stack, CompoundTag data) {
    if (data.getBoolean(UNBREAKABLE)) {
      stack.set(DataComponents.UNBREAKABLE, new Unbreakable((data.getInt(HIDE_FLAGS) & 4) == 0));
    } else {
      stack.remove(DataComponents.UNBREAKABLE);
    }
  }

  /** Apply changed keys to current components, retaining unrelated changes by other item users. */
  public void commit() {
    Set<String> keys = new HashSet<>(committed.getAllKeys());
    keys.addAll(data.getAllKeys());
    Set<String> changed = new HashSet<>();
    for (String key : keys) {
      if (!Objects.equals(committed.get(key), data.get(key))) changed.add(key);
    }
    if (changed.isEmpty()) return;
    boolean damageChanged = changed.contains(DAMAGE);
    boolean unbreakableChanged = changed.contains(UNBREAKABLE) || changed.contains(HIDE_FLAGS);
    Set<String> customChanged = new HashSet<>(changed);
    customChanged.removeIf(ToolStackData::vanillaKey);
    for (Map.Entry<ItemStack, State> entry : attached.entrySet()) {
      ItemStack stack = entry.getKey();
      if (stack.isEmpty()) continue;
      boolean externallyChanged = !entry.getValue().matches(stack);
      if (!customChanged.isEmpty() || damageChanged || unbreakableChanged) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
          if (damageChanged) tag.remove(DAMAGE);
          if (unbreakableChanged) tag.remove(UNBREAKABLE);
          for (String key : customChanged) {
            Tag value = data.get(key);
            if (value == null) tag.remove(key);
            else tag.put(key, value.copy());
          }
        });
      }
      if (damageChanged) writeDamage(stack);
      if (unbreakableChanged) writeUnbreakable(stack);
      // Our delta preserves outside edits, but the working view must still refresh on next lookup.
      if (!externallyChanged) entry.setValue(State.read(stack));
    }
    committed = data.copy();
  }
}
