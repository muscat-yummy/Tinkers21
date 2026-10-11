package slimeknights.tconstruct.port;

import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.utils.ResourceId;

public final class ResourceIdSmoke {
  private static final class Id extends ResourceId {
    private Id(String namespace, String path, Void validated) { super(namespace, path, validated); }
    private Id(String value) { super(value); }
    private Id(String namespace, String path) { super(namespace, path); }
    private static Id parseNullable(String value) { return tryParse(value, (n, p) -> new Id(n, p, null)); }
    private static Id buildNullable(String namespace, String path) { return tryBuild(namespace, path, (n, p) -> new Id(n, p, null)); }
  }

  private static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    ResourceLocation vanilla = ResourceLocation.parse("tconstruct:iron");
    Id typed = new Id("tconstruct:iron");
    check(typed.equals(vanilla) && vanilla.equals(typed), "symmetric equality");
    check(typed.hashCode() == vanilla.hashCode(), "compatible hash keys");
    check(typed.compareTo(vanilla) == 0, "compatible sorting");
    check(new Id("iron").getNamespace().equals("minecraft"), "default namespace");
    check(new Id("tconstruct", "iron").equals(typed), "namespace/path construction");
    check(Id.parseNullable(":iron").getNamespace().equals("minecraft"), "empty namespace");
    check(Id.parseNullable("UPPER:iron") == null, "invalid namespace");
    check(Id.buildNullable("tconstruct", "bad path") == null, "invalid path");
    check(Id.buildNullable("tconstruct", "a/b").getPath().equals("a/b"), "nested path");
    try {
      new Id("UPPER:iron");
      throw new AssertionError("invalid public constructor accepted");
    } catch (net.minecraft.ResourceLocationException expected) {
      // Vanilla validation is preserved.
    }
    System.out.println("ResourceId validation, equality, hash and ordering: PASS");
  }
}
