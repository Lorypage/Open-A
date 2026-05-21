package cn.lazymoon.component.rotation.utils;

public interface IPriority {
    int getLevel();

    default boolean isHigherThan(IPriority other) {
        return this.getLevel() > other.getLevel();
    }

    default boolean isLowerThan(IPriority other) {
        return this.getLevel() < other.getLevel();
    }
}
