package cn.lazymoon.features.value;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Getter
@Setter
public abstract class Value<V> {
    protected final Dependency dependency;
    protected V value;
    protected final String name;

    public Value(String name, Dependency dependency) {
        this.name = name;
        this.dependency = dependency;
    }

    public Value(String name, String description) {
        this(name, () -> true);
    }

    public Value(String name) {
        this(name, () -> true);
    }

    public V get() {
        return this.value;
    }

    public void set(V value) {
        this.value = value;
    }

    public boolean isAvailable() {
        return dependency != null && this.dependency.check();
    }
}
