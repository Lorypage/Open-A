package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.features.value.impl.NumberValue;

import java.util.Arrays;

@ModuleInfo(name = "Camera", description = "Let you have the perspective of sports",key = 0,category = Category.Visual,hidden = false)
public class Camera extends Module {
    public static final BoolValue modifyFov = new BoolValue("Modify Fov", true);
    public static final NumberValue fov = (NumberValue) new NumberValue("Fov",modifyFov::get, 1.00,0.00,1.50,0.01);
    public static final NumberValue cameraDistance = new NumberValue("Camera Distance", 4, 0, 10, 0.1);
    public static final MultiBoolValue cameraOptions = new MultiBoolValue("Camera Options", Arrays.asList(
            new BoolValue("No Fog", true),
            new BoolValue("No Camera Clip", false),
            new BoolValue("No Hurt Tilt", false),
            new BoolValue("Smooth", true),
            new BoolValue("Motion Camera", false)
    ));

    public static final NumberValue cameraSpeed = (NumberValue) new NumberValue("Camera Speed",() -> cameraOptions.isEnabled("Motion Camera"), 0.1, 0.01, 0.5, 0.01);
}
