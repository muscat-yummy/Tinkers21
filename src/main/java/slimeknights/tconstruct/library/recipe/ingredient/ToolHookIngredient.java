package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.item.IModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Matches tagged tool items whose current definition supplies the requested hook. */
public class ToolHookIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tconstruct", "tool_hook");
  public static final LoadableIngredientSerializer<ToolHookIngredient> SERIALIZER = new LoadableIngredientSerializer<>(RecordLoadable.create(
    Loadables.ITEM_TAG.defaultField("tag", TinkerTags.Items.MODIFIABLE, value -> value.tag),
    ToolHooks.LOADER.requiredField("hook", value -> value.hook),
    ToolHookIngredient::new));
  public static final IngredientType<ToolHookIngredient> TYPE = SERIALIZER.type("tag", "hook");
  private final TagKey<Item> tag;
  private final ModuleHook<?> hook;

  protected ToolHookIngredient(TagKey<Item> tag, ModuleHook<?> hook) {
    this.tag = Objects.requireNonNull(tag);
    this.hook = Objects.requireNonNull(hook);
  }

  public static Ingredient of(TagKey<Item> tag, ModuleHook<?> hook) {
    return new ToolHookIngredient(tag, hook).toVanilla();
  }

  public static Ingredient of(ModuleHook<?> hook) {
    return of(TinkerTags.Items.MODIFIABLE, hook);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && stack.is(tag) && stack.getItem() instanceof IModifiable modifiable
      && modifiable.getToolDefinition().getData().getHooks().hasHook(hook);
  }

  @Override
  public Stream<ItemStack> getItems() {
    List<ItemStack> list = new ArrayList<>();
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (holder.value() instanceof IModifiable modifiable && modifiable.getToolDefinition().getData().getHooks().hasHook(hook)) {
        list.add(new ItemStack(modifiable));
      }
    }
    if (list.isEmpty()) {
      ItemStack barrier = new ItemStack(Blocks.BARRIER);
      barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Empty Tag: " + tag.location()));
      list.add(barrier);
    }
    return list.stream();
  }

  @Override
  public boolean isSimple() {
    return true;
  }

  @Override
  public IngredientType<ToolHookIngredient> getType() {
    return TYPE;
  }

  public JsonObject toJson() {
    return SERIALIZER.serialize(this);
  }

  @Override
  public boolean equals(Object object) {
    return object == this || object instanceof ToolHookIngredient other
      && tag.equals(other.tag) && hook.getId().equals(other.hook.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag, hook.getId());
  }
}
