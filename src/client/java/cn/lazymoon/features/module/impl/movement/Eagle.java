package cn.lazymoon.features.module.impl.movement;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import net.minecraft.block.AirBlock;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(name = "Eagle",description = "Help you automatically squat",key = 0,category = Category.Movement,hidden = false)
public class Eagle extends Module {
    @Override
    public void onDisable() {
        mc.options.sneakKey.setPressed(false);
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;
        mc.options.sneakKey.setPressed(mc.player.onGround && mc.world.getBlockState(new BlockPos(mc.player.getBlockPos()).add(0, -1, 0)).getBlock() instanceof AirBlock);
    }
}
