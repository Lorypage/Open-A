package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.input.EventClick;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.world.BedBreaker;
import cn.lazymoon.features.module.impl.world.utils.ItemSpoofUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.utils.misc.ItemUtils;
import cn.lazymoon.utils.player.MoveUtil;
import net.minecraft.block.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(name = "AutoTool",description = "Help switch to the corresponding tool",key = 0, category = Category.Player,hidden = false)
public class AutoTool extends Module {
    public static final BoolValue spoof = new BoolValue("Spoof", true);
    private int originalSlot = -1;
    private boolean hasStartedSpoofing = false;

    @SuppressWarnings("unused")
    @EventTarget
    public void onClick(EventClick event) {
        switchSlot(event.clickedBlock);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || event.isPost()) return;
        if (!mc.options.attackKey.isPressed() && BedBreaker.breakingBlockPos == null) {
            if (hasStartedSpoofing) {
                if (originalSlot != -1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                    originalSlot = -1;
                }
                ItemSpoofUtils.stopSpoof();
                hasStartedSpoofing = false;
            }
        }
    }

    public void switchSlot(BlockPos blockPos) {
        if (mc.world == null || mc.player == null) return;
        float bestSpeed = 1F;
        int bestSlot = -1;

        BlockState blockState = mc.world.getBlockState(blockPos);

        for (int i = 0; i <= 8; i++) {
            ItemStack item = mc.player.getInventory().getStack(i);
            if (ItemUtils.isGodItem(item)) {
                continue;
            }
            if (!item.isEmpty()) {
                float speed = item.getMiningSpeedMultiplier(blockState);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        if (bestSlot != -1 && mc.player.getInventory().selectedSlot != bestSlot) {
            if (spoof.get() && !ItemSpoofUtils.isSpoofing) {
                ItemSpoofUtils.startSpoof();
                hasStartedSpoofing = true;
            }
            if (originalSlot == -1) {
                originalSlot = mc.player.getInventory().selectedSlot;
            }
            mc.player.getInventory().selectedSlot = bestSlot;
        }
    }
}
