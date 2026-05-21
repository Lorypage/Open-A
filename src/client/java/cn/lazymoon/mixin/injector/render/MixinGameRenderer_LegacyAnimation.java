package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.utils.accessor.ViewBobbingStorage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_LegacyAnimation {

    @Shadow
    @Final
    private MinecraftClient client;

    @WrapOperation(method = "tiltViewWhenHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getDamageTiltYaw()F"))
    private float animatium$revertYaw(LivingEntity instance, Operation<Float> original) {
        return 0.0F;
    }

    @WrapOperation(method = "tiltViewWhenHurt", at= @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;hurtTime:I"))
    private int animatium$hurtTime(LivingEntity instance, Operation<Integer> original) {
        int hurtTime = original.call(instance);
        return Math.max(hurtTime - 1, 0);
    }

    @Inject(method = "bobView", at = @At("TAIL"))
    private void animatium$fixVerticalBobbingTilt(MatrixStack poseStack, float tickDelta, CallbackInfo ci) {
        if (this.client.getCameraEntity() instanceof PlayerEntity player) {
            ViewBobbingStorage bobbingAccessor = (ViewBobbingStorage) player;
            float j = MathHelper.lerp(tickDelta, bobbingAccessor.arcane_nextgen$getPreviousBobbingTilt(), bobbingAccessor.arcane_nextgen$getBobbingTilt());
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(j));
        }
    }

    @WrapOperation(method = "bobView", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;distanceMoved:F"))
    private float animatium$changeDistance(AbstractClientPlayerEntity instance, Operation<Float> original) {
        return ((ViewBobbingStorage) instance).arcane_nextgen$getHorizontalSpeed();
    }

    @WrapOperation(method = "bobView", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;lastDistanceMoved:F"))
    private float animatium$changePreviousDistance(AbstractClientPlayerEntity instance, Operation<Float> original) {
        return ((ViewBobbingStorage) instance).arcane_nextgen$getPreviousHorizontalSpeed();
    }

}
