package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "AntiBlind",description = "Cancel your blindness effect",key = 0,category = Category.Visual,hidden = false)
public class AntiBlind extends Module {
    public static BoolValue fireAspect = new BoolValue("FireAspect", true);
    public static NumberValue opacity = new NumberValue("Fire Opacity \"%\"",20,0,100,1);
    public static BoolValue darkness = new BoolValue("Darkness",true);
    public static BoolValue pumpkin = new BoolValue("Pumpkin",true);
    public static BoolValue blind = new BoolValue("Blinding",true);
    public static BoolValue nausea = new BoolValue("Nausea",true);
}
