package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.utils.accessor.RenderRotationAccessor;
import cn.lazymoon.utils.render.LegacyEntityUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @Author:Guyuemang
 * @Time:03-28
 */
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Redirect(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"
            )
    )
    private float hookHeadPitch(LivingEntity entity, float tickDelta) {
        if (entity instanceof RenderRotationAccessor && entity == MinecraftClient.getInstance().player) {
            return tickDelta == 1.0F ? RotationComponent.serverRotation.getPitch()
                    : MathHelper.lerp(tickDelta,
                    ((RenderRotationAccessor) entity).getPrevRenderPitchHead(),
                    ((RenderRotationAccessor) entity).getRenderPitchHead());
        }
        return entity.getLerpedPitch(tickDelta);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 1)
    )
    private void animatium$syncPlayerModelWithEyeHeight(
            S livingEntityRenderState,
            MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider,
            int light,
            CallbackInfo ci) {
        if (OldHitting.syncPlayerModelWithEyeHeight.get()) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (livingEntityRenderState instanceof PlayerEntityRenderState state && player != null && state.id == player.getId()) {
                float cameraLerpValue = LegacyEntityUtils.lerpCameraPosition(client.gameRenderer.getCamera());
                matrixStack.translate(0.0F, (PlayerEntity.STANDING_DIMENSIONS.eyeHeight() * player.getScale()) - cameraLerpValue, 0.0F);
            }
        }
    }

    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 0)
    )
    private float hookHeadYaw(float original, LivingEntity livingEntity, LivingEntityRenderState state, float tickDelta) {
        if (livingEntity instanceof RenderRotationAccessor && livingEntity == MinecraftClient.getInstance().player) {
            return MathHelper.lerpAngleDegrees(tickDelta, ((RenderRotationAccessor) livingEntity).getPrevRenderYawHead(), ((RenderRotationAccessor) livingEntity).getRenderYawHead());
        }
        return original;
    }
}
