package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.utils.render.LegacyEntityUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Arm;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BipedEntityModel.class)
public class MixinBipedEntityModel<T extends BipedEntityRenderState> {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

   /* @Inject(method = "setAngles*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;positionLeftArm(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;Lnet/minecraft/client/render/entity/model/BipedEntityModel$ArmPose;)V", ordinal = 0))
    public void positionLeftArm(T bipedEntityRenderState, CallbackInfo ci) {
        if (mc.world != null) {
            var customName = bipedEntityRenderState.customName;
            var displayName = bipedEntityRenderState.displayName;
            for (var player : mc.world.getPlayers()) {
                if (player.getCustomName() != null && player.getDisplayName() != null && player.getCustomName().equals(customName) && player.getDisplayName().equals(displayName)) {
                    if (Client.moduleManager.getModule(Animation.class).isEnabled() && Animation.blockAnimation.getValue() && (Animation.everythingBlock.getValue() || player.getMainHandStack().getItem() instanceof SwordItem) && player.isUsingItem() && player.getOffHandStack().getItem() instanceof ShieldItem && player.getMainHandStack().getItem() instanceof SwordItem) {
                        this.rightArm.pitch = this.rightArm.pitch * 0.5F - 0.9424779F;
                        this.rightArm.yaw = (float) (-Math.PI / 6);
                    }
                    break;
                }
            }
        }
    }*/

    @WrapOperation(method = {"positionLeftArm", "positionRightArm"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;positionBlockingArm(Lnet/minecraft/client/model/ModelPart;Z)V"))
    private void animatium$oldSwordBlockArm(BipedEntityModel<?> instance, ModelPart arm, boolean rightArm, Operation<Void> original, @Local(argsOnly = true) T state) {
        original.call(instance, arm, rightArm);

        Entity entity = LegacyEntityUtils.getEntityByState(state);
        if (entity instanceof LivingEntity livingEntity && state instanceof BipedEntityRenderState) {
            ItemStack stack = livingEntity.getStackInArm(rightArm ? Arm.RIGHT : Arm.LEFT);
            if (!(stack.getItem() instanceof ShieldItem)) {
                arm.pitch = arm.pitch * 0.5F - ((float) Math.PI / 10F) * 2F;
                arm.yaw = 0;
            }
        }
    }

    @ModifyExpressionValue(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;isUsingItem:Z", ordinal = 0))
    private boolean animatium$fixOffHandUsingPose(boolean original) {
        return false;
    }

    @WrapOperation(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;isInSneakingPose:Z"))
    private boolean animatium$sneakingFeetPosition(BipedEntityRenderState instance, Operation<Boolean> original) {
        if (OldHitting.bodyOffset.get() && instance.isInSneakingPose) {
            body.pitch = 0.5F;
            rightArm.pitch += 0.4F;
            leftArm.pitch += 0.4F;
            rightLeg.pivotZ = 4.0F;
            leftLeg.pivotZ = 4.0F;
            rightLeg.pivotY = 9.0F;
            leftLeg.pivotY = 9.0F;
            head.pivotY = 1.0F;
            return false;
        } else {
            return original.call(instance);
        }
    }

}
