package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.event.impl.player.JumpEvent;
import cn.lazymoon.features.module.impl.visual.Animations;
import cn.lazymoon.features.module.impl.visual.AntiBlind;
import cn.lazymoon.utils.accessor.ViewBobbingStorage;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity implements ViewBobbingStorage {
    @Shadow
    public float bodyYaw;
    @Unique
    private JumpEvent jumpEvent;
    @Unique private float preYaw = 0f;

    @Inject(method = "turnHead", at = @At("HEAD"), cancellable = true)
    private void overrideTurnHead(float bodyRotation, float headRotation, CallbackInfoReturnable<Float> cir) {
        Entity entity = (Entity) (Object) this;

        if (!(entity instanceof ClientPlayerEntity)) return;
        if (RotationComponent.serverRotation == null) return;

        float targetYaw = RotationComponent.serverRotation.getYaw();

        float f = MathHelper.wrapDegrees(bodyRotation - this.bodyYaw);
        this.bodyYaw += f * 0.3F;
        float g = MathHelper.wrapDegrees(targetYaw - this.bodyYaw);

        g = MathHelper.clamp(g, -75.0F, 75.0F);
        this.bodyYaw = targetYaw - g;
        if (Math.abs(g) > 50.0F) {
            this.bodyYaw += g * 0.2F;
        }

        boolean bl = g < -90.0F || g >= 90.0F;
        cir.setReturnValue(bl ? -headRotation : headRotation);
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    public void getHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (Client.INSTANCE.getModuleManager().getModule(Animations.class).isState()) {
            cir.setReturnValue(Animations.swingSpeed.get().intValue());
        }
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void hookAntiNausea(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == StatusEffects.NAUSEA && !(AntiBlind.nausea.get() && Client.INSTANCE.getModuleManager().getModule(AntiBlind.class).isState())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "jump",at = @At("HEAD"),cancellable = true)
    public void jumpPre(CallbackInfo callbackInfo) {
        if((LivingEntity)(Object)this instanceof ClientPlayerEntity playerEntity) {
            jumpEvent = new JumpEvent(playerEntity.getYaw());
            Client.INSTANCE.getEventManager().call(jumpEvent);
            if (jumpEvent.isCancelled()) {
                callbackInfo.cancel();
                return;
            }
            preYaw = playerEntity.getYaw();
            playerEntity.setYaw(jumpEvent.yaw);
        }
    }

    @Inject(method = "jump",at = @At("TAIL"))
    public void jumpPost(CallbackInfo ci){
        if((LivingEntity)(Object)this instanceof ClientPlayerEntity playerEntity && jumpEvent != null) {
            jumpEvent = null;
            playerEntity.setYaw(preYaw);
        }
    }
    @Unique
    private float animatium$bobbingTilt = 0.0F;
    @Unique
    private float animatium$previousBobbingTilt = 0.0F;

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;tickStatusEffects()V", shift = At.Shift.BEFORE))
    private void updatePreviousBobbingTiltValue(CallbackInfo ci) {
        this.animatium$previousBobbingTilt = this.animatium$bobbingTilt;
    }

    @Override
    public void arcane_nextgen$setBobbingTilt(float bobbingTilt) {
        this.animatium$bobbingTilt = bobbingTilt;
    }

    @Override
    public float arcane_nextgen$getBobbingTilt() {
        return this.animatium$bobbingTilt;
    }

    @Override
    public float arcane_nextgen$getPreviousBobbingTilt() {
        return this.animatium$previousBobbingTilt;
    }
}