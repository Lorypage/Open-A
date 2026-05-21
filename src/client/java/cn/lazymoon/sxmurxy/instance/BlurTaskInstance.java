package cn.lazymoon.sxmurxy.instance;

import cn.lazymoon.Client;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BlurTaskInstance {

    /// Performance Optimization
    public static List<BuiltBlur> tasks = new ArrayList<>();

    public static void addTask(BuiltBlur builtBlur) {
        tasks.add(builtBlur);
    }

    public static void runTask() {
        tasks = tasks.stream().sorted(Comparator.comparing(BuiltBlur::blurRadius)).collect(Collectors.toCollection(ArrayList::new));

        for (BuiltBlur builtBlur : tasks) {
            builtBlur.render(builtBlur.matrix4f(), builtBlur.positionState().x(), builtBlur.positionState().y());
        }
    }

    public static void clearTask() {
        tasks.clear();
    }
}
