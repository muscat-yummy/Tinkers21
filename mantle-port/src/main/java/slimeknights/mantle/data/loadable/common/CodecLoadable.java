package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import slimeknights.mantle.data.loadable.LoadableOps;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.util.typed.TypedMap;

/** Implementation of a loadable using a codec. Note this will be inefficient when reading from and writing to the network */
public record CodecLoadable<T>(DynamicOps<Tag> ops, Codec<T> codec) implements Loadable<T> {
  public CodecLoadable(Codec<T> codec) {
    this(NbtOps.INSTANCE, codec);
  }

  @Override
  public T convert(JsonElement element, String key, TypedMap context) {
    return codec.parse(LoadableOps.current(), element).getOrThrow();
  }

  @Override
  public JsonElement serialize(T object) {
    return codec.encodeStart(LoadableOps.current(), object).getOrThrow();
  }

  private DynamicOps<Tag> networkOps(FriendlyByteBuf buffer) {
    return buffer instanceof RegistryFriendlyByteBuf registryBuffer
      ? RegistryOps.create(ops, registryBuffer.registryAccess()) : ops;
  }

  @Override
  public T decode(FriendlyByteBuf buffer, TypedMap context) {
    return buffer.readWithCodec(networkOps(buffer), codec, NbtAccounter.unlimitedHeap());
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T object) {
    buffer.writeWithCodec(networkOps(buffer), codec, object);
  }
}
