package cn.lazymoon.utils.vector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Rotation {
    public float yaw, pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation(Rotation vector) {
        this(vector.yaw, vector.pitch);
    }

    public Rotation add(float x, float y) {
        return new Rotation(this.yaw + x, this.pitch + y);
    }
    public double distanceToSqr(Rotation other) {
        double dyaw = this.yaw - other.yaw;
        double dpitch = this.pitch - other.pitch;
        return dyaw * dyaw + dpitch * dpitch;
    }
}
