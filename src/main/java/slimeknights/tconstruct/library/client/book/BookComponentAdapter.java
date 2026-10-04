package slimeknights.tconstruct.library.client.book;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;

import java.lang.reflect.Type;

/** Native component JSON; registry-bearing hover data uses the actual current client level. */
final class BookComponentAdapter implements JsonDeserializer<Component>, JsonSerializer<Component> {
  private static DynamicOps<JsonElement> ops() {
    ClientLevel level = Minecraft.getInstance().level;
    return level == null ? JsonOps.INSTANCE : RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
  }

  @Override
  public Component deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
    return ComponentSerialization.CODEC.parse(ops(), json).getOrThrow(JsonParseException::new);
  }

  @Override
  public JsonElement serialize(Component component, Type type, JsonSerializationContext context) {
    return ComponentSerialization.CODEC.encodeStart(ops(), component).getOrThrow(JsonParseException::new);
  }
}
