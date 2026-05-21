package cn.lazymoon.utils.render;

import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.mixin.injector.accessor.CameraAccessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public final class LegacyEntityUtils {
    private static final ThreadLocal<@Nullable BipedEntityRenderState> HUMAN_RENDER_STATE = ThreadLocal.withInitial(() -> null);
    private static final HashMap<EntityRenderState, Entity> STATE_TO_ENTITY = new HashMap<>();

    public static void setEntityByState(EntityRenderState state, Entity entity) {
        STATE_TO_ENTITY.put(state, entity);
    }

    public static @Nullable Entity getEntityByState(EntityRenderState state) {
        return STATE_TO_ENTITY.getOrDefault(state, null);
    }

    public static void setHumanRenderState(BipedEntityRenderState state) {
        HUMAN_RENDER_STATE.set(state);
    }

    public static void removeHumanRenderState() {
        HUMAN_RENDER_STATE.remove();
    }

    public static @Nullable BipedEntityRenderState getHumanRenderState() {
        return HUMAN_RENDER_STATE.get();
    }

    public static boolean isBlocking(LivingEntity livingEntity, ItemStack stack) {
        return (livingEntity instanceof PlayerEntity player && ((player.getItemUseTimeLeft() > 0 && stack.getUseAction() == UseAction.BLOCK)
                || (player instanceof ClientPlayerEntity && stack.getItem() instanceof SwordItem && KillAura.renderblock)));
    }

    public static boolean isBlockingArm(Arm arm, ArmedEntityRenderState armedEntityState) {
        if (arm == Arm.LEFT && (armedEntityState.leftArmPose == BipedEntityModel.ArmPose.BLOCK)) {
            return true;
        } else return arm == Arm.RIGHT && (armedEntityState.rightArmPose == BipedEntityModel.ArmPose.BLOCK
                || (getEntityByState(armedEntityState) instanceof ClientPlayerEntity && KillAura.renderblock));
    }

    public static float lerpCameraPosition(Camera camera) {
        CameraAccessor cameraAccessor = (CameraAccessor) camera;
        return MathHelper.lerp(camera.getLastTickDelta(), cameraAccessor.getLastCameraY(), cameraAccessor.getCameraY());
    }
}

