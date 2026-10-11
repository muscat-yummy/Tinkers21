package slimeknights.tconstruct.port;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.tconstruct.fluids.util.BottleBrewingRecipe;

import java.util.List;
import java.util.Map;

public final class BottleBrewingSmoke {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    net.neoforged.neoforge.registries.VanillaRegistryPortTestBootstrap.modifyRegistries();
    PotionBrewing.Builder builder = new PotionBrewing.Builder(FeatureFlags.VANILLA_SET);
    PotionBrewing.addVanillaMixes(builder);
    ItemStack output = new ItemStack(Items.GLASS_BOTTLE, 2);
    CompoundTag marker = new CompoundTag();
    marker.putString("port_test", "preserved");
    output.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
    var recipe = new BottleBrewingRecipe(Ingredient.of(Items.GLASS_BOTTLE), Items.POTION,
      Items.SPLASH_POTION, output, builder);
    builder.addRecipe(recipe);
    var gunpowder = new ItemStack(Items.GUNPOWDER, 4);
    var input = new ItemStack(Items.GLASS_BOTTLE);
    check(recipe.isIngredient(gunpowder) && recipe.getIngredient().test(gunpowder),
      "Native vanilla container ingredient missing");
    check(!recipe.isIngredient(new ItemStack(Items.DIAMOND)), "Unrelated ingredient accepted");
    var built = builder.build();
    check(built.getRecipes().contains(recipe), "Recipe was not registered on the native builder");
    check(built.hasMix(input, gunpowder), "Valid single bottle input was rejected");
    check(!built.hasMix(new ItemStack(Items.GLASS_BOTTLE, 3), gunpowder),
      "Native single item brewing slot policy changed");
    ItemStack first = built.mix(gunpowder, input);
    check(first.getCount() == 2 && first.getItem() == Items.GLASS_BOTTLE
      && first.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        .getString("port_test").equals("preserved"), "Output count/components lost");
    first.setCount(1);
    first.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    ItemStack second = built.mix(gunpowder, input);
    check(second.getCount() == 2 && second.has(DataComponents.CUSTOM_DATA),
      "Brewing outputs shared mutable state");
    check(input.getCount() == 1 && gunpowder.getCount() == 4, "Recipe lookup consumed input");

    // An addon can register its mix after our custom recipe's listener runs.
    var addonRecipe = new BottleBrewingRecipe(Ingredient.of(Items.GLASS_BOTTLE),
      Items.LINGERING_POTION, Items.POTION, output, builder);
    check(addonRecipe.getIngredient().isEmpty(), "Absent container mix had an ingredient");
    builder.addContainerRecipe(Items.LINGERING_POTION, Items.REDSTONE, Items.POTION);
    check(addonRecipe.isIngredient(new ItemStack(Items.REDSTONE)), "Later addon mix was not visible");
    builder.addContainerRecipe(Items.LINGERING_POTION, Items.DIAMOND, Items.POTION);
    check(!addonRecipe.isIngredient(new ItemStack(Items.DIAMOND)),
      "Legacy first matching container mix policy changed");
    var otherBuilder = new PotionBrewing.Builder(FeatureFlags.VANILLA_SET);
    var otherRecipe = new BottleBrewingRecipe(Ingredient.of(Items.GLASS_BOTTLE),
      Items.LINGERING_POTION, Items.POTION, output, otherBuilder);
    check(otherRecipe.getIngredient().isEmpty()
      && !otherRecipe.isIngredient(new ItemStack(Items.REDSTONE)),
      "Container mixes leaked between world builders");
    System.out.println("Native bottle brewing registration, components, first-match addon mixes and world scope: PASS");
  }
}
