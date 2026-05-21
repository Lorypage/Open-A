package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.features.module.impl.visual.OldHitting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ElytraFlightController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ElytraFlightController.class)
public class MixinElytraFlightController {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInSneakingPose()Z"))
    private boolean animatium$sneakAnimationWhileFlying(LivingEntity livingEntity, Operation<Boolean> original) {
        boolean isCrouching = original.call(livingEntity);
        if (OldHitting.sneakAnimationWhileFlying.get()) {
            return isCrouching || livingEntity.isSneaking();
        } else {
            return isCrouching;
        }
    }
}
