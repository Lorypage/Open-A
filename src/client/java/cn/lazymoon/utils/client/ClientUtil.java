package cn.lazymoon.utils.client;

import cn.lazymoon.utils.InstanceAccess;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClientUtil implements InstanceAccess {
    public int offGroundTicks, onGroundTicks;
    public int skipTicks = 0;
    public float timer = 1;

    private double lastY;
    public float fallDistance;

    public void updateFallDistance() {
        if (mc.player == null) {
            reset();
            return;
        }

        double y = mc.player.getY();

        if (lastY != 0 && !mc.player.isOnGround() && y < lastY) {
            fallDistance += (float) (lastY - y);
        } else if (mc.player.isOnGround()) {
            fallDistance = 0;
        }

        lastY = y;
    }

    public void reset() {
        fallDistance = 0;
        lastY = 0;
    }
}
