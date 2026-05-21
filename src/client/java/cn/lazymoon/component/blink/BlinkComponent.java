package cn.lazymoon.component.blink;

import cn.lazymoon.component.Component;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.world.WorldEvent;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.pack.PacketUtils;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.network.encryption.PacketEncryptionManager;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.HandshakePackets;
import net.minecraft.network.packet.LoginPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PingPackets;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Author:Guyuemang
 * @Time:03-01
 */
public class BlinkComponent extends Component {
    private static final LinkedBlockingDeque<Packet<ClientPlayPacketListener>> serverPackets = new LinkedBlockingDeque<>();
    private static final LinkedBlockingDeque<Packet<?>> clientPackets = new LinkedBlockingDeque<>();
    public static boolean blinking = false;
    public static boolean delaying = false;

    public static void startBlink() {
        if (blinking) return;
        clientPackets.clear();
        blinking = true;
    }

    public static void stopBlink() {
        if (!blinking) return;
        sendBlinkPackets();
        blinking = false;
    }

    public static void startDelay() {
        if (delaying) return;
        serverPackets.clear();
        delaying = true;
    }

    public static void stopDelay() {
        if (!delaying) return;
        handleDelayPackets();
        delaying = false;
    }

    public static void sendBlinkPackets() {
        synchronized (clientPackets) {
            while (!clientPackets.isEmpty()) {
                Packet<?> packet = clientPackets.poll();
                PacketUtils.sendPacketNoEvent(packet);
            }
        }
    }

    public static void handleMove(PlayerMoveC2SPacket packet, OtherClientPlayerEntity clonePlayer) {
        double x = packet.getX(clonePlayer.getX());
        double y = packet.getY(clonePlayer.getY());
        double z = packet.getZ(clonePlayer.getZ());

        float yaw = packet.getYaw(clonePlayer.getYaw());
        float pitch = packet.getPitch(clonePlayer.getPitch());

        clonePlayer.updatePositionAndAngles(x, y, z, yaw, pitch);

        if (packet.changesLook()) {
            clonePlayer.setYaw(yaw);
            clonePlayer.setHeadYaw(yaw);
            clonePlayer.setPitch(pitch);
        }
    }

    public static void releaseTick(OtherClientPlayerEntity clonePlayer) {
        synchronized (clientPackets) {
            while (!clientPackets.isEmpty()) {
                Packet<?> poll = clientPackets.poll();
                PacketUtils.sendPacketNoEvent(poll);
                if (poll instanceof PlayerMoveC2SPacket) {
                    handleMove((PlayerMoveC2SPacket) poll, clonePlayer);
                    break;
                }
            }
        }
    }

    public static void handleDelayPackets() {
        synchronized (serverPackets) {
            while (!serverPackets.isEmpty()) {
                Packet<ClientPlayPacketListener> packet = serverPackets.poll();
                packet.apply(mc.getNetworkHandler());
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.isCancelled()) return;
        if (ClientUtils.isNull() || mc.getNetworkHandler() == null || mc.player.isDead()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof HandshakeC2SPacket
                || packet instanceof QueryRequestC2SPacket
                || packet instanceof QueryPingC2SPacket
                || packet instanceof GameMessageS2CPacket
                || packet instanceof DisconnectS2CPacket
                || packet instanceof EntityS2CPacket) {
            return;
        }

        if (event.getState() == PacketEvent.PacketType.Send && blinking) {
            event.setCancelled(true);
            synchronized (clientPackets) {
                clientPackets.add(packet);
            }
        }

        if (event.getState() == PacketEvent.PacketType.SyncReceive && delaying) {
            event.setCancelled(true);
            synchronized (serverPackets) {
                serverPackets.add((Packet<ClientPlayPacketListener>) packet);
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldEvent event) {
        stopBlink();
    }
}
