package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.player.LookEvent;
import cn.lazymoon.event.impl.player.StrafeEvent;
import cn.lazymoon.utils.accessor.IMixinCameraEntity;
import cn.lazymoon.utils.accessor.ViewBobbingStorage;
import cn.lazymoon.utils.client.ClientData;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.lazymoon.utils.InstanceAccess.mc;

@Mixin(Entity.class)
public abstract class MixinEntity implements IMixinCameraEntity, ViewBobbingStorage {
    @Shadow public abstract float getYaw(float tickDelta);
    @Shadow public abstract float getPitch(float tickDelta);
    @Shadow
    public abstract Vec3d getRotationVector(float var1, float var2);
    @Shadow
    protected static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        return null;
    }
    @Shadow public abstract Vec3d getVelocity();
    @Shadow public abstract boolean isOnGround();
    @Shadow public abstract void onLanding();

    /**
     * @author
     * @reason
     */
    @Overwrite
    public final Vec3d getRotationVec(float p_20253_) {
        float pitch = this.getPitch(p_20253_);
        float yaw = this.getYaw(p_20253_);
        Entity thisEntity = (Entity) (Object) this;

        if (thisEntity == MinecraftClient.getInstance().player) {
            LookEvent lookEvent = new LookEvent(thisEntity, yaw, pitch);
            Client.INSTANCE.getEventManager().call(lookEvent);
            yaw = lookEvent.yaw;
            pitch = lookEvent.pitch;
        }

        return this.getRotationVector(pitch, yaw);
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            var event = Client.INSTANCE.getEventManager().call(new StrafeEvent(yaw));
            return movementInputToVelocity(movementInput, speed, event.getYaw());
        }

        return movementInputToVelocity(movementInput, speed, yaw);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", shift = At.Shift.AFTER))
    private void fuckMojangFallDistance(MovementType type, Vec3d movement, CallbackInfo ci, @Local(ordinal = 1) Vec3d vec3d) {
        Entity entity = (Entity) (Object) this;
        if (entity == mc.player) {
            if (this.isOnGround()) {
                this.onLanding();
            } else if (vec3d.y < 0.0) {
                ClientData.setFallDistance((float) (ClientData.getFallDistance() - vec3d.y));
            }
        }
    }

    @Inject(method = "onLanding", at = @At("HEAD"))
    private void onLanding(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity == mc.player) {
            ClientData.setFallDistance(0);
        }
    }

    @Inject(method = "limitFallDistance", at = @At("HEAD"))
    private void limitFallDistance(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity == mc.player) {
            if (this.getVelocity().getY() > -0.5 && ClientData.getFallDistance() > 1.0F) {
                ClientData.setFallDistance(1.0F);
            }
        }
    }

    @Inject(method = "readNbt", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtCompound;getFloat(Ljava/lang/String;)F"))
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity == mc.player) {
            ClientData.setFallDistance(nbt.getFloat("FallDistance"));
        }
    }

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setOnFireFromLava()V", shift = At.Shift.AFTER))
    private void baseTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity == mc.player) {
            ClientData.setFallDistance(ClientData.getFallDistance() * 0.5F);
        }
    }

    @Unique
    private float animatium$horizontalSpeed = 0.0F;
    @Unique
    private float animatium$previousHorizontalSpeed = 0.0F;

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;tickPortalTeleportation()V", shift = At.Shift.AFTER))
    private void animatium$StorePreviousHorizontalSpeed(CallbackInfo ci) {
        this.animatium$previousHorizontalSpeed = this.animatium$horizontalSpeed;
    }

    @Inject(method = "applyMoveEffect", at = @At("HEAD"))
    private void animation$storeHorizontalSpeed(Entity.MoveEffect movementEmission, Vec3d vec3d, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        this.animatium$horizontalSpeed = this.animatium$horizontalSpeed + (float) vec3d.horizontalLength() * 0.6F;
    }

    @Override
    public float arcane_nextgen$getHorizontalSpeed() {
        return this.animatium$horizontalSpeed;
    }

    @Override
    public float arcane_nextgen$getPreviousHorizontalSpeed() {
        return this.animatium$previousHorizontalSpeed;
    }
}