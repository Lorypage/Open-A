package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.features.module.impl.visual.OldHitting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BipedEntityRenderer.class)
public class MixinBipedEntityRenderer {
    @WrapOperation(method = "updateBipedRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInSneakingPose()Z"))
    private static boolean animatium$sneakAnimationWhileFlying(LivingEntity livingEntity, Operation<Boolean> original) {
        boolean isCrouching = original.call(livingEntity);
        if (OldHitting.sneakAnimationWhileFlying.get()) {
            return isCrouching || livingEntity.isSneaking();
        } else {
            return isCrouching;
        }
    }
}
