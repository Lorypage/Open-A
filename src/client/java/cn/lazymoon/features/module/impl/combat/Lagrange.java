package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.blink.BlinkComponent;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.component.rotation.utils.Priority;
import cn.lazymoon.component.rotation.utils.SmoothMode;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.AttackEvent;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.event.impl.render.Render3DEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.event.impl.world.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.player.PlayerUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import cn.lazymoon.utils.render.RenderUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.Objects;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@ModuleInfo(name = "Lagrange", description = "Attack-triggered blink/delay effect for friend-room style showcase", key = 0, category = Category.Combat, hidden = false)
public class Lagrange extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"Blink", "Delay"}, "Blink");
    public final NumberValue distance = new NumberValue("Distance", 4.5, 1.0, 8.0, 0.1);
    public final NumberValue delayTick = new NumberValue("Delay Tick", 4, 1, 20, 1);
    public final BoolValue fixedRotation = new BoolValue("Fixed Rotation", true);
    public final BoolValue render = new BoolValue("Render Box", true);

    private int activeTicks = 0;
    private boolean active = false;

    private Box startBox = null;
    private Rotation lockedRotation = null;

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        stopEffect();
        resetState();
    }

    @EventTarget
    public void onWorldChange(WorldEvent event) {
        this.setState(false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!event.isPost()) return;
        if (mc.player == null || mc.world == null) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (!isValidTarget(living)) return;
        if (living instanceof PlayerEntity) return;

        if (mc.player.squaredDistanceTo(living) > distance.get() * distance.get()) return;

        startEffect();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player == null || mc.world == null) return;

        if (!active) return;

        // 没有目标在范围内，直接结束
        if (!hasEntityInRange()) {
            stopEffect();
            return;
        }

        // tick 计时结束
        if (activeTicks <= 0) {
            stopEffect();
            return;
        }

        activeTicks--;

        // 固定视角
        if (fixedRotation.get() && lockedRotation != null) {
            Client.INSTANCE.getRotationComponent().setRotations(
                    lockedRotation,
                    MovementFix.SILENT,
                    false,
                    SmoothMode.LINEAR,
                    180f,
                    1,
                    0,
                    Priority.VERY_HIGH
            );
        }

        // tick 结束时停止
        if (activeTicks <= 0) {
            stopEffect();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!active) return;

        if (fixedRotation.get() && lockedRotation != null) {
            event.setYaw(lockedRotation.getYaw());
            event.setPitch(lockedRotation.getPitch());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(mode.getValue());
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        if (!active) return;
        if (startBox == null) return;
        if (mc.player == null || mc.world == null) return;

        Color c = InterFace.color(0);
        Color boxColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 120);

        RenderUtils.drawBox(
                event.getMatrix(),
                startBox.expand(0.08),
                boxColor,
                false,
                null,
                true
        );
    }

    private void startEffect() {
        if (mc.player == null) return;

        active = true;
        activeTicks = delayTick.get().intValue();
        lockedRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        startBox = mc.player.getBoundingBox();

        if (mode.is("Blink")) {
            BlinkComponent.startBlink();
        } else if (mode.is("Delay")) {
            BlinkComponent.startDelay();
        }
    }

    private void stopEffect() {
        if (!active) return;

        if (mode.is("Blink")) {
            BlinkComponent.stopBlink();
        } else if (mode.is("Delay")) {
            BlinkComponent.stopDelay();
        }

        active = false;
        activeTicks = 0;
        startBox = null;
        lockedRotation = null;
    }

    private void resetState() {
        active = false;
        activeTicks = 0;
        startBox = null;
        lockedRotation = null;
    }

    private boolean hasEntityInRange() {
        if (mc.player == null || mc.world == null) return false;

        double rangeSq = distance.get() * distance.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isRemoved()) continue;
            if (living instanceof PlayerEntity) continue;

            if (mc.player.squaredDistanceTo(living) <= rangeSq) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null) return false;
        if (!entity.isAlive() || entity.isRemoved()) return false;
        if (entity == mc.player) return false;

        // 这里默认允许所有怪物/生物，不攻击玩家
        return true;
    }
}