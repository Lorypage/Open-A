package cn.lazymoon.component.rotation.utils;

import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.math.MathUtils;
import net.minecraft.util.math.MathHelper;

public enum SmoothMode {
    LINEAR {
        @Override
        public Rotation apply(Rotation current, Rotation target, float rotationSpeed) {
            float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
            float deltaPitch = target.getPitch() - current.getPitch();

            float factor = Math.min(1.0f, rotationSpeed / 180f);

            float newYaw = current.getYaw() + deltaYaw * factor;
            float newPitch = MathHelper.clamp(current.getPitch() + deltaPitch * factor, -90f, 90f);

            return new Rotation(newYaw, newPitch);
        }
    },

    ADVANCED {
        @Override
        public Rotation apply(Rotation current, Rotation target, float rotationSpeed) {
            int yawSpeed = (int) rotationSpeed;
            int pitchSpeed = (int) (rotationSpeed / 2f);

            if (yawSpeed <= 0 || pitchSpeed <= 0) {
                return target;
            }

            float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
            float deltaPitch = target.getPitch() - current.getPitch();

            if (deltaYaw != 0) {
                float absYaw = Math.abs(deltaYaw);
                float yawRatio = Math.min(absYaw, yawSpeed) / absYaw;
                deltaYaw *= yawRatio;
            }

            if (deltaPitch != 0) {
                float absPitch = Math.abs(deltaPitch);
                float pitchRatio = Math.min(absPitch, pitchSpeed) / absPitch;
                deltaPitch *= pitchRatio;
            }

            return new Rotation(
                    current.getYaw() + deltaYaw,
                    MathUtils.clamp(current.getPitch() + deltaPitch, -90, 90)
            );
        }
    };

    public abstract Rotation apply(Rotation current, Rotation target, float rotationSpeed);
}
