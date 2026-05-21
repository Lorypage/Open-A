package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.render.Render3DEvent;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.features.module.impl.render.Notification;
import cn.lazymoon.features.module.impl.visual.AntiBlind;
import cn.lazymoon.features.module.impl.visual.Camera;
import cn.lazymoon.ingameui.notification.NotificationManager;
import cn.lazymoon.nanovg.gl.States;
import cn.lazymoon.sxmurxy.builders.Builder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.BlurTaskInstance;
import cn.lazymoon.sxmurxy.instance.SimpleFrameBufferInstance;
import cn.lazymoon.sxmurxy.instance.TextureTaskInstance;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(GameRenderer.class)
public class MixinGameRenderer implements InstanceAccess {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    public void onRenderScreenEvent(RenderTickCounter tickCounter, boolean tick, CallbackInfo callbackInfo) {
        if (ClientUtils.isNull()) {
            return;
        }
        if (mc.currentScreen instanceof MessageScreen) {
            return;
        }
        if (mc.currentScreen instanceof DownloadingTerrainScreen) {
            return;
        }
        if (mc.options.hudHidden) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        States.push();
        RenderHelper.beginRender();
        SimpleFrameBufferInstance.updateSimpleFrameBufferInstance();
        SimpleFrameBufferInstance.renderSimpleFrameBufferInstance();
        SimpleFrameBufferInstance.setShaderTexturePre();
        BlurTaskInstance.runTask();
        SimpleFrameBufferInstance.setShaderTexturePost();
        BlurTaskInstance.clearTask();
        /// Event
        DrawContext drawContext = new DrawContext(mc, mc.gameRenderer.buffers.getEntityVertexConsumers());
        Matrix4f matrix4f = drawContext.getMatrices().peek().getPositionMatrix();
        Client.INSTANCE.getEventManager().call(new RenderNvgEvent(drawContext, matrix4f, tickCounter));
        if (Client.INSTANCE.getModuleManager() != null && Client.INSTANCE.getModuleManager().getModule(Notification.class).isState())
            NotificationManager.publish(mc.getWindow(), matrix4f);
        TextureTaskInstance.runTask();
        TextureTaskInstance.clearTask();
        SimpleFrameBufferInstance.updateSimpleFrameBufferInstance();
        SimpleFrameBufferInstance.renderSimpleFrameBufferInstance();
        SimpleFrameBufferInstance.setShaderTexturePre();
        BuiltBlur blur = Builder.blur().size(new SizeState(1, 1)).radius(new QuadRadiusState(5f)).blurRadius(20).smoothness(5f).color(QuadColorState.TRANSPARENT).position(new PositionState(1, 1)).matrix4f(new DrawContext(mc, mc.gameRenderer.buffers.getEntityVertexConsumers()).getMatrices().peek().getPositionMatrix()).build();
        blur.render(blur.matrix4f(), blur.positionState().x(), blur.positionState().y());
        SimpleFrameBufferInstance.setShaderTexturePost();
        RenderHelper.endRender();
        States.pop();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
    }

    @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float hookNausea(float original) {
        if (!(AntiBlind.nausea.get() && Client.INSTANCE.getModuleManager().getModule(AntiBlind.class).isState())) {
            return 0f;
        }
        return original;
    }

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
        var material = new MatrixStack();
        material.multiplyPositionMatrix(matrix4f);
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
        float partialTicks = RenderUtils.getTickDelta();
        Render3DEvent event = new Render3DEvent(material,tickCounter, partialTicks, projectionMatrix);
        Client.INSTANCE.getEventManager().call(event);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void noHurtCamHook(MatrixStack matrixStack, float f, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player != null) {
            if (Objects.requireNonNull(Client.INSTANCE.getModuleManager().getModule(Camera.class)).isState() && Camera.cameraOptions.isEnabled("No Hurt Tilt")) {
                ci.cancel();
            }
        }
    }
}
