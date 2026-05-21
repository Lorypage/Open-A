package cn.lazymoon.mixin.injector.splash;

import cn.lazymoon.ingameui.splash.ArcaneMainMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void arcane$replaceMenu(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // 直接替换成你的主菜单
        client.setScreen(new ArcaneMainMenuScreen());

        // 阻止原版主菜单继续初始化
        ci.cancel();
    }
}