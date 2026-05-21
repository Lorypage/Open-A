package cn.lazymoon.features.module.impl.world.utils;

import cn.lazymoon.utils.InstanceAccess;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class ItemSpoofUtils implements InstanceAccess {
    public static boolean isSpoofing = false;
    public static int originalSlot = -1;
    private int counter = 0;

    public void startSpoof() {
        if (mc.player != null && mc.world != null) {
            ++counter;

            if (!isSpoofing) {
                originalSlot = mc.player.getInventory().selectedSlot;
                isSpoofing = true;
            }
        }
    }

    public void stopSpoof() {
        if (mc.player != null && mc.world != null && isSpoofing) {
            --counter;

            if (counter <= 0) {
                mc.player.getInventory().selectedSlot = originalSlot;
                isSpoofing = false;
            }
        }
    }

    public int getSpoofedSlot() {
        if (mc.player != null && mc.world != null) {
            return isSpoofing ? originalSlot : mc.player.getInventory().selectedSlot;
        } else {
            return -1;
        }
    }

    public ItemStack getSpoofedStack() {
        if (mc.player != null && mc.world != null) {
            return isSpoofing ? mc.player.getInventory().getStack(originalSlot) : mc.player.getMainHandStack();
        } else {
            return null;
        }
    }

    public void reset() {
        isSpoofing = false;
        originalSlot = -1;
        counter = 0;
    }
}
