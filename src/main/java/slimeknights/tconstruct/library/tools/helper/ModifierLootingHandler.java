package slimeknights.tconstruct.library.tools.helper;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.hook.combat.ArmorLootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.nbt.DummyToolStack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.shared.TinkerEffects;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logic to handle the looting event for all main tinker tools
 */
public class ModifierLootingHandler {
  /** If contained in the set, they should use the offhand for looting */
  private static final Map<UUID,EquipmentSlot> LOOTING_OFFHAND = new HashMap<>();
  private static boolean init = false;

  /** Initializies this listener */
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    NeoForge.EVENT_BUS.addListener(ModifierLootingHandler::onLeaveServer);
  }

  /**
   * Sets the hand used for looting, so the tool is fetched from the proper context
   * @param entity    Player to set
   * @param slotType  Slot type
   */
  public static void setLootingSlot(LivingEntity entity, EquipmentSlot slotType) {
    if (slotType == EquipmentSlot.MAINHAND) {
      LOOTING_OFFHAND.remove(entity.getUUID());
    } else {
      LOOTING_OFFHAND.put(entity.getUUID(), slotType);
    }
  }

  /** Gets the slot to use for looting */
  public static EquipmentSlot getLootingSlot(@Nullable LivingEntity entity) {
    return entity != null ? LOOTING_OFFHAND.getOrDefault(entity.getUUID(), EquipmentSlot.MAINHAND) : EquipmentSlot.MAINHAND;
  }

  /** Applies the looting bonus for modifiers */
  public static int getLooting(LivingEntity target, @Nullable DamageSource damageSource, int level) {
    if (damageSource == null) {
      return level;
    }

    // bleeding kills use the level of the effect for looting
    if (damageSource.is(TinkerDamageTypes.BLEEDING)) {
      return Math.max(0, TinkerEffect.getAmplifier(target, TinkerEffects.bleeding.get()));
    }

    // otherwise, use the proper tool
    Entity source = damageSource.getEntity();
    if (source instanceof LivingEntity holder) {
      Entity direct = damageSource.getDirectEntity();

      // determine who is in charge of the looting
      LootingContext context;
      IToolStackView tool = null;
      if (direct instanceof Projectile) {
        // need to build a context from the relevant capabilities to use the modifier
        ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(direct);
        context = new LootingContext(holder, target, damageSource, null);
        // no modifiers means its not a projectile we fired, so just defer to dumb vanilla behavior of whatever looting
        // since we don't set the enchantment on our tools, our looting modifiers won't set anything here anyways
        if (!modifiers.isEmpty()) {
          ModDataNBT persistentData = PersistentDataCapability.getOptionalData(direct).orElseGet(ModDataNBT::new);
          level = LootingModifierHook.getLooting(new DummyToolStack(Items.AIR, modifiers, persistentData), context, 0);
        }
      } else {
        // not an arrow? means the held tool is to blame
        EquipmentSlot slotType = getLootingSlot(holder);
        context = new LootingContext(holder, target, damageSource, slotType);
        ItemStack held = holder.getItemBySlot(slotType);

        // if its modifiable, let it increase the level
        if (held.is(TinkerTags.Items.MODIFIABLE)) {
          tool = ToolStack.from(held);
          level = LootingModifierHook.getLooting(tool, context, level);
        } else if (slotType != EquipmentSlot.MAINHAND) {
          // if it's not modifiable, yet we have a lot marked to blame for looting, ignore the event value
          level = 0;
        }
      }
      // boost looting with armor regardless, hopefully you did not switch your pants mid arrow firing
      level = ArmorLootingModifierHook.getLooting(tool, context, level);
      // we allow the hook to return negatives to cancel out looting, so ensure its at least 0
      return Math.max(level, 0);
    }
    return level;
  }

  /**
   * Supplies a query holder for bleeding kills without replacing the actual loot context.
   * Native enchanted loot functions otherwise skip querying a level when no living attacker exists.
   */
  public static <T> T getLootingQueryEntity(LootContext context, LootContextParam<T> param, Holder<Enchantment> enchantment) {
    T value = context.getParamOrNull(param);
    if (!(value instanceof LivingEntity) && param == LootContextParams.ATTACKING_ENTITY
        && enchantment.is(Enchantments.LOOTING)) {
      DamageSource damage = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
      Entity target = context.getParamOrNull(LootContextParams.THIS_ENTITY);
      if (damage != null && damage.is(TinkerDamageTypes.BLEEDING) && target instanceof LivingEntity) {
        @SuppressWarnings("unchecked")
        T queryEntity = (T) target;
        return queryEntity;
      }
    }
    return value;
  }

  /** Applies context-aware tool looting to native count and chance queries. */
  public static int getEnchantmentLooting(Holder<Enchantment> enchantment, LivingEntity queryEntity, LootContext context) {
    // A victim supplied above is only a query holder; its equipped enchantments are not an attacker bonus.
    Entity attacker = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
    Entity target = context.getParamOrNull(LootContextParams.THIS_ENTITY);
    DamageSource damage = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
    if (enchantment.is(Enchantments.LOOTING) && target instanceof LivingEntity living
        && damage != null && damage.is(TinkerDamageTypes.BLEEDING) && !(attacker instanceof LivingEntity)) {
      return getLooting(living, damage, 0);
    }
    int queried = EnchantmentHelper.getEnchantmentLevel(enchantment, queryEntity);
    if (!enchantment.is(Enchantments.LOOTING) || !(target instanceof LivingEntity living) || damage == null) {
      return queried;
    }
    int vanilla = 0;
    for (ItemStack stack : enchantment.value().getSlotItems(queryEntity).values()) {
      vanilla = Math.max(vanilla, stack.getEnchantments().getLevel(enchantment));
    }
    // Preserve NeoForge query-event adjustments even when projectile/offhand logic replaces the vanilla base.
    return Math.max(0, getLooting(living, damage, vanilla) + queried - vanilla);
  }

  /** Called when a player leaves the server to clear the face */
  private static void onLeaveServer(PlayerLoggedOutEvent event) {
    LOOTING_OFFHAND.remove(event.getEntity().getUUID());
  }
}
