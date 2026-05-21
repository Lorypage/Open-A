package cn.lazymoon.mixin.injector.utils;

import cn.lazymoon.Client;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Mixin(Window.class)
public class JMixinWindow {

    @SuppressWarnings("resource")
    @Redirect(method = "setIcon", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Icons;getIcons(Lnet/minecraft/resource/ResourcePack;)Ljava/util/List;"))
    private List<InputSupplier<InputStream>> onSetIcon(Icons instance, ResourcePack resourcePack) throws IOException {
        final InputStream stream16 = JMixinWindow.class.getResourceAsStream("/assets/lazymoon/arcane/icons/icon.png");
        final InputStream stream32 = JMixinWindow.class.getResourceAsStream("/assets/lazymoon/arcane/icons/icon.png");
        if (stream16 == null || stream32 == null) {
            return instance.getIcons(resourcePack);
        }
        return List.of(() -> stream16, () -> stream32);
    }

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;",
                    shift = At.Shift.AFTER,
                    remap = false
            )
    )
    private void onAfterCreateCapabilities(CallbackInfo ci) {
        Client.INSTANCE.InitializingNanoVG();
        FontManager.registerFonts();
    }
}
