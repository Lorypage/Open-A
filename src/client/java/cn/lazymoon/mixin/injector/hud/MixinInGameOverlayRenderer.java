package cn.lazymoon.mixin.injector.hud;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.visual.AntiBlind;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameOverlayRenderer.class)
public abstract class MixinInGameOverlayRenderer {

    @Redirect(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer injectFireOpacity(VertexConsumer vertexConsumer, float red, float green, float blue, float alpha) {
        return vertexConsumer.color(red, green, blue, ((Client.INSTANCE.getModuleManager().getModule(AntiBlind.class).isState() && AntiBlind.fireAspect.get()) ? AntiBlind.opacity.get().floatValue() / 100F : 1F) * alpha);
    }

}
