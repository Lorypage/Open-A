package cn.lazymoon.features.value.impl;

import cn.lazymoon.features.value.Dependency;
import cn.lazymoon.features.value.Value;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@Getter
@Setter
public class NumberValue extends Value<Double> {
    public float animatedPercentage;
    private final double min;
    private final double max;
    private final double step;

    public NumberValue(String name, Dependency dependency, double defaultValue, double min, double max, double step) {
        super(name, dependency);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public NumberValue(String name, double defaultValue, double min, double max, double step) {
        this(name, () -> true, defaultValue, min, max, step);
    }

    @Override
    public void setValue(Double value) {
        if (value < min) {
            super.setValue(min);
        } else if (value > max) {
            super.setValue(max);
        } else {
            super.setValue(value);
        }
    }
}
