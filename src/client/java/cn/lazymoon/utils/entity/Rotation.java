package cn.lazymoon.utils.entity;

import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.utils.InstanceAccess;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;

/**
 * Rotations
 */
@Getter
@Setter
public class Rotation implements InstanceAccess {
    public static final Rotation ZERO = new Rotation(0f, 0f);

    float yaw;
    float pitch;
    boolean isNormalized;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.isNormalized = false;
    }

    public Rotation(float yaw, float pitch,boolean isNormalized) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.isNormalized = isNormalized;
    }

    public Rotation normalize() {
        if (isNormalized) return this;

        double f = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double gcd = f * f * f * 8.0 * 0.15;

        Rotation currentRotation = RotationComponent.targetRotation != null
                ? RotationComponent.targetRotation
                : new Rotation(mc.player.getYaw(),mc.player.getPitch());

        RotationDelta diff = currentRotation.rotationDeltaTo(this);

        float g1 = (float) (Math.round(diff.deltaYaw() / gcd) * gcd);
        float g2 = (float) (Math.round(diff.deltaPitch() / gcd) * gcd);

        float newYaw = currentRotation.yaw + g1;
        float newPitch = Math.max(-90f, Math.min(90f, currentRotation.pitch + g2));

        return new Rotation(newYaw, newPitch, true);
    }

    public RotationDelta rotationDeltaTo(Rotation other) {
        return new RotationDelta(
                MathHelper.wrapDegrees(other.yaw - this.yaw),
                MathHelper.wrapDegrees(other.pitch - this.pitch)
        );
    }

    @Override
    public boolean equals(Object object) {
        if (object == null && mc.player != null) {
            object = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        if (object instanceof Rotation rotation) {
            if (this.yaw != rotation.yaw) {
                return false;
            }

            return this.pitch == rotation.pitch;
        }

        return false;
    }

    public record RotationDelta(float deltaYaw, float deltaPitch) {
        public float length() {
            return (float) Math.hypot(deltaYaw, deltaPitch);
        }
    }
}