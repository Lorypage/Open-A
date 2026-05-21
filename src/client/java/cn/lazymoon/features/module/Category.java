package cn.lazymoon.features.module;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public enum Category {
    Combat("B"),
    Exploit("Q"),
    Movement("C"),
    Player("D"),
    Render("S"),
    Visual("F"),
    World("R");

    public final String icon;

    Category(String icon) {
        this.icon = icon;
    }
}
