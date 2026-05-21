package cn.lazymoon.sxmurxy.instance;


import cn.lazymoon.sxmurxy.renderers.impl.BuiltTexture;

import java.util.ArrayList;
import java.util.List;

public class TextureTaskInstance {

    /// Performance Optimization
    public static List<BuiltTexture> tasks = new ArrayList<>();

    public static void addTask(BuiltTexture builtTexture) {
        tasks.add(builtTexture);
    }

    public static void runTask() {
        for (BuiltTexture builtTexture : tasks) {
            builtTexture.render(builtTexture.matrix4f(), builtTexture.positionState().x(), builtTexture.positionState().y());
        }
    }

    public static void clearTask() {
        tasks.clear();
    }

}
