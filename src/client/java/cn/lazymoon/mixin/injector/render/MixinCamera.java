package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.visual.Camera;
import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.mixin.injector.accessor.PlayerEntityAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static cn.lazymoon.utils.InstanceAccess.mc;
import static cn.lazymoon.utils.animations.Animation.animate;

@Mixin(net.minecraft.client.render.Camera.class)
public abstract class MixinCamera {

    @Shadow private float lastCameraY;
    @Shadow private float cameraY;
    @Shadow private float pitch;
    @Shadow private float yaw;
    @Unique public double lastRenderX = 0;
    @Unique public double lastRenderY = 0;
    @Unique public double lastRenderZ = 0;
    @Unique private float smoothedThirdPersonDistance;
    @Unique private float smoothedThirdPersonRotate;
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract float clipToSpace(float f);
    @Shadow protected abstract void moveBy(float f, float g, float h);
    @Unique private boolean firstTime = true;

    @Shadow
    private Entity focusedEntity;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", shift = At.Shift.AFTER))
    private void animatium$removeSmoothSneaking(CallbackInfo ci) {
        if (!OldHitting.smoothSneaking.get()) {
            this.lastCameraY = cameraY;
            this.cameraY = this.animation$getStandingEyeHeight();
        }
    }

    @WrapOperation(method = "updateEyeHeight", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getStandingEyeHeight()F"))
    private float animatium$useOldEyeHeight(Entity instance, Operation<Float> original) {
        if (OldHitting.fakeEyeHeight.get()) {
            return this.animation$getStandingEyeHeight();
        } else {
            return original.call(instance);
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"), cancellable = true)
    public void update(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        double renderX = MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX());
        double renderY = MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY()) + MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY);
        double renderZ = MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ());

            firstTime = true;

        if (Client.INSTANCE.getModuleManager().getModule(Camera.class).isState()) {
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
                if (Camera.cameraOptions.isEnabled("Motion Camera")) {
                    this.smoothedThirdPersonDistance = 0;
                }
                if (Camera.cameraOptions.isEnabled("Motion Camera")) {
                    this.lastRenderX = renderX;
                    this.lastRenderY = renderY;
                    this.lastRenderZ = renderZ;
                }
                this.setPos(renderX, renderY, renderZ);
            } else if (Camera.cameraOptions.isEnabled("Motion Camera")) {
                double speed = Camera.cameraSpeed.getValue();
                this.lastRenderX = animate(this.lastRenderX * 1000, renderX * 1000, speed) / 1000;
                this.lastRenderY = animate(this.lastRenderY * 1000, renderY * 1000, speed) / 1000;
                this.lastRenderZ = animate(this.lastRenderZ * 1000, renderZ * 1000, speed) / 1000;
                this.setPos(this.lastRenderX, this.lastRenderY, this.lastRenderZ);
            } else {
                this.setPos(renderX, renderY, renderZ);
            }
        } else {
            this.setPos(renderX, renderY, renderZ);
        }
        if (thirdPerson) {
            if (inverseView) {
                if (Client.INSTANCE.getModuleManager().getModule(Camera.class).isState() && Camera.cameraOptions.isEnabled("Smooth")) {
                    this.smoothedThirdPersonRotate = (float) (animate(this.smoothedThirdPersonRotate * 10, 1800, 0.1F) / 10);
                    this.setRotation(this.yaw + this.smoothedThirdPersonRotate, -this.pitch);
                } else {
                    this.setRotation(this.yaw + 180, -this.pitch);
                }
            } else {
                this.smoothedThirdPersonRotate = 0;
            }
            this.moveBy(this.getX(focusedEntity), 0, 0);
        } else if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
            Direction direction = ((LivingEntity) focusedEntity).getSleepingDirection();
            this.setRotation(direction != null ? direction.getPositiveHorizontalDegrees() - 180 : 0, 0);
            this.moveBy(0, 0.3f, 0);
        }

        if (!OldHitting.cameraVersion.get().equals("Latest") && !thirdPerson && !(focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping())) {
            switch (OldHitting.cameraVersion.getValue()) {
                case "Pre 1.8":
                    this.moveBy(-0.15F, 0, 0);
                case "Pre 1.13":
                    this.moveBy(0.1F, 0.0F, 0.0F);
                case "Pre 1.14":
                    this.moveBy(-0.05000000074505806F, 0.0F, 0.0F);
                case "Latest":
                    break;
            }
        }
        ci.cancel();
    }
    
    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void clipToSpace(float f, CallbackInfoReturnable<Float> cir) {
        if (Client.INSTANCE.getModuleManager().getModule(Camera.class).isState() && Camera.cameraOptions.isEnabled("No Camera Clip")) {
            cir.setReturnValue(f);
        }
    }

    @Inject(method = "getSubmersionType", at = @At("RETURN"), cancellable = true)
    private void getSubmersionType(CallbackInfoReturnable<CameraSubmersionType> cir) {
        if (Client.INSTANCE.getModuleManager().getModule(Camera.class).isState() && Camera.cameraOptions.isEnabled("No Fog")) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }

    @Unique
    private float getX(Entity focusedEntity) {
        if (Client.INSTANCE.getModuleManager().getModule(Camera.class).isState()) {
            if (Camera.cameraOptions.isEnabled("Smooth")) {
                return this.smoothedThirdPersonDistance = (float) (animate(this.smoothedThirdPersonDistance * 10, -this.clipToSpace(Camera.cameraDistance.getValue().floatValue()) * 10, 0.1f) / 10);
            } else {
                return -this.clipToSpace(Camera.cameraDistance.getValue().floatValue());
            }
        } else {
            return -this.clipToSpace(4 * (focusedEntity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1));
        }
    }

    @WrapOperation(method = "updateEyeHeight", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/render/Camera;cameraY:F"))
    private void animatium$oldSneakAnimationInterpolation(net.minecraft.client.render.Camera instance, float value, Operation<Void> original) {
        if (OldHitting.alternativeSmoothSneaking.get() && OldHitting.smoothSneaking.get() && this.focusedEntity.getStandingEyeHeight() < cameraY) {
            this.cameraY = this.animation$getStandingEyeHeight();
        } else {
            original.call(instance, value);
        }
    }

    @Unique
    private float animation$getStandingEyeHeight() {
        float standingEyeHeight = this.focusedEntity.getStandingEyeHeight();
        if (OldHitting.fakeEyeHeight.get()
                && this.focusedEntity.isInPose(EntityPose.CROUCHING)
                && this.focusedEntity instanceof PlayerEntity player && ((PlayerEntityAccessor) player).stuck(EntityPose.STANDING)) {
            float scale = this.focusedEntity instanceof LivingEntity livingEntity ? livingEntity.getScale() : 1.0F;
            return 1.54F * scale;
        } else {
            return standingEyeHeight;
        }
    }

}