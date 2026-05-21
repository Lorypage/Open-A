package cn.lazymoon.utils.client;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.utils.InstanceAccess;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author:Guyuemang
 * @Time:03-07
 */
public class ClientData implements InstanceAccess {
    public static Set<BlockEntity> clickedContainers = new HashSet<>();
    public static Set<BlockEntity> playerClickedContainers = new HashSet<>();
    public static boolean packetOnGround;
    @Getter
    @Setter
    private static int onGroundTicks = 0, offGroundTicks = 0, skipTicks = 0;
    @Getter
    @Setter
    private static float fallDistance = 0;
    public static boolean realSprint;
    public static boolean serverSprint;

    public static boolean clientOnGround() {
        if (mc.player == null) return false;
        return packetOnGround && mc.player.onGround;
    }

    public static double getDistanceToEntityBox(Entity from, Entity to) {
        Vec3d eyes = from.getEyePos();
        Vec3d nearestPoint = getNearestPointBB(eyes, getHitBox(to));
        return eyes.distanceTo(nearestPoint);
    }

    public static Box getHitBox(Entity entity) {
        double borderSize = entity.getTargetingMargin();
        Box box = entity.getBoundingBox();
        return box.expand(borderSize);
    }

    public static Vec3d getNearestPointBB(Vec3d eye, Box box) {
        double x = eye.x;
        double y = eye.y;
        double z = eye.z;

        if (x > box.maxX) x = box.maxX;
        else if (x < box.minX) x = box.minX;

        if (y > box.maxY) y = box.maxY;
        else if (y < box.minY) y = box.minY;

        if (z > box.maxZ) z = box.maxZ;
        else if (z < box.minZ) z = box.minZ;

        return new Vec3d(x, y, z);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getState() == PacketEvent.PacketType.Send) {
            if (event.packet instanceof ClientCommandC2SPacket packet) {
                if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
                    realSprint = true;
                    serverSprint = true;
                }
                if (packet.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
                    realSprint = false;
                    serverSprint = false;
                }
            }

            if (event.getPacket() instanceof PlayerMoveC2SPacket packet) {
                packetOnGround = packet.isOnGround();
            }
        }
    }
}
