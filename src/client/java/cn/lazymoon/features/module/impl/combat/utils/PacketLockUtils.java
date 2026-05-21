package cn.lazymoon.features.module.impl.combat.utils;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.world.TickEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class PacketLockUtils {
    private static final AtomicBoolean attackLock = new AtomicBoolean(false);
    private static final AtomicBoolean swingLock = new AtomicBoolean(false);

    public static boolean attackAndLock() {
        return attackLock.compareAndSet(false, true);
    }

    public static boolean swingAndLock() {
        return swingLock.compareAndSet(false, true);
    }

    @EventTarget()
    public void onTick(TickEvent event){
        if (event.isPost()) return;
        reset();
    }

    public static void reset() {
        attackLock.set(false);
        swingLock.set(false);
    }

}
