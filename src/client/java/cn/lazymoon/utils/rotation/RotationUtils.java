package cn.lazymoon.utils.rotation;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.component.rotation.utils.Priority;
import cn.lazymoon.component.rotation.utils.SmoothMode;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.math.MathUtil;
import cn.lazymoon.utils.math.MathUtils;
import cn.lazymoon.utils.vector.Vector2f;
import cn.lazymoon.utils.vector.Vector3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

public class RotationUtils implements InstanceAccess {
    private static final double RAD_TO_DEG = 180.0 / Math.PI;

    public static Rotation calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());
        final float yaw = (float) (MathHelper.atan2(diff.getZ(), diff.getX()) * MathUtil.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.getY(), distance) * MathUtil.TO_DEGREES));
        return new Rotation(yaw, pitch);
    }

    public static float wrapAngleDiff(float angle, float target) {
        return target + wrapAngleTo180_float(angle - target);
    }

    public static float wrapAngleTo180_float(float value) {
        value = value % 360.0F;

        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }


    public static Vec3d getCenter(Box bb) {
        return new Vec3d(bb.minX + (bb.maxX - bb.minX) * 0.5, bb.minY + (bb.maxY - bb.minY) * 0.5, bb.minZ + (bb.maxZ - bb.minZ) * 0.5);
    }

    public static float quantizeAngle(float angle) {
        return (float) ((double) angle - (double) angle % (double) 0.0096f);
    }

    public static double getRotationDifference(Rotation a, Rotation b) {
        return Math.hypot(getAngleDifference(a.getYaw(), b.getYaw()), a.getPitch() - b.getPitch());
    }

    public static float getAngleDifference(float a, float b) {
        return ((((a - b) % 360F) + 540F) % 360F) - 180F;
    }

    public static Vec3d getEntityNearestVec(PlayerEntity player, LivingEntity entity) {
        Vec3d eyePos = player.getEyePos();
        Box targetBox = entity.getBoundingBox();

        double x = MathHelper.clamp(eyePos.x, targetBox.minX, targetBox.maxX);
        double y = MathHelper.clamp(eyePos.y, targetBox.minY, targetBox.maxY);
        double z = MathHelper.clamp(eyePos.z, targetBox.minZ, targetBox.maxZ);

        return new Vec3d(x, y, z);
    }

    public static Rotation vecToRotation(Vec3d target, boolean predict) {
        if (mc.player == null) {
            return new Rotation(0f, 0f);
        }

        Vec3d eyesPos = mc.player.getEyePos();

        if (predict) {
            eyesPos = eyesPos.add(mc.player.getVelocity());
        }

        double diffX = target.x - eyesPos.x;
        double diffY = target.y - eyesPos.y;
        double diffZ = target.z - eyesPos.z;

        final double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, distXZ) * RAD_TO_DEG));

        return new Rotation(yaw, pitch);
    }

    public static void updateCurrentCrosshairTarget() {
        mc.gameRenderer.updateCrosshairTarget(1f);
    }

    public static float clampYaw(float yaw, float maxYaw) {
        maxYaw = MathUtils.clamp(maxYaw, 0, 180);

        if (yaw > maxYaw) {
            yaw = maxYaw;
        } else if (yaw < -maxYaw) {
            yaw = -maxYaw;
        }

        return yaw;
    }

    public static float quantize(float angle) {
        return angle - angle % 0.0096f;
    }

    public static Rotation toRotation(Vec3d eyePos, Vec3d targetPos) {
        double diffX = targetPos.x - eyePos.x;
        double diffY = targetPos.y - eyePos.y;
        double diffZ = targetPos.z - eyePos.z;

        float yaw = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90));
        float pitch = MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))));

        return new Rotation(yaw, MathHelper.clamp(pitch, -90, 90));
    }

    public static Rotation toRotation(Vec3d vec, boolean predict) {
        if (mc.player == null) {
            return new Rotation(0f, 0f);
        }

        Vec3d eyesPos = mc.player.getEyePos();

        if (predict) {
            eyesPos = eyesPos.add(mc.player.getVelocity());
        }

        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;

        float yaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f);
        float pitch = MathHelper.wrapDegrees(-(float) Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))));

        return new Rotation(yaw, pitch);
    }

    public static Vector2f getRotationSmooth(
            Vec3d targetPos, Vec3d eyePos,
            float yaw1 , float pitch1) {

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        double horiz = Math.hypot(dx, dz);

        float rawYaw = (horiz < 1e-6) ? yaw1 : (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float rawPitch = (horiz < 1e-6) ? (dy > 0 ? - 90f : 90f) : (float) - Math.toDegrees(Math.atan2(dy, horiz));

        float yaw = yaw1 + MathHelper.wrapDegrees(rawYaw - yaw1);
        float pitch = pitch1 + MathHelper.wrapDegrees(rawPitch - pitch1);
        pitch = MathHelper.clamp(pitch, - 90.0f, 90.0f);

        return new Vector2f(yaw, pitch);
    }

    public static Rotation getRotationOrElseMC() {

        if (mc.player != null) {
            return RotationComponent.targetRotation == null ? new Rotation(mc.player.getYaw(), mc.player.getPitch()) : RotationComponent.targetRotation;
        } else {
            return new Rotation(0, 0);
        }

    }

    public static HitResult rayCast(float tickDelta, float yawDeg, float pitchDeg) {
        Entity camera = mc.getCameraEntity();
        if (camera == null || mc.world == null || mc.player == null) return null;

        double distance = mc.player.getBlockInteractionRange();

        Vec3d start = camera.getCameraPosVec(tickDelta);
        Vec3d dir = calculateViewVector(pitchDeg, yawDeg);
        Vec3d end = start.add(dir.multiply(distance));

        return mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                camera
        ));
    }

    public static Vec3d calculateViewVector(float pXRot, float pYRot) {
        float f = pXRot * ((float)Math.PI / 180F);
        float f1 = -pYRot * ((float)Math.PI / 180F);

        float f2 = MathHelper.cos(f1);
        float f3 = MathHelper.sin(f1);
        float f4 = MathHelper.cos(f);
        float f5 = MathHelper.sin(f);
        return new Vec3d(f3 * f4, -f5, f2 * f4);
    }

    public static Rotation calculate(final Entity entity) {
        return calculate(new Vector3d(entity.getX(), entity.getY(), entity.getZ()).add(0, Math.max(0, Math.min(mc.player.getY() - entity.getY() +
                mc.player.getEyeHeight(mc.player.getPose()), (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * 0.9)), 0));
    }

    public static float rotateToYaw(float yawSpeed, float currentYaw, float calcYaw) {
        return updateRotation(currentYaw, calcYaw, yawSpeed);
    }

    public static float updateRotation(final float current, final float calc, final float maxDelta) {
        float f = MathHelper.wrapDegrees(calc - current);
        if (f > maxDelta) {
            f = maxDelta;
        }
        if (f < -maxDelta) {
            f = -maxDelta;
        }
        return current + f;
    }

    public static Rotation calculate(final Vec3d to, final Direction direction) {
        return calculate(new Vector3d(to.x, to.y, to.z), direction);
    }

    public static Rotation calculate(final Vec3d to) {
        return calculate(new Vector3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), new Vector3d(to.x, to.y, to.z));
    }

    public static Rotation calculate(final BlockPos to) {
        return calculate(new Vector3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), new Vector3d(to.getX(), to.getY(), to.getZ()).add(0.5, 0.5, 0.5));
    }

    public static Rotation calculate(final Vector3d to) {
        return calculate(new Vector3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ()), to);
    }

    public static Rotation calculate(final Vector3d position, final Direction direction) {
        double x = position.getX() + 0.5;
        double y = position.getY() + 0.5;
        double z = position.getZ() + 0.5;

        x += (double) direction.getOffsetX() * 0.5;
        y += (double) direction.getOffsetY() * 0.5;
        z += (double) direction.getOffsetZ() * 0.5;
        return calculate(new Vector3d(x, y, z));
    }

    private static float shortestYaw(float from, float to) {
        return from + MathHelper.wrapDegrees(to - from);
    }

    @Deprecated(forRemoval = true)
    public static void setRotation(Rotation b, int k, boolean s, int y, int p) {
        MovementFix correction = s ? MovementFix.SILENT : MovementFix.STRICT;
        Client.INSTANCE.getRotationComponent().setRotations(b,correction,true, SmoothMode.ADVANCED,y,k,0, Priority.MEDIUM);
    }

    @Deprecated(forRemoval = true)
    public static void setRotation(Rotation r, int k, boolean s) {
        MovementFix correction = s ? MovementFix.SILENT : MovementFix.STRICT;
        Client.INSTANCE.getRotationComponent().setRotations(r,correction,true, SmoothMode.ADVANCED,180,k,0, Priority.MEDIUM);
    }

    @Deprecated(forRemoval = true)
    public static void setRotationNoSmooth(Rotation r, int k, boolean s) {
        MovementFix correction = s ? MovementFix.SILENT : MovementFix.STRICT;
        Client.INSTANCE.getRotationComponent().setRotations(r,correction,false, SmoothMode.LINEAR,180,k,0, Priority.MEDIUM);
    }
}
