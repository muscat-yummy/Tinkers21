package example.ticaddon;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import slimeknights.mantle.network.NetworkDirection;
import slimeknights.mantle.network.NetworkEvent;
import slimeknights.mantle.network.NetworkWrapper;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.capability.ToolCapabilityProvider;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerTools;
import java.util.function.Supplier;

/** Compiled separately against public production outputs; a distinct mod container owns registration. */
@Mod(AddonCheck.ID)
public final class AddonCheck {
  public static final String ID = "portaddoncheck";
  public static final ResourceLocation STATE = ResourceLocation.fromNamespaceAndPath(ID, "state");
  public static final MaterialId MATERIAL = new MaterialId(ID, "test_metal");
  public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(ID);
  public static final StaticModifier<SpeedModifier> SPEED = MODIFIERS.register("speed", SpeedModifier::new);
  public static final ModuleHook<Runnable> CUSTOM_HOOK = new ModuleHook<>(
    ResourceLocation.fromNamespaceAndPath(ID, "custom_hook"), Runnable.class, () -> {});
  public static final NetworkWrapper NETWORK = new NetworkWrapper(ResourceLocation.fromNamespaceAndPath(ID, "test"), "1");
  public static volatile long receivedReply;
  public interface StateView { int value(); }
  public static final ItemCapability<StateView, Void> STATE_CAPABILITY = ItemCapability.createVoid(STATE, StateView.class);

  public AddonCheck(IEventBus bus) {
    boolean rejected = false;
    try {
      MODIFIERS.register("speed", Modifier::new);
    } catch (IllegalArgumentException expected) {
      rejected = true;
    }
    require(rejected, "Duplicate addon modifier was accepted");
    MODIFIERS.registerDynamic("data_speed");
    MODIFIERS.register(bus);
    NETWORK.registerPacket(Request.class, Request::new, NetworkDirection.PLAY_TO_SERVER);
    NETWORK.registerPacket(Reply.class, Reply::new, NetworkDirection.PLAY_TO_CLIENT);
    bus.addListener(NETWORK::registerPayloads);
    ToolCapabilityProvider.register((stack, tool) -> new ToolCapabilityProvider.IToolCapabilityProvider() {
      @SuppressWarnings("unchecked")
      public <T> T getCapability(IToolStackView current, ItemCapability<T, Void> capability) {
        return capability == STATE_CAPABILITY ? (T) (StateView) () -> tool.get().getPersistentData().getInt(STATE) : null;
      }
    });
    bus.addListener(net.neoforged.bus.api.EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class,
      event -> ToolCapabilityProvider.registerCapability(event, STATE_CAPABILITY));
  }

  public record Request(long nonce) implements ISimplePacket {
    public Request(FriendlyByteBuf buffer) { this(buffer.readLong()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeLong(nonce); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
      var context = supplier.get();
      require(context.getSender() != null, "Addon request arrived in wrong direction");
      context.enqueueWork(() -> NETWORK.sendTo(new Reply(nonce), context.getSender()));
      context.setPacketHandled(true);
    }
  }

  public record Reply(long nonce) implements ISimplePacket {
    public Reply(FriendlyByteBuf buffer) { this(buffer.readLong()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeLong(nonce); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
      var context = supplier.get();
      require(context.getSender() == null, "Addon reply arrived in wrong direction");
      context.enqueueWork(() -> receivedReply = nonce);
      context.setPacketHandled(true);
    }
  }

  public static final class SpeedModifier extends Modifier implements ToolStatsModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
      builder.addHook(this, ModifierHooks.TOOL_STATS);
      builder.addHook((Runnable) () -> {}, CUSTOM_HOOK);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
      ToolStats.MINING_SPEED.add(builder, 3 * modifier.getLevel());
    }
  }

  public static ItemStack create(HolderLookup.Provider registries) {
    require(SPEED.isBound() && SPEED.get().getId().getNamespace().equals(ID), "Addon modifier registration lost ownership");
    require(SPEED.get().getHooks().hasHook(CUSTOM_HOOK), "Addon custom hook was discarded");
    var material = MaterialRegistry.getMaterial(MATERIAL);
    require(material.getIdentifier().equals(MATERIAL), "Addon material data did not reload");
    var tool = ToolStack.createTool(TinkerTools.pickaxe.get(), TinkerTools.pickaxe.get().getToolDefinition(),
      MaterialNBT.of(material, material, material));
    float base = tool.getStats().get(ToolStats.MINING_SPEED);
    tool.addModifier(SPEED.getId(), 2);
    require(tool.getStats().get(ToolStats.MINING_SPEED) == base + 6, "Addon hook did not rebuild native tool stats");
    tool.addModifier(new slimeknights.tconstruct.library.modifiers.ModifierId(ID, "data_speed"), 1);
    require(tool.getStats().get(ToolStats.MINING_SPEED) == base + 8, "Addon dynamic module did not reload");
    tool.getPersistentData().putInt(STATE, 37);
    var stack = tool.createStack();
    var ops = registries.createSerializationContext(NbtOps.INSTANCE);
    var loaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, stack).getOrThrow()).getOrThrow();
    verify(loaded);
    var capability = loaded.getCapability(STATE_CAPABILITY);
    require(capability != null && capability.value() == 37, "Addon native capability dispatch failed");
    ToolStack.from(loaded).getPersistentData().putInt(STATE, 38);
    require(capability.value() == 38 && loaded.getCapability(STATE_CAPABILITY).value() == 38,
      "Retained addon capability did not refresh component changes");
    ToolStack.from(loaded).getPersistentData().putInt(STATE, 37);
    var detached = loaded.copy();
    ToolStack.from(detached).getPersistentData().putInt(STATE, 99);
    require(ToolStack.from(loaded).getPersistentData().getInt(STATE) == 37, "Addon mutation leaked through native component copy");
    var restored = ToolStack.from(loaded);
    restored.removeModifier(SPEED.getId(), 2);
    require(restored.getStats().get(ToolStats.MINING_SPEED) == base + 2, "Addon modifier removal did not rebuild stats");
    return stack;
  }

  public static void verify(ItemStack stack) {
    var tool = ToolStack.from(stack);
    require(tool.getMaterials().get(0).getId().equals(MATERIAL), "Addon material ID was lost");
    require(tool.getModifierLevel(SPEED.getId()) == 2 && tool.getPersistentData().getInt(STATE) == 37,
      "Addon modifier or owned state was lost in native storage/synchronization");
    require(tool.getStats().get(ToolStats.MINING_SPEED) > 8, "Addon tool stats were lost");
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
