package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.world.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.pack.PacketUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(name = "Blink",description = "Let you get fake lag",key = 0,category = Category.Player,hidden = false)
public class Blink extends Module {
    public static NumberValue releaseInterval = (NumberValue) new NumberValue("Release interval",350,0,1000,10);

    public static TimerUtils releaseTimer = new TimerUtils();
    public static TimerUtils delayTimer = new TimerUtils();
    public static OtherClientPlayerEntity clonePlayer = null;
    public static final LinkedBlockingDeque<Packet<?>> blinkPackets = new LinkedBlockingDeque<>();

    @Override
    public void onEnable() {
        releaseTimer.reset();
        delayTimer.reset();
        if (mc.isInSingleplayer()) {
            this.setState(false);
            ClientUtils.displayChat("You can't use blink in singleplayer!");
            return;
        }
        if (mc.world == null || mc.player == null) return;
        OtherClientPlayerEntity clone = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        clone.headYaw = mc.player.headYaw;
        clone.copyPositionAndRotation(mc.player);
        clone.setUuid(UUID.randomUUID());
        mc.world.addEntity(clone);
        clonePlayer = clone;
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
                || packet instanceof DisconnectS2CPacket) {
            return;
        }

        if (event.isSend()) {
            event.setCancelled(true);
            synchronized (blinkPackets) {
                blinkPackets.add(packet);
            }

        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        if (mc.world == null) {
            blinkPackets.clear();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (event.isPost()) {
            if (mc.player == null) return;
            if (mc.player.isDead() || mc.player.age <= 10) {
                sendBlinkPacket();
            }
            if (releaseTimer.hasTimeElapsed(releaseInterval.get())) {
                releaseTickPacket();
                releaseTimer.reset();
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        sendBlinkPacket();
    }

    private void sendBlinkPacket() {
        synchronized (blinkPackets) {
            while (!blinkPackets.isEmpty()) {
                Packet<?> packet = blinkPackets.poll();
                PacketUtils.sendPacketNoEvent(packet);
            }
        }

        if (clonePlayer != null) {
            deleteFakePlayer();
            clonePlayer = null;
        }
    }

    private void releaseTickPacket() {
        synchronized (blinkPackets) {
            while (!blinkPackets.isEmpty()) {
                Packet<?> packet = blinkPackets.poll();
                PacketUtils.sendPacketNoEvent(packet);
                if (packet instanceof PlayerMoveC2SPacket packet1) {
                    double x = packet1.getX(clonePlayer.getX());
                    double y = packet1.getY(clonePlayer.getY());
                    double z = packet1.getZ(clonePlayer.getZ());

                    float yaw = packet1.getYaw(clonePlayer.getYaw());
                    float pitch = packet1.getPitch(clonePlayer.getPitch());

                    clonePlayer.updatePositionAndAngles(x, y, z, yaw, pitch);

                    if (packet1.changesLook()) {
                        clonePlayer.setYaw(yaw);
                        clonePlayer.setHeadYaw(yaw);
                        clonePlayer.setPitch(pitch);
                    }
                    break;
                }
            }
        }
    }

    private void deleteFakePlayer() {
        if (clonePlayer == null || mc.world == null) return;
        OtherClientPlayerEntity clone = clonePlayer;

        mc.world.removeEntity(clone.getId(), Entity.RemovalReason.DISCARDED);
        clonePlayer = null;
    }
}