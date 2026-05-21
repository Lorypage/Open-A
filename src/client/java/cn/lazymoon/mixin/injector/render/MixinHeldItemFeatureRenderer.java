package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.utils.client.LegacyBlockingUtil;
import cn.lazymoon.utils.render.LegacyEntityUtils;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

@Mixin(HeldItemFeatureRenderer.class)
public abstract class MixinHeldItemFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ModelWithArms> extends FeatureRenderer<S, M> {
    public MixinHeldItemFeatureRenderer(FeatureRendererContext<S, M> context) {
        super(context);
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void animatium$setRef(S armedEntityRenderState, ItemRenderState itemStackRenderState, Arm arm, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci, @Share("stack") LocalRef<ItemStack> stackRef) {
        if (LegacyBlockingUtil.shoulditemPositionsInThirdPerson(armedEntityRenderState) && !itemStackRenderState.isEmpty()) {
            Entity entity = LegacyEntityUtils.getEntityByState(armedEntityRenderState);
            if (entity instanceof LivingEntity livingEntity && armedEntityRenderState instanceof ArmedEntityRenderState) {
                stackRef.set(livingEntity.getStackInArm(arm));
            }
        }
    }

    @ModifyArgs(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private void animatium$oldTransformTranslation(Args args, @Local(argsOnly = true) S entityState, @Share("stack") LocalRef<ItemStack> stackRef) {
        if (LegacyBlockingUtil.shoulditemPositionsInThirdPerson(entityState) && !LegacyBlockingUtil.isItemBlacklisted(stackRef.get())) {
            args.setAll((float) args.get(0) * -1.0F, 0.4375F, (float) args.get(2) / 10 * -1.0F);
        }
    }

    @WrapWithCondition(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V"))
    private boolean animatium$removeTransformMultiply(MatrixStack instance, Quaternionf quaternionf, @Local(argsOnly = true) S entityState, @Share("stack") LocalRef<ItemStack> stackRef) {
        return !LegacyBlockingUtil.shoulditemPositionsInThirdPerson(entityState) || LegacyBlockingUtil.isItemBlacklisted(stackRef.get());
    }

    @Inject(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V"))
    private void animatium$itemPositionsThird(S entityRenderState, ItemRenderState itemStackRenderState, Arm arm, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (LegacyBlockingUtil.shoulditemPositionsInThirdPerson(entityRenderState)) {
            Entity entity = LegacyEntityUtils.getEntityByState(entityRenderState);
            if (entity instanceof LivingEntity livingEntity && entityRenderState instanceof ArmedEntityRenderState armedEntityRenderState) {
                int direction = LegacyBlockingUtil.getArmMultiplier(arm);
                ItemStack stack = livingEntity.getStackInArm(arm);
                Item item = stack.getItem();
                if (!stack.isEmpty() && !LegacyBlockingUtil.isItemBlacklisted(stack)) {
                    boolean isStickRod = item == Items.FISHING_ROD &&
                            (livingEntity instanceof PlayerEntity player && player.fishHook != null);
                    if (LegacyBlockingUtil.isBlock3d(stack, itemStackRenderState)) {
                        float scale = 0.375F;
                        matrixStack.translate(0.0F, 0.1875F, -0.3125F);
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 45.0F));
                        matrixStack.scale(-scale, -scale, scale);
                    } else if (item instanceof BowItem) {
                        float scale = 0.625F;
                        matrixStack.translate(direction * 0.0F, 0.125F, 0.3125F);
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -20.0F));
                        matrixStack.translate(direction * -0.0625F, 0.0F, 0.0F);
                        matrixStack.scale(scale, scale, scale);
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(100.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -145.0F));
                        matrixStack.translate(-0.011765625F, 0.0F, 0.002125F);
                    } else if (LegacyBlockingUtil.isHandheldItem(stack)) {
                        float scale = 0.625F;
                        if (LegacyBlockingUtil.isFishingRodItem(stack) && !isStickRod) {
                            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * 180.0F));
                            matrixStack.translate(0.0F, -0.125F, 0.0F);
                        }

                        if (LegacyEntityUtils.isBlocking(livingEntity, stack) && LegacyEntityUtils.isBlockingArm(arm, armedEntityRenderState)) {
                            matrixStack.translate(direction * 0.05F, 0.0F, -0.1F);
                            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -50.0F));
                            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * -60.0F));
                        }

                        matrixStack.translate(direction * -0.0625F, 0.1875F, 0.0F);
                        matrixStack.scale(scale, scale, scale);
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(100));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -145));
                        matrixStack.translate(-0.011765625F, 0.0F, 0.002125F);
                    } else {
                        float scale = 0.375F;
                        matrixStack.translate(direction * 0.25F, 0.1875F, -0.1875F);
                        matrixStack.scale(scale, scale, scale);
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * 60.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * 20.0F));
                    }

                    if (!LegacyBlockingUtil.isBlock3d(stack, itemStackRenderState)) {
                        matrixStack.translate(0.0F, -0.3F, 0.0F);
                        matrixStack.scale(1.5F, 1.5F, 1.5F);
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 50.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * 335.0F));
                        matrixStack.translate(direction * -0.9375F, -0.0625F, 0.0F);

                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 180.0F));
                        matrixStack.translate(direction * -0.5F, 0.5F, 0.03125F);
                    }

                    if (LegacyBlockingUtil.isBlock3d(stack, itemStackRenderState)) {
                        matrixStack.scale(1 / 0.375F, 1 / 0.375F, 1 / 0.375F);
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -45.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75.0F));
                        matrixStack.translate(0.0F, -2.5F * 0.0625F, 0.0F);
                    } else if (item instanceof BowItem) {
                        matrixStack.scale(1 / 0.9F, 1 / 0.9F, 1 / 0.9F);
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * 40.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -260.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(80.0F));
                        matrixStack.translate(direction * 0.0625F, 2.0F * 0.0625F, -2.5F * 0.0625F);
                    } else if (LegacyBlockingUtil.isHandheldItem(stack)) {
                        boolean isRod = LegacyBlockingUtil.isFishingRodItem(stack) && !isStickRod;
                        matrixStack.scale(1 / 0.85F, 1 / 0.85F, 1 / 0.85F);
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * -55.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 90.0F));
                        if (isRod) {
                            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -180.0F));
                        }

                        matrixStack.translate(0.0F, -4.0F * 0.0625F, -0.5F * 0.0625F);
                        if (isRod) {
                            matrixStack.translate(0.0F, 0.0F, -2.0F * 0.0625F);
                        }
                    } else {
                        matrixStack.scale(1 / 0.55F, 1 / 0.55F, 1 / 0.55F);
                        matrixStack.translate(0.0F, -3.0F * 0.0625F, -1.0F * 0.0625F);
                    }
                }
            }
        }
    }
}