package slimeknights.tconstruct.tables.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import slimeknights.mantle.client.render.InventoryBlockEntityRenderer;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;

/** Retains table display bounds through NeoForge's native renderer callback. */
public class TableBlockEntityRenderer extends InventoryBlockEntityRenderer<TableBlockEntity> {
  public TableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public AABB getRenderBoundingBox(TableBlockEntity table) {
    return table.getRenderBoundingBox();
  }
}
