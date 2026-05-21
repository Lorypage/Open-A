//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package tech.skidonion.obfuscator.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface NativeObfuscation {
    boolean obfuscated() default true;

    boolean manualTryCatch() default false;

    VirtualMachine virtualize() default NativeObfuscation.VirtualMachine.NONE;

    String verificationLock() default "";

    public static enum VirtualMachine {
        NONE,
        MUTATE_ONLY,
        TIGER_WHITE,
        TIGER_RED,
        TIGER_BLACK,
        FISH_WHITE,
        FISH_RED,
        FISH_BLACK,
        PUMA_WHITE,
        PUMA_RED,
        PUMA_BLACK,
        SHARK_WHITE,
        SHARK_RED,
        SHARK_BLACK,
        DOLPHIN_WHITE,
        DOLPHIN_RED,
        DOLPHIN_BLACK,
        EAGLE_WHITE,
        EAGLE_RED,
        EAGLE_BLACK,
        LION_WHITE,
        LION_RED,
        LION_BLACK;
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface Inline {
    }

    /** @deprecated */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.FIELD})
    @Deprecated
    public @interface InlineStaticFieldAccess {
    }
}
