package cn.lazymoon.utils.time;

import cn.lazymoon.utils.InstanceAccess;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class TimerUtils implements InstanceAccess {
    //long field
    public long lastMS = System.currentTimeMillis();

    //reset time
    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    //delay + reset
    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - lastMS > time) {
            if (reset) reset();
            return true;
        }

        return false;
    }

    //delay long
    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    //delay double
    public boolean hasTimeElapsed(double time) {
        return hasTimeElapsed((long) time);
    }

    //delay float
    public boolean hasTimeElapsed(float time) {
        return hasTimeElapsed((long) time);
    }

    //get Time
    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    //set Time
    public void setTime(long time) {
        lastMS = time;
    }
}
