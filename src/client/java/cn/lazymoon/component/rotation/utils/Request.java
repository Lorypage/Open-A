package cn.lazymoon.component.rotation.utils;

import cn.lazymoon.utils.entity.Rotation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Request {
    private Rotation rotation;
    private final IPriority priority;
    private final MovementFix movementFix;
    private final boolean smooth;
    private final float rotationSpeed;
    private final int keepLength;
    private final SmoothMode smoothMode;
    private final int revTicks;
    private int ticksRemaining;

    public Request(Rotation rotation, IPriority priority, MovementFix movementFix, boolean smooth,SmoothMode smoothMode, float rotationSpeed, int keepLength, int revTicks) {
        this.rotation = rotation;
        this.priority = priority;
        this.movementFix = movementFix;
        this.smooth = smooth;
        this.rotationSpeed = rotationSpeed;
        this.keepLength = keepLength;
        this.revTicks = revTicks;
        this.smoothMode = smoothMode;
        this.ticksRemaining = keepLength;
    }

    public void decrement() {
        ticksRemaining--;
    }

    public boolean isExpired() {
        return ticksRemaining < 0;
    }
}