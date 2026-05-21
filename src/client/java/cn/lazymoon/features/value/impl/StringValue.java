
package cn.lazymoon.features.value.impl;

import cn.lazymoon.features.value.Dependency;
import cn.lazymoon.features.value.Value;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StringValue extends Value {
    private String text;
    private boolean onlyNumber;

    public StringValue(String name, String text, Dependency dependency) {
        super(name, dependency);
        this.text = text;
        this.onlyNumber = false;
    }

    public StringValue(String name, String text) {
        super(name, () -> true);
        this.text = text;
    }

    public StringValue(String name, String text, boolean onlyNumber, Dependency dependency) {
        super(name, dependency);
        this.text = text;
        this.onlyNumber = onlyNumber;
    }

    public StringValue(String name, String text, boolean onlyNumber) {
        super(name, () -> true);
        this.text = text;
        this.onlyNumber = onlyNumber;
    }

    public String get() {
        return text;
    }
}
