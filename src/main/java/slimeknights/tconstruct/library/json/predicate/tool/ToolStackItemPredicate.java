package slimeknights.tconstruct.library.json.predicate.tool;

import com.mojang.serialization.Codec;
import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import slimeknights.mantle.data.loadable.common.LoadableJsonCodec;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags.Items;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Native item sub-predicate for matching Tinker tools without extending the final vanilla predicate. */
@RequiredArgsConstructor(staticName = "subPredicate")
public class ToolStackItemPredicate implements ItemSubPredicate {
  public static final ResourceLocation ID = TConstruct.getResource("tool_stack");
  public static final Codec<ToolStackItemPredicate> CODEC =
    LoadableJsonCodec.of(ToolStackPredicate.LOADER).fieldOf("predicate")
      .xmap(ToolStackItemPredicate::subPredicate, value -> value.predicate).codec();
  private static final DeferredRegister<ItemSubPredicate.Type<?>> TYPES =
    DeferredRegister.create(BuiltInRegistries.ITEM_SUB_PREDICATE_TYPE, TConstruct.MOD_ID);
  public static final DeferredHolder<ItemSubPredicate.Type<?>,ItemSubPredicate.Type<ToolStackItemPredicate>> TYPE =
    TYPES.register("tool_stack", () -> new ItemSubPredicate.Type<>(CODEC));

  private final IJsonPredicate<IToolStackView> predicate;

  public static void register(IEventBus bus) {
    TYPES.register(bus);
  }

  public static ItemPredicate ofTool(IJsonPredicate<IToolStackView> predicate) {
    return ItemPredicate.Builder.item().withSubPredicate(TYPE.get(), subPredicate(predicate)).build();
  }

  public static ItemPredicate ofContext(IJsonPredicate<IToolContext> predicate) {
    return ofTool(ToolStackPredicate.context(predicate));
  }

  @Override
  public boolean matches(ItemStack stack) {
    // Non-tools must not be modified merely by evaluating an advancement criterion.
    return stack.is(Items.MODIFIABLE) && predicate.matches(ToolStack.from(stack));
  }
}
