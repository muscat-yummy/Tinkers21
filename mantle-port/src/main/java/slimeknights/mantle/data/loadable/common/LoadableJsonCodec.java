package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.LoadableOps;

/** Exposes a legacy loadable as a native codec while retaining real registry operations. */
public final class LoadableJsonCodec {
  private LoadableJsonCodec() {}

  public static <T> Codec<T> of(Loadable<T> loadable) {
    return new Codec<>() {
      @Override
      public <O> DataResult<Pair<T,O>> decode(DynamicOps<O> ops, O input) {
        try {
          JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
          T value = LoadableOps.with(ops, () -> loadable.convert(json, "value"));
          return DataResult.success(Pair.of(value, input));
        } catch (RuntimeException exception) {
          return DataResult.error(() -> "Invalid loadable: " + exception.getMessage());
        }
      }

      @Override
      public <O> DataResult<O> encode(T value, DynamicOps<O> ops, O prefix) {
        try {
          JsonElement json = LoadableOps.with(ops, () -> loadable.serialize(value));
          O encoded = JsonOps.INSTANCE.convertTo(ops, json);
          if (json.isJsonObject()) {
            return ops.getMap(encoded).flatMap(map -> ops.mergeToMap(prefix, map));
          }
          if (json.isJsonArray()) {
            return ops.getStream(encoded).flatMap(stream -> ops.mergeToList(prefix, stream.toList()));
          }
          return ops.mergeToPrimitive(prefix, encoded);
        } catch (RuntimeException exception) {
          return DataResult.error(() -> "Invalid loadable: " + exception.getMessage());
        }
      }
    };
  }
}
