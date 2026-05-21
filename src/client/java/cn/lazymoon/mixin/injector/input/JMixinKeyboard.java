package cn.lazymoon.mixin.injector.input;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.input.KeyEvent;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Mixin(Keyboard.class)
public class JMixinKeyboard {
    /** Inject KeyBoard OnKey Event */
    @Inject(at=@At(value="HEAD"), method="onKey")
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
        if (action == 1) {
            KeyEvent event = new KeyEvent(key);
            Client.INSTANCE.getEventManager().call(event);
        }
    }
}
