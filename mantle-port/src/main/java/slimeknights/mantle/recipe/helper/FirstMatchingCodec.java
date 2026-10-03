package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/** Lazily decodes the first enabled alternative, using the caller's actual reload ops. */
public final class FirstMatchingCodec {
  private FirstMatchingCodec() {}

  public static <A> MapCodec<A> create(Codec<Optional<A>> entryCodec) {
    return new MapCodec<>() {
      @Override
      public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        T recipes = input.get("recipes");
        if (recipes == null) {
          return DataResult.error(() -> "Missing conditional recipe alternatives");
        }
        return ops.getStream(recipes).flatMap(stream -> {
          try (stream) {
            Iterator<T> entries = stream.iterator();
            while (entries.hasNext()) {
              DataResult<Optional<A>> result = entryCodec.parse(ops, entries.next());
              if (result.error().isPresent()) {
                return DataResult.error(() -> result.error().orElseThrow().message());
              }
              Optional<A> decoded = result.result().orElseThrow();
              if (decoded.isPresent()) {
                return DataResult.success(decoded.orElseThrow());
              }
            }
          }
          return DataResult.error(() -> "No conditional recipe matched; an outer condition must disable this entry");
        });
      }

      @Override
      public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return prefix.add("recipes", entryCodec.encodeStart(ops, Optional.of(input))
            .map(entry -> ops.createList(Stream.of(entry))));
      }

      @Override
      public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString("recipes"));
      }
    };
  }
}
