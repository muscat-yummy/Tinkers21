package slimeknights.tconstruct.world.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PiglinHeadModel;
import net.minecraft.client.model.geom.ModelPart;

/** Extension of {@link PiglinHeadModel} to adjust scale for slimeskulls */
public class PiglinSkullModel extends PiglinHeadModel {
  public PiglinSkullModel(ModelPart pRoot) {
    super(pRoot);
  }

  @Override
  public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int packedColor) {
    poseStack.pushPose();
    poseStack.scale(0.97f, 0.97f, 0.97f);
    super.renderToBuffer(poseStack, buffer, light, overlay, packedColor);
    poseStack.popPose();
  }
}
