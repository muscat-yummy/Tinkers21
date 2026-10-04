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
    this.template = new ItemStack(item);
    this.ownedRarity = template.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
    applyFull(template);
    attached.put(template, State.read(template));
  }

  private ToolStackData(ItemStack source, boolean attach) {
    this.data = read(source);
    this.committed = data.copy();
    this.template = source.copy();
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
    if (stack.has(DataComponents.UNBREAKABLE)) data.putBoolean(UNBREAKABLE, true);
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
    CompoundTag custom = data.copy();
    custom.remove(DAMAGE);
    custom.remove(UNBREAKABLE);
    if (custom.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    writeDamage(stack);
    writeUnbreakable(stack);
    if (ownedRarity != null) stack.set(DataComponents.RARITY, ownedRarity);
  }

  private void writeDamage(ItemStack stack) {
    if (data.contains(DAMAGE, Tag.TAG_ANY_NUMERIC)) {
      stack.set(DataComponents.DAMAGE, Math.max(0, data.getInt(DAMAGE)));
    }
  }

  private void writeUnbreakable(ItemStack stack) {
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
