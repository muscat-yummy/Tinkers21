package slimeknights.tconstruct.library.tools.capability.inventory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.item.ItemPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.IntRange;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.modifiers.util.ModuleWithKey;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.InventoryModifierHook;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability.StackMatch;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryModule implements ModifierModule, InventoryModifierHook, VolatileDataModifierHook, ValidateModifierHook, ModifierRemovalHook, ModuleWithKey, ConditionalModule<IToolContext>, SlotStackModifierHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<InventoryModule>defaultHooks(ToolInventoryCapability.HOOK, ModifierHooks.VOLATILE_DATA, ModifierHooks.VALIDATE, ModifierHooks.REMOVE, ModifierHooks.SLOT_STACK);
  /** Mod Data NBT mapper to get a compound list */
  public static final BiFunction<CompoundTag,String,ListTag> GET_COMPOUND_LIST = (nbt, name) -> nbt.getList(name, Tag.TAG_COMPOUND);
  /** Error for if the container has items preventing modifier removal */
  private static final Component HAS_ITEMS = TConstruct.makeTranslation("modifier", "inventory_cannot_remove");
  /** NBT key to store the slot for a stack */
  public static final String TAG_SLOT = "Slot";
  // fields
  protected static final LoadableField<ResourceLocation,? super InventoryModule> KEY_FIELD = ModuleWithKey.FIELD;
  protected static final LoadableField<LevelingInt, InventoryModule> SLOTS_FIELD = LevelingInt.LOADABLE.requiredField("slots", InventoryModule::slots);
  protected static final LoadableField<LevelingInt, InventoryModule> LIMIT_FIELD = LevelingInt.LOADABLE.defaultField("limit", LevelingInt.flat(64), InventoryModule::slotLimit);
  protected static final LoadableField<IJsonPredicate<Item>, InventoryModule> FILTER_FIELD = ItemPredicate.LOADER.defaultField("filter", InventoryModule::filter);
  protected static final LoadableField<Pattern, InventoryModule> PATTERN_FIELD = Pattern.PARSER.nullableField("pattern", InventoryModule::pattern);
  protected static final LoadableField<IntRange,InventoryModule> VALIDATION_FIELD = ModifierEntry.VALID_LEVEL.defaultField("validation_level", InventoryModule::validationLevel);
  /** Loader instance */
  public static final RecordLoadable<InventoryModule> LOADER = RecordLoadable.create(KEY_FIELD, SLOTS_FIELD, LIMIT_FIELD, FILTER_FIELD, PATTERN_FIELD, ModifierCondition.CONTEXT_FIELD, VALIDATION_FIELD, InventoryModule::new);

  /** Module adding an inventory to a tool */
  private final @Nullable ResourceLocation key;
  /** Location to save the inventory */
  private final LevelingInt slots;
  /** Slots to add to the tool */
  private final LevelingInt slotLimit;
  /** Maximum stack size in each slot */
  private final IJsonPredicate<Item> filter;
  /** Filter for valid items in the slot */
  private final @Nullable Pattern pattern;
  /** Slot background to show */
  private final ModifierCondition<IToolContext> condition;
  /** Additional conditions */
  private final IntRange validationLevel;

  @Override
  public RecordLoadable<? extends InventoryModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }


  /* Properties */

  /** Gets the number of slots at the given level, assuming this module is active */
  private int getPotentialSlots(float level) {
    return Math.max(0, slots.computeForLevel(level));
  }

  @Override
  public int getSlots(IToolStackView tool, ModifierEntry modifier) {
    return condition.matches(tool, modifier) ? getPotentialSlots(modifier.getEffectiveLevel()) : 0;
  }

  @Override
  public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
    if (condition.matches(context, modifier)) {
      ToolInventoryCapability.addSlots(volatileData, getPotentialSlots(modifier.getEffectiveLevel()));
    }
  }

  @Override
  public int getSlotLimit(IToolStackView tool, ModifierEntry modifier, int slot) {
    return slotLimit.compute(modifier.getEffectiveLevel());
  }

  @Override
  public boolean isItemValid(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
    return condition.matches(tool, modifier) && filter.matches(stack.getItem());
  }

  @Nullable
  @Override
  public Pattern getPattern(IToolStackView tool, ModifierEntry modifier, int slot, boolean hasStack) {
    return hasStack ? null : pattern;
  }


  /* Inventory */

  @Override
  public ItemStack getStack(IToolStackView tool, ModifierEntry modifier, int slot) {
    if (slot < 0 || slot >= getSlots(tool, modifier)) {
      return ItemStack.EMPTY;
    }
    return ToolDataComponents.storage(tool).inventory(getKey(modifier.getModifier())).getOrDefault(slot, ItemStack.EMPTY);
  }

  /** Updates only item contents; callers maintaining cooking metadata commit it separately. */
  public void setStackContents(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
    if (slot < 0 || slot >= getSlots(tool, modifier)) {
      return;
    }
    ResourceLocation key = getKey(modifier.getModifier());
    var storage = ToolDataComponents.storage(tool);
    Map<Integer,ItemStack> inventory = storage.inventory(key);
    if (stack.isEmpty()) {
      inventory.remove(slot);
    } else {
      inventory.put(slot, stack);
    }
    ToolDataComponents.setStorage(tool, storage.withInventory(key, inventory));
  }

  @Override
  public void setStack(IToolStackView tool, ModifierEntry modifier, int slot, ItemStack stack) {
    if (slot < 0 || slot >= getSlots(tool, modifier)) {
      return;
    }
    setStackContents(tool, modifier, slot, stack);
    ModDataNBT data = tool.getPersistentData();
    ResourceLocation key = getKey(modifier.getModifier());
    ListTag list = data.get(key, GET_COMPOUND_LIST);
    int insert = 0;
    for (int i = 0; i < list.size(); i++) {
      int storedSlot = list.getCompound(i).getInt(TAG_SLOT);
      if (storedSlot == slot) {
        list.remove(i);
        break;
      }
      if (storedSlot < slot) {
        insert = i + 1;
      }
    }
    if (!stack.isEmpty()) {
      CompoundTag entry = new CompoundTag();
      entry.putInt(TAG_SLOT, slot);
      list.add(Math.min(insert, list.size()), entry);
    }
    if (list.isEmpty()) {
      data.remove(key);
    } else {
      data.put(key, list);
    }
  }

  /* Validation */

  @Nullable
  @Override
  public Component validate(IToolStackView tool, ModifierEntry modifier) {
    if (condition.tool().matches(tool) && validationLevel.test(modifier.getLevel())) {
      ResourceLocation key = getKey(modifier.getModifier());
      var storage = ToolDataComponents.storage(tool);
      Map<Integer,ItemStack> inventory = storage.inventory(key);
      if (!inventory.isEmpty()) {
        int maxSlots = getSlots(tool, modifier);
        if (inventory.size() > maxSlots) {
          return HAS_ITEMS;
        }
        BitSet freeSlots = new BitSet(maxSlots);
        freeSlots.set(0, maxSlots);
        for (int slot : inventory.keySet()) {
          if (slot < maxSlots) {
            freeSlots.clear(slot);
          }
        }
        Map<Integer,Integer> moves = new HashMap<>();
        for (int slot : new TreeMap<>(inventory).keySet()) {
          if (slot >= maxSlots) {
            int free = freeSlots.nextSetBit(0);
            if (free < 0) {
              return HAS_ITEMS;
            }
            freeSlots.clear(free);
            moves.put(slot, free);
          }
        }
        if (!moves.isEmpty()) {
          Map<Integer,ItemStack> moved = new HashMap<>();
          inventory.forEach((slot, stack) -> moved.put(moves.getOrDefault(slot, slot), stack));
          ModDataNBT persistent = tool.getPersistentData();
          ListTag metadata = persistent.get(key, GET_COMPOUND_LIST);
          for (int i = 0; i < metadata.size(); i++) {
            CompoundTag entry = metadata.getCompound(i);
            int slot = entry.getInt(TAG_SLOT);
            if (moves.containsKey(slot)) {
              entry.putInt(TAG_SLOT, moves.get(slot));
            }
          }
          ToolDataComponents.setStorage(tool, storage.withInventory(key, moved));
          persistent.put(key, metadata);
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Component onRemoved(IToolStackView tool, Modifier modifier) {
    ResourceLocation key = getKey(modifier);
    if (!ToolDataComponents.storage(tool).inventory(key).isEmpty()) {
      return HAS_ITEMS;
    }
    tool.getPersistentData().remove(key);
    return null;
  }

  /* Helpers */

  /**
   * Writes a stack to NBT, including the slot
   * @param stack  Stack to write
   * @param slot   Target slot
   * @param compound  Tag to write to, use new for new tag
   * @return Tag written to, same as {@code compound}.
   */
  public static CompoundTag writeStack(ItemStack stack, int slot, CompoundTag compound, HolderLookup.Provider registries) {
    stack.save(registries, compound);
    compound.putInt(TAG_SLOT, slot);
    return compound;
  }

  @Override
  public StackMatch findStack(IToolStackView tool, ModifierEntry modifier, Predicate<ItemStack> predicate) {
    int max = getSlots(tool, modifier);
    if (max > 0) {
      Map<Integer,ItemStack> inventory = ToolDataComponents.storage(tool).inventory(getKey(modifier.getModifier()));
      for (var entry : new TreeMap<>(inventory).entrySet()) {
        if (entry.getKey() < max && predicate.test(entry.getValue())) {
          return new StackMatch(entry.getValue(), entry.getKey());
        }
      }
    }
    return StackMatch.EMPTY;
  }

  @Override
  public List<ItemStack> getAllStacks(IToolStackView tool, ModifierEntry entry, List<ItemStack> stackList) {
    int max = getSlots(tool, entry);
    if (max > 0) {
      Map<Integer,ItemStack> inventory = ToolDataComponents.storage(tool).inventory(getKey(entry.getModifier()));
      for (var stored : new TreeMap<>(inventory).entrySet()) {
        if (stored.getKey() < max) {
          stackList.add(stored.getValue());
        }
      }
    }
    return stackList;
  }

  /* Slot interaction */

  @Override
  public boolean overrideOtherStackedOnMe(IToolStackView slotTool, ModifierEntry modifier, ItemStack held, Slot slot, Player player, SlotAccess access) {
    if (getSlots(slotTool, modifier) > 0) {
      return InventorySlotMenuModule.INSTANCE.overrideOtherStackedOnMe(slotTool, modifier, held, slot, player, access);
    }
    return false;
  }

  /* Builder */

  /** Creates a new builder instance */
  public static Builder builder() {
    return new Builder();
  }

  @Accessors(fluent = true)
  @Setter
  public static class Builder extends ModuleBuilder.Context<Builder> {
    @Nullable
    protected ResourceLocation key = null;
    protected LevelingInt slotLimit = LevelingInt.flat(64);
    protected IJsonPredicate<Item> filter = ItemPredicate.ANY;
    @Nullable
    protected Pattern pattern = null;
    protected IntRange validationLevel = ModifierEntry.VALID_LEVEL;

    protected Builder() {}

    /** Copies properties from the given module, excluding slots which is terminal. */
    public Builder from(InventoryModule inventory) {
      this.key = inventory.key;
      this.slotLimit = inventory.slotLimit;
      this.filter = inventory.filter;
      this.pattern = inventory.pattern;
      this.validationLevel = inventory.validationLevel;
      this.condition = inventory.condition;
      return this;
    }

    /** Sets the base slot limit */
    public Builder flatLimit(int limit) {
      return slotLimit(LevelingInt.flat(limit));
    }

    /** Sets the base slot limit */
    public Builder limitPerLevel(int limit) {
      return slotLimit(LevelingInt.eachLevel(limit));
    }

    /** Builds the final instance */
    public InventoryModule slots(LevelingInt slots) {
      return new InventoryModule(key, slots, slotLimit, filter, pattern, condition, validationLevel);
    }

    /** Builds the final instance */
    public InventoryModule slots(int base, int perLevel) {
      return slots(new LevelingInt(base, perLevel));
    }

    /** Builds the final instance */
    public InventoryModule flatSlots(int slots) {
      return slots(slots, 0);
    }

    /** Builds the final instance */
    public InventoryModule slotsPerLevel(int slots) {
      return slots(0, slots);
    }
  }
}
