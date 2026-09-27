package slimeknights.mantle.network;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Adapter for handlers that still use Mantle's original packet interface. */
public final class NetworkEvent {
  private NetworkEvent() {}

  public static final class Context {
    private final IPayloadContext delegate;

    Context(IPayloadContext delegate) {
      this.delegate = delegate;
    }

    @Nullable
    public ServerPlayer getSender() {
      return delegate.flow() == PacketFlow.SERVERBOUND ? (ServerPlayer) delegate.player() : null;
    }

    public CompletableFuture<Void> enqueueWork(Runnable task) {
      return delegate.enqueueWork(task);
    }

    /** NeoForge marks payload dispatch as handled automatically. */
    public void setPacketHandled(boolean handled) {
      if (!handled) {
        throw new IllegalArgumentException("NeoForge payloads cannot be marked unhandled");
      }
    }
  }
}
