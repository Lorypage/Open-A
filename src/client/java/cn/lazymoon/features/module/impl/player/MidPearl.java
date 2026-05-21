package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.TickMovementEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import static cn.lazymoon.utils.math.MathUtil.getRandom;

@ModuleInfo(name = "MiddlePearl",description = "Allows you to throw pearls with your middle mouse button",key = 0, category = Category.Player,hidden = false)
public class MidPearl extends Module {
    private boolean doWork = false;
    private boolean switchBack = false;
    private int lastSlot = 0;
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onEnable() {
        this.lastSlot = -1;
        this.doWork = false;
        this.switchBack = false;
    }

    @EventTarget
    public void onTickMovement(TickMovementEvent event) {
        if (ClientUtils.isNull() || mc.interactionManager == null || mc.player.isDead()) {
            return;
        }

        if (mc.options.pickItemKey.isPressed()) {
            this.timer.reset();
            this.doWork = true;
            this.switchBack = false;
        }

        if (this.doWork && this.timer.hasTimeElapsed(getRandom(1, 5))) {
            int pearlSlot = findPearlSlot();

            if (pearlSlot != -1) {
                this.lastSlot = mc.player.getInventory().selectedSlot;

                mc.player.getInventory().selectedSlot = pearlSlot;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

                this.timer.reset();
                this.doWork = false;
                this.switchBack = true;
            }
        }

        if (this.switchBack && this.timer.hasTimeElapsed(getRandom(1, 5))) {
            mc.player.getInventory().selectedSlot = this.lastSlot;
            this.switchBack = false;
        }
    }

    private int findPearlSlot() {
        if (mc.player != null) {
            for (int i = 0; i <= 8; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);

                if (!stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) {
                    return i;
                }
            }
        }

        return -1;
    }
}
