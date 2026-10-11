package slimeknights.tconstruct.port;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import slimeknights.tconstruct.smeltery.client.util.TintedVertexBuilder;

public final class TintedVertexSmoke {
  private static final class Capture implements VertexConsumer {
    float[] position, uv, normal;
    int[] color, overlay, light;
    int colors;
    public VertexConsumer addVertex(float x, float y, float z) { position = new float[]{x,y,z}; return this; }
    public VertexConsumer setColor(int r, int g, int b, int a) { color = new int[]{r,g,b,a}; colors++; return this; }
    public VertexConsumer setUv(float u, float v) { uv = new float[]{u,v}; return this; }
    public VertexConsumer setUv1(int u, int v) { overlay = new int[]{u,v}; return this; }
    public VertexConsumer setUv2(int u, int v) { light = new int[]{u,v}; return this; }
    public VertexConsumer setNormal(float x, float y, float z) { normal = new float[]{x,y,z}; return this; }
  }

  public static void main(String[] args) {
    var capture = new Capture();
    VertexConsumer tinted = new TintedVertexBuilder(capture, 128, 64, 255, 127);
    if (tinted.addVertex(1,2,3).setUv(0.25f,0.75f).setUv1(4,5).setUv2(6,7)
        .setNormal(0,1,0).setColor(200,150,100,255) != tinted) {
      throw new AssertionError("Vertex chain escaped tint wrapper");
    }
    if (!Arrays.equals(capture.color, new int[]{100,37,100,127}) || capture.colors != 1
        || !Arrays.equals(capture.position, new float[]{1,2,3})
        || !Arrays.equals(capture.uv, new float[]{0.25f,0.75f})
        || !Arrays.equals(capture.overlay, new int[]{4,5})
        || !Arrays.equals(capture.light, new int[]{6,7})
        || !Arrays.equals(capture.normal, new float[]{0,1,0})) {
      throw new AssertionError("Vertex attributes or exact RGBA tint changed");
    }
    tinted.setColor(0xffffffff);
    if (!Arrays.equals(capture.color, new int[]{128,64,255,127}) || capture.colors != 2) {
      throw new AssertionError("Native packed color bypassed tint or applied it twice");
    }
    System.out.println("Native fluent vertex chains, attribute forwarding and packed RGBA tint exactly once: PASS");
  }
}
