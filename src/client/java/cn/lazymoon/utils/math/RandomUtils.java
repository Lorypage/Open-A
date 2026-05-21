package cn.lazymoon.utils.math;

import cn.lazymoon.utils.InstanceAccess;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils implements InstanceAccess {
    public static float getRandom(float min, float max) {
        if (min == max) {
            return min;
        }

        if (min > max) {
            final float temp = min;
            min = max;
            max = temp;
        }

        return ThreadLocalRandom.current().nextFloat(min, max);
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        }

        if (min > max) {
            final double temp = min;
            min = max;
            max = temp;
        }

        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}
