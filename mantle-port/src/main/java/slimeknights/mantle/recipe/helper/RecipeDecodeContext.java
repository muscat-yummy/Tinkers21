package slimeknights.mantle.recipe.helper;

import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

/** The actual enclosing holder ID, scoped only across native recipe decoding. */
public final class RecipeDecodeContext {
  private static final ThreadLocal<ResourceLocation> CURRENT = new ThreadLocal<>();
  private RecipeDecodeContext() {}

  @Nullable
  public static ResourceLocation currentId() {
    return CURRENT.get();
  }

  public static ResourceLocation requireId() {
    return Objects.requireNonNull(currentId(), "Recipe payload requires its actual enclosing RecipeHolder ID");
  }

  /** Nested decodes and failures restore the caller's context without leaking it to another recipe. */
  public static <T> T withId(ResourceLocation id, Supplier<T> operation) {
    Objects.requireNonNull(id, "recipe holder ID");
    ResourceLocation previous = CURRENT.get();
    CURRENT.set(id);
    try {
      return operation.get();
    } finally {
      if (previous == null) CURRENT.remove();
      else CURRENT.set(previous);
    }
  }
}
