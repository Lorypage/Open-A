package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.component.rotation.utils.Priority;
import cn.lazymoon.component.rotation.utils.SmoothMode;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.rotation.RotationUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

@ModuleInfo(
        name = "RageBot",
        description = "Auto aim and click selected entities",
        key = 0,
        category = Category.Combat,
        hidden = false
)
public class MobAssist extends Module {

    // 左键 / 右键 / 两者都做
    private final ModeValue clickMode = new ModeValue("Click Mode", "Attack", new String[]{"Attack", "Interact", "Both"});

    private final BoolValue aim = new BoolValue("Aim Assist", true);
    private final BoolValue throughWalls = new BoolValue("Through Walls", false);

    // 最大距离拉到 40
    private final NumberValue range = new NumberValue("Range", 6.0, 1.0, 40.0, 0.1);
    private final NumberValue fov = new NumberValue("FOV", 120.0, 20.0, 180.0, 1.0);
    private final NumberValue cps = new NumberValue("CPS", 10.0, 1.0, 20.0, 1.0);
    private final NumberValue rotationSpeed = new NumberValue("Rotation Speed", 55.0, 1.0, 180.0, 1.0);

    // 自定义实体类型
    private final MultiBoolValue targets = new MultiBoolValue("Targets", Arrays.asList(
            new BoolValue("Players", true),
            new BoolValue("Hostile", true),
            new BoolValue("Animals", false),
            new BoolValue("Villagers", false)
    ));

    private final TimerUtils actionTimer = new TimerUtils();
    private LivingEntity currentTarget;

    @Override
    public void onEnable() {
        currentTarget = null;
        actionTimer.reset();
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        if (Client.INSTANCE.getRotationComponent() != null) {
            Client.INSTANCE.getRotationComponent().reset();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player == null || mc.world == null) return;

        currentTarget = findTarget();

        if (currentTarget == null) {
            if (Client.INSTANCE.getRotationComponent() != null) {
                Client.INSTANCE.getRotationComponent().reset();
            }
            return;
        }

        if (aim.get()) {
            lookAtTarget(currentTarget);
        }

        if (canAct()) {
            doAction(currentTarget);
        }
    }

    private void lookAtTarget(LivingEntity target) {
        Vec3d aimPos = getAimPos(target);
        Rotation rot = RotationUtils.toRotation(mc.player.getEyePos(), aimPos);

        Client.INSTANCE.getRotationComponent().setRotations(
                rot,
                MovementFix.SILENT,
                true,
                SmoothMode.ADVANCED,
                rotationSpeed.get().floatValue(),
                1,
                0,
                Priority.MEDIUM
        );
    }

    private boolean canAct() {
        return currentTarget != null
                && currentTarget.isAlive()
                && !currentTarget.isRemoved()
                && mc.player != null
                && mc.player.squaredDistanceTo(currentTarget) <= range.get() * range.get();
    }

    private void doAction(LivingEntity target) {
        if (mc.interactionManager == null || mc.player == null) return;

        double delay = 1000.0 / Math.max(1.0, cps.get());
        if (!actionTimer.hasTimeElapsed((long) delay)) return;

        boolean didSomething = false;

        // 左键：攻击
        if (clickMode.is("Attack") || clickMode.is("Both")) {
            if (mc.player.getAttackCooldownProgress(0.0f) >= 1.0f) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                didSomething = true;
            }
        }

        // 右键：交互
        if (clickMode.is("Interact") || clickMode.is("Both")) {
            mc.interactionManager.interactEntity(mc.player, target, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            didSomething = true;
        }

        if (didSomething) {
            actionTimer.reset();
        }
    }

    private LivingEntity findTarget() {
        if (mc.player == null || mc.world == null) return null;

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isRemoved()) continue;

            if (!isValidTarget(living)) continue;

            double distSq = mc.player.squaredDistanceTo(living);
            double maxDistSq = range.get() * range.get();
            if (distSq > maxDistSq) continue;

            if (!throughWalls.get() && !mc.player.canSee(living)) continue;

            Rotation rotation = RotationUtils.toRotation(mc.player.getEyePos(), getAimPos(living));

            float yawDiff = Math.abs(MathHelper.wrapDegrees(rotation.getYaw() - mc.player.getYaw()));
            float pitchDiff = Math.abs(rotation.getPitch() - mc.player.getPitch());

            if (yawDiff > fov.get() / 2.0f) continue;

            // 越靠近准星、越近的优先
            double score = yawDiff * 2.0 + pitchDiff + Math.sqrt(distSq);

            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }

        return best;
    }

    private boolean isValidTarget(LivingEntity living) {
        if (living instanceof PlayerEntity) {
            return targets.isEnabled("Players");
        }

        if (living instanceof HostileEntity) {
            return targets.isEnabled("Hostile");
        }

        if (living instanceof AnimalEntity) {
            return targets.isEnabled("Animals");
        }

        if (living instanceof VillagerEntity) {
            return targets.isEnabled("Villagers");
        }

        return false;
    }

    private Vec3d getAimPos(LivingEntity target) {
        // 更偏向上半身/头部
        Vec3d center = target.getBoundingBox().getCenter();
        return new Vec3d(center.x, target.getY() + target.getHeight() * 0.75f, center.z);
    }
}