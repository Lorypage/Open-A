package cn.lazymoon.mixin.injector.network;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.player.PacketEvent;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NetworkThreadUtils.class)
public class MixinNetworkThreadUtils {

    /**
     * @author DSJ
     * @reason SyncReceive
     */
    @Overwrite
    public static <T extends PacketListener> void forceMainThread(Packet<T> packet, T listener, ThreadExecutor<?> engine) throws OffThreadException {
        if (!engine.isOnThread()) {
            engine.executeSync(() -> {
                if (listener.accepts(packet)) {
                    try {
                        PacketEvent event = new PacketEvent(packet, PacketEvent.PacketType.SyncReceive);

                        if (engine.isOnThread()) {
                            Client.INSTANCE.getEventManager().call(event);
                            if (event.isCancelled()) {
                                return;
                            }
                        }

                        packet.apply(listener);
                    } catch (Exception var4) {
                        if (var4 instanceof CrashException crashException) {
                            if (crashException.getCause() instanceof OutOfMemoryError) {
                                throw NetworkThreadUtils.createCrashException(var4, packet, listener);
                            }
                        }

                        listener.onPacketException(packet, var4);
                    }
                } else {
                    Client.logger.debug("Ignoring packet due to disconnection: {}", packet);
                }

            });
            throw OffThreadException.INSTANCE;
        }
    }
}
