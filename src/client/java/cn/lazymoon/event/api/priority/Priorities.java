package cn.lazymoon.event.api.priority;

/**
 * The priority for the dispatcher to determine what method should be invoked first.
 * Using bytes for memory optimization as per request.
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public final class Priorities {
    private Priorities() {}

    public static final byte VERY_LOW = 0;
    public static final byte LOW = 1;
    public static final byte MEDIUM = 2;
    public static final byte HIGH = 3;
    public static final byte VERY_HIGH = 4;
    /**
     * Array containing all the priority values.
     */
    public static final byte[] VALUE_ARRAY = {
            VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
    };
}