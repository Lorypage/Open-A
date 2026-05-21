package cn.lazymoon.event.api;

import cn.lazymoon.event.api.priority.Priorities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method so that the EventManager knows
 * that it should be registered as an event handler
 *
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTarget {
    byte value() default Priorities.MEDIUM;

    boolean ignoreCancelled() default false;
}