package cn.lazymoon.features.module.impl.movement;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import net.minecraft.client.option.KeyBinding;

@ModuleInfo(name = "Sprint",description = "Helps you sprint automatically",key = 0, category = Category.Movement,hidden = false)
public class Sprint extends Module {
    @Override
    public void onDisable() {
        if (mc.player == null) return;

        KeyBinding.setKeyPressed(mc.options.sprintKey.getDefaultKey(), mc.options.sprintKey.isPressed());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player == null) return;

        KeyBinding.setKeyPressed(mc.options.sprintKey.getDefaultKey(), true);
    }
}

