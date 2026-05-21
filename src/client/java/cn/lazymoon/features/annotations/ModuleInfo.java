package cn.lazymoon.features.annotations;

import cn.lazymoon.features.module.Category;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {
    String name();
    String description();

    int key();

    Category category();

    boolean hidden();
}