package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.Client;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import org.lwjgl.glfw.GLFW;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "ClickGui",description = "Open a click GUI that can adjust module settings",key = GLFW.GLFW_KEY_RIGHT_SHIFT, category = Category.Render, hidden = false)
public class ClickGui extends Module {
    @Override
    public void onEnable() {
        mc.setScreen(Client.INSTANCE.getPanelClickGui());
        this.toggle();
        super.onEnable();
    }
}
