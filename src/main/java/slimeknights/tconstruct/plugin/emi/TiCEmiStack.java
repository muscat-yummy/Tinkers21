package slimeknights.tconstruct.plugin.emi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.core.registries.BuiltInRegistries;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;
import slimeknights.tconstruct.tables.TinkerTables;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Logical material/pattern/modifier/entity ingredients; never masquerade as craftable icon items. */
public final class TiCEmiStack extends EmiStack {
  public record Key(String kind,String value) {}
  private final Key key;
  private final Component name;
  private final ItemStack icon;
  public TiCEmiStack(String kind,String value) {
    key=new Key(kind,value);
    switch(kind) {
      case "material" -> {
        var variant=MaterialVariantId.parse(value);
        name=MaterialTooltipCache.getDisplayName(variant);
        icon=TinkerToolParts.repairKit.get().withMaterialForDisplay(variant);
      }
      case "pattern" -> {
        var pattern=new slimeknights.tconstruct.library.recipe.partbuilder.Pattern(value);
        name=pattern.getDisplayName();icon=new ItemStack(TinkerTables.pattern.get());
      }
      case "modifier" -> {
        var id=new ModifierId(value);
        name=new slimeknights.tconstruct.library.modifiers.util.LazyModifier(id).get().getDisplayName();
        icon=ModifierCrystalItem.withModifier(id);
      }
      case "entity" -> {
        var entity=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(value));
        name=entity.getDescription();var egg=SpawnEggItem.byId(entity);
        icon=new ItemStack(egg==null?Items.IRON_SWORD:egg);
      }
      default -> throw new IllegalArgumentException("Unknown TiC ingredient kind: "+kind);
    }
  }
  @Override public TiCEmiStack copy() { var copy=new TiCEmiStack(key.kind(),key.value());copy.amount=amount;copy.chance=chance;copy.comparison=comparison;return copy; }
  @Override public boolean isEmpty() { return false; }
  @Override public DataComponentPatch getComponentChanges() { return DataComponentPatch.EMPTY; }
  @Override public Object getKey() { return key; }
  @Override public ResourceLocation getId() { return TConstruct.getResource("emi/"+key.kind()+"/"+UUID.nameUUIDFromBytes(key.value().getBytes(StandardCharsets.UTF_8))); }
  @Override public Component getName() { return name; }
  @Override public List<Component> getTooltipText() { return List.of(name,Component.literal(key.value()),Component.literal("× "+amount)); }
  @Override public List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> getTooltip() {
    return getTooltipText().stream().map(c->net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(c.getVisualOrderText())).toList();
  }
  @Override public void render(GuiGraphics g,int x,int y,float delta,int flags) { g.renderFakeItem(icon,x,y);if((flags&RENDER_AMOUNT)!=0 && amount!=1)g.drawString(net.minecraft.client.Minecraft.getInstance().font,Long.toString(amount),x+8,y+9,-1,true); }
  public static final EmiIngredientSerializer<TiCEmiStack> SERIALIZER=new EmiIngredientSerializer<>() {
    @Override public String getType(){ return "tconstruct:native"; }
    @Override public JsonElement serialize(TiCEmiStack stack){ var json=new JsonObject();json.addProperty("type",getType());json.addProperty("kind",stack.key.kind());json.addProperty("value",stack.key.value());json.addProperty("amount",stack.getAmount());return json; }
    @Override public EmiIngredient deserialize(JsonElement element){var json=element.getAsJsonObject();return new TiCEmiStack(json.get("kind").getAsString(),json.get("value").getAsString()).setAmount(json.has("amount")?json.get("amount").getAsLong():1);}
  };
}
