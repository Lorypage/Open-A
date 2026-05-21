package cn.lazymoon.utils.pack;

import cn.lazymoon.mixin.injector.network.packet.c2s.play.PlayerMoveC2SPacketAccessor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;

@UtilityClass
public class PacketUtils implements InstanceAccess {
    public static void setPlayerMovePacketYaw(PlayerMoveC2SPacket packet, float yaw) {
        ((PlayerMoveC2SPacketAccessor) packet).setYaw(yaw);
    }

    public static void setPlayerMovePacketPitch(PlayerMoveC2SPacket packet, float pitch) {
        ((PlayerMoveC2SPacketAccessor) packet).setPitch(pitch);
    }

    @Getter
    private final ArrayList<Packet<?>> packets = new ArrayList<>();

    public void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        packets.add(packet);
        sendPacket(packet);
    }

    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;

        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence()) {
            mc.getNetworkHandler().sendPacket(packetCreator.predict(pendingUpdateManager.getSequence()));
        }
    }

    public void sendSequencedPacketNoEvent(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;

        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence()) {
            Packet<?> packet = packetCreator.predict(pendingUpdateManager.getSequence());
            packets.add(packet);
            mc.getNetworkHandler().sendPacket(packet);
        }
    }
}
