package cn.lazymoon.component.rotation.utils;

import lombok.Getter;

@Getter
public enum Priority implements IPriority{
    VERY_LOW(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    VERY_HIGH(4);

    private final int level;

    @Override
    public int getLevel() {
        return level;
    }

    public static IPriority of(int custom) {
        return () -> custom;
    }

    Priority(int level) { this.level = level; }

}
