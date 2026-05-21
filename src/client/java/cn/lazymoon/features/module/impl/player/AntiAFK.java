package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;

@ModuleInfo(name = "AntiAFK",description = "Help you automatically stay logged in",key = 0, category = Category.Player,hidden = false)
public class AntiAFK extends Module {
    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        event.jump = true;
    }
}
