package slimeknights.tconstruct.port;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.armor.AbstractArmorModel;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.TinkerTools;

/** Test-source-only actual equipment synchronization and native third-person armor rendering. */
public final class ArmorRenderingCheck {
  private static int family;
  private static int frames;
  private static boolean requested;
  private static volatile List<ItemStack> equipment = List.of();
  private static volatile RuntimeException failure;

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  public static boolean tick(Minecraft minecraft) {
    if (failure != null) throw failure;
    if (!requested) {
      requested = true;
      frames = 0;
      minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
      var server = minecraft.getSingleplayerServer();
      var uuid = minecraft.player.getUUID();
      server.execute(() -> {
        try {
          var player = server.getPlayerList().getPlayer(uuid);
          List<ItemStack> actual = new ArrayList<>();
          for (ArmorItem.Type type : ModifiableArmorMaterial.ARMOR_TYPES) {
            var item = switch (family) {
              case 0 -> TinkerTools.plateArmor.get(type);
              case 1 -> TinkerTools.travelersGear.get(type);
              default -> TinkerTools.slimesuit.get(type);
            };
            ItemStack armor = ToolBuildHandler.buildItemRandomMaterials(item, RandomSource.create(400 + family * 10 + type.ordinal()));
            require(!armor.isEmpty() && !ToolStack.from(armor).getMaterials().isEmpty(),
              "Actual armor definition did not build a material-dependent item");
            actual.add(armor.copy());
            player.setItemSlot(type.getSlot(), armor);
          }
          equipment = List.copyOf(actual);
        } catch (Throwable problem) {
          failure = new IllegalStateException("Actual armor equipment setup failed", problem);
        }
      });
      return false;
    }
    if (equipment.size() != 4 || minecraft.screen != null || minecraft.getOverlay() != null) return false;
    for (int i = 0; i < 4; i++) {
      var type = ModifiableArmorMaterial.ARMOR_TYPES[i];
      var stack = minecraft.player.getItemBySlot(type.getSlot());
      if (stack.getItem() != equipment.get(i).getItem()) {
        frames = 0;
        return false;
      }
      require(ToolStack.from(stack).getMaterials().equals(ToolStack.from(equipment.get(i)).getMaterials()),
        "Actual armor equipment packets changed material data");
      var layer = type == ArmorItem.Type.LEGGINGS ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR;
      var base = new HumanoidModel<>(minecraft.getEntityModels().bakeLayer(layer));
      var model = IClientItemExtensions.of(stack).getGenericArmorModel(minecraft.player, stack, type.getSlot(), base);
      require(model instanceof AbstractArmorModel, "Native armor extension fell back to the generic armor model");
      String name = family == 0 ? "plate" : family == 1 ? "travelers" :
        type == ArmorItem.Type.HELMET ? "slimeskull" : "slime";
      var textures = ArmorModelManager.INSTANCE.getModel(TConstruct.getResource(name));
      require(!textures.layers().isEmpty(), "Actual armor resource has no material/overlay layers");
      require(textures.layers().stream().anyMatch(supplier -> supplier.getArmorTexture(stack,
        ArmorTextureSupplier.TextureType.fromSlot(type.getSlot()), minecraft.level.registryAccess()) != ArmorTextureSupplier.ArmorTexture.EMPTY),
        "Actual equipped armor has no resolved texture layer");
    }
    if (++frames < 60) return false;
    TConstruct.LOG.info("TiC actual third-person armor family {} and four equipment slots PASS", family);
    if (++family == 3) {
      TConstruct.LOG.info("TiC actual material armor equipment and native rendering PASS");
      return true;
    }
    requested = false;
    equipment = List.of();
    return false;
  }
}
