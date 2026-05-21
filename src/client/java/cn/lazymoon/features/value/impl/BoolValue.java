package cn.lazymoon.features.value.impl;

import cn.lazymoon.features.value.Dependency;
import cn.lazymoon.features.value.Value;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class BoolValue extends Value<Boolean> {
    public BoolValue(String name, Dependency dependency, boolean defaultValue) {
        super(name, dependency);
        this.value = defaultValue;
    }

    public BoolValue(String name, boolean defaultValue) {
        this(name, () -> true, defaultValue);
    }

    public void toggle() {
        value = !value;
    }
}
