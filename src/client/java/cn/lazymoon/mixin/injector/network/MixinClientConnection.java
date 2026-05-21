package cn.lazymoon.mixin.injector.network;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.utils.pack.PacketUtils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo info) {
        if (PacketUtils.getPackets().remove(packet)) {
            return;
        }

        PacketEvent event = new PacketEvent(packet, PacketEvent.PacketType.Send);
        Client.INSTANCE.getEventManager().call(event);

        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        if (packet instanceof BundleS2CPacket bundlePacket) {
            for (Packet<?> p : bundlePacket.getPackets()) {
                PacketEvent event = new PacketEvent(p, PacketEvent.PacketType.Receive);
                Client.INSTANCE.getEventManager().call(event);
                if (event.isCancelled()) {
                    info.cancel();
                    return;
                }
            }
        } else {
            PacketEvent event = new PacketEvent(packet, PacketEvent.PacketType.Receive);
            Client.INSTANCE.getEventManager().call(event);
            if (event.isCancelled()) {
                info.cancel();
            }
        }
    }
}
