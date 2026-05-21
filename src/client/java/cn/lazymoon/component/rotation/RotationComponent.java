package cn.lazymoon.component.rotation;

import cn.lazymoon.Client;
import cn.lazymoon.component.Component;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.component.rotation.utils.Priority;
import cn.lazymoon.component.rotation.utils.Request;
import cn.lazymoon.component.rotation.utils.SmoothMode;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.api.priority.Priorities;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import cn.lazymoon.event.impl.player.*;
import cn.lazymoon.event.impl.player.LookEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.utils.entity.Rotation;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

public class RotationComponent extends Component {
    //Rotation you are going to turn toward(NOT ACTUAL ROTATION)
    public static Rotation targetRotation;
    //Current(previous tick) rotation
    public static Rotation serverRotation = Rotation.ZERO;

    //Rotation request
    public Request request;
    public MovementFix movementCorrection;

    public RotationComponent(){
        Client.INSTANCE.getEventManager().register(this);
    }

    //Immediate queue
    //TODO: Buffered queue
    public void setRotations(Rotation rotation, MovementFix movementFix, boolean smooth, SmoothMode smoothMode, float rotationSpeed, int keepLength, int revTicks, Priority priority) {
        if (request != null && priority.isLowerThan(request.getPriority())) {
            return;
        }

        request = new Request(rotation, priority, movementFix, smooth, smoothMode, rotationSpeed, keepLength, revTicks);

        if (request.isSmooth()){
            targetRotation = request.getSmoothMode().apply(serverRotation, request.getRotation(), request.getRotationSpeed()).normalize();
        } else {
            targetRotation = request.getRotation().normalize();
        }

        movementCorrection = movementFix;
    }

    public void setRotations(Rotation rotation) {
        setRotations(rotation, MovementFix.NONE);
    }

    public void setRotations(Rotation rotation, MovementFix movementFix) {
        setRotations(rotation, movementFix, false, SmoothMode.LINEAR, 180.0f, 1, 0, Priority.MEDIUM);
    }

    public void setRotations(Rotation rotation, int keepLength, MovementFix movementFix) {
        setRotations(rotation, movementFix, true, SmoothMode.ADVANCED, 180.0f, keepLength, 0, Priority.MEDIUM);
    }
    public void setRotations(Rotation rotation, int keepLength, MovementFix movementFix,float rotationSpeed) {
        setRotations(rotation, movementFix, true, SmoothMode.ADVANCED, rotationSpeed, keepLength, 0, Priority.MEDIUM);
    }
    public void setRotations(Rotation rotation, Priority priority) {
        setRotations(rotation, MovementFix.NONE, false, SmoothMode.LINEAR, 180.0f, 1, 0, priority);
    }

    public void setRotations(Rotation rotation, float rotationSpeed, boolean smooth) {
        setRotations(rotation, MovementFix.NONE, smooth, SmoothMode.LINEAR, rotationSpeed, 1, 0, Priority.MEDIUM);
    }

    //Sample text
    public void reset() {
        targetRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch()).normalize();
        if (Math.abs((serverRotation.getYaw() - mc.player.getYaw()) % 360) < 1 && Math.abs((serverRotation.getPitch() - mc.player.getPitch())) < 1) {
            correctDisabledRotations();
            targetRotation = null;
            request = null;
        }
    }

    private void correctDisabledRotations() {
        final Rotation fixedRotations = resetRotation(new float[]{serverRotation.getYaw(),serverRotation.getPitch()});
        mc.player.setYaw(fixedRotations.getYaw());
        mc.player.setPitch(fixedRotations.getPitch());
    }

    public Rotation resetRotation(final float[] rotation) {
        if (rotation == null) {
            return null;
        }

        final float yaw = rotation[0] + MathHelper.wrapDegrees(mc.player.getYaw() - rotation[0]);
        final float pitch = mc.player.getPitch();
        return new Rotation(yaw, pitch);
    }

    public static double getDirection(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    @EventTarget(Priorities.VERY_HIGH)
    public void onTick(TickEvent event){
        if (event.isPost()) return;
        if (request != null) {
            request.decrement();
            if (request.isExpired()) {
                reset();
            }
        }
    }

    @EventTarget(Priorities.VERY_LOW)
    public void onMotion(MotionEvent event) {
        if (targetRotation != null){
            event.setYaw(targetRotation.getYaw());
            event.setPitch(targetRotation.getPitch());
        }
    }

    @EventTarget(Priorities.VERY_LOW)
    public void onMovementInput(MoveInputEvent event){
        if (targetRotation != null && movementCorrection == MovementFix.SILENT) {
            final float yaw = targetRotation.getYaw();
            final float forward = event.getForward();
            final float strafe = event.getStrafe();

            final double angle = MathHelper.wrapDegrees(Math.toDegrees(getDirection(mc.player.getYaw(), forward, strafe)));

            if (forward == 0 && strafe == 0) return;

            float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

            for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
                for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                    if (predictedStrafe == 0 && predictedForward == 0) continue;

                    final double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)));
                    final double difference = Math.abs(angle - predictedAngle);

                    if (difference < closestDifference) {
                        closestDifference = (float) difference;
                        closestForward = predictedForward;
                        closestStrafe = predictedStrafe;
                    }
                }
            }

            event.setForward(closestForward);
            event.setStrafe(closestStrafe);
        }
    }

    @EventTarget(Priorities.VERY_LOW)
    public void onStrafe(StrafeEvent event) {
        if (targetRotation != null && movementCorrection != MovementFix.NONE) {
            event.setYaw(targetRotation.getYaw());
        }
    }

    @EventTarget(Priorities.VERY_LOW)
    public void onLook(LookEvent event) {
        if (targetRotation != null && movementCorrection != MovementFix.NONE){
            event.setYaw(targetRotation.getYaw());
            event.setPitch(targetRotation.getPitch());
        }
    }

    @EventTarget(Priorities.VERY_LOW)
    public void onLook(JumpEvent event) {
        if (targetRotation != null && movementCorrection != MovementFix.NONE){
            event.setYaw(targetRotation.getYaw());
        }
    }

    @EventTarget(Priorities.VERY_LOW)
    public void onPacket(PacketEvent event){
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerMoveC2SPacket moveC2SPacket){
            if (moveC2SPacket.changesLook()) {
                serverRotation = new Rotation(moveC2SPacket.getYaw(0), moveC2SPacket.getPitch(0));
            }
        }
    }
}