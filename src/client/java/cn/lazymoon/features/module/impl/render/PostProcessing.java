package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.features.annotations.AutoEnable;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@AutoEnable
@ModuleInfo(name = "PostProcessing",description = "Post-processing for games and GUI",key = 0, category = Category.Render, hidden = false)
public class PostProcessing extends Module {
    public static BoolValue bloom = new BoolValue("Bloom", false);
}
