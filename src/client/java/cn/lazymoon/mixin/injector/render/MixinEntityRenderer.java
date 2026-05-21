package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.utils.accessor.EntityRendererAddon;
import cn.lazymoon.utils.render.LegacyEntityUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> implements EntityRendererAddon {

    @Unique
    public Entity entity;

    @Inject(method = "getDisplayName", at = @At("HEAD")) public void getDisplayName(T entity, CallbackInfoReturnable<Text> callbackInfoReturnable) {
        this.entity = entity;
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private <T extends Entity, S extends EntityRenderState> void animatium$saveEntityByState(T entity, S state, float tickDelta, CallbackInfo ci) {
        LegacyEntityUtils.setEntityByState(state, entity);
    }

    @WrapOperation(method = "renderLabelIfPresent", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;sneaking:Z"))
    private boolean animatium$sneakAnimationWhileFlying(EntityRenderState instance, Operation<Boolean> original) {
        if (OldHitting.sneakAnimationWhileFlying.get() && instance instanceof LivingEntityRenderState livingEntityRenderState) {
            return livingEntityRenderState.sneaking || livingEntityRenderState.isInPose(EntityPose.CROUCHING);
        } else {
            return original.call(instance);
        }
    }

    @SuppressWarnings("AddedMixinMembersNamePattern") @Override public Entity getEntity() {
        return entity;
    }

}