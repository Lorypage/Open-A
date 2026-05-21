package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.utils.accessor.RenderRotationAccessor;
import cn.lazymoon.utils.accessor.ViewBobbingStorage;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements RenderRotationAccessor {
    @Unique public float renderPitchHead;
    @Unique public float prevRenderPitchHead;

    @Unique public float renderYawHead;
    @Unique public float prevRenderYawHead;

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World level) {
        super(entityType, level);
    }

    @Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
    private void removeCooldown(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (OldHitting.noCooldown.get()) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setMovementSpeed(F)V", shift = At.Shift.AFTER))
    private void updateBobbingTiltValues(CallbackInfo ci) {
        ViewBobbingStorage bobbingAccessor = (ViewBobbingStorage) this;
        float g = this.isOnGround() || this.getHealth() <= 0.0F ? 0.0F : (float) (Math.atan(-this.getVelocity().y * (double) 0.2F) * 15.0F);
        bobbingAccessor.arcane_nextgen$setBobbingTilt(MathHelper.lerp(0.8F, bobbingAccessor.arcane_nextgen$getBobbingTilt(), g));
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void tickRenderHead(CallbackInfo ci) {
        prevRenderPitchHead = renderPitchHead;
        renderPitchHead = this.getPitch();

        prevRenderYawHead = renderYawHead;
        renderYawHead = this.getYaw();

        if (RotationComponent.serverRotation != null){
            renderPitchHead = RotationComponent.serverRotation.getPitch();
            renderYawHead = RotationComponent.serverRotation.getYaw();
        }
    }

    @Override
    public float getRenderPitchHead() {
        return renderPitchHead;
    }

    @Override
    public float getPrevRenderPitchHead() {
        return prevRenderPitchHead;
    }

    @Override
    public float getRenderYawHead() {
        return renderYawHead;
    }

    @Override
    public float getPrevRenderYawHead() {
        return prevRenderYawHead;
    }
}