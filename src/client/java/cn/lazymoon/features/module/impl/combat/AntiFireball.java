package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.world.Scaffold;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.entity.RaycastUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(name = "AntiFireball", description = "Automatically strikes incoming fireball projectiles", key = 0, category = Category.Combat, hidden = false)
public class AntiFireball extends Module {
    public static BoolValue bypassSprintFlag = new BoolValue("Bypass Sprint Flag", false);
    public static BoolValue clientSideSwing = new BoolValue("Client Side Animation", true);
    public static NumberValue turnSpeed = new NumberValue("Turn Speed", 180, 0, 180, 1);
    public static NumberValue scanDistance = new NumberValue("Scan Distance", 8, 1, 20, 0.1);
    public static NumberValue strikeDistance = new NumberValue("Strike Distance", 4, 1, 20, 0.1);

    private final List<FireballEntity> monitoredProjectiles = new CopyOnWriteArrayList<>();

    @EventTarget
    public void onPlayerUpdate(UpdateEvent event) {
        ClientPlayerEntity user = mc.player;
        if (user == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState()) return;

        final double rangeSquared = scanDistance.get() * scanDistance.get();

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof FireballEntity projectile)) continue;

            Vec3d velocityVector = new Vec3d(projectile.getX() - projectile.prevX, projectile.getY() - projectile.prevY, projectile.getZ() - projectile.prevZ);
            Vec3d userDirection = user.getPos().subtract(projectile.getPos());

            boolean isApproaching = velocityVector.dotProduct(userDirection) > 0;
            boolean isInProximity = projectile.squaredDistanceTo(user) <= rangeSquared;

            if (isApproaching && isInProximity && !monitoredProjectiles.contains(projectile)) {
                monitoredProjectiles.add(projectile);
            }
        }

        monitoredProjectiles.removeIf(proj -> {
            Vec3d vel = new Vec3d(proj.getX() - proj.prevX, proj.getY() - proj.prevY, proj.getZ() - proj.prevZ);
            Vec3d dirToUser = user.getPos().subtract(proj.getPos());
            boolean isNotApproaching = vel.dotProduct(dirToUser) <= 0;
            boolean isTooFar = proj.squaredDistanceTo(user) > rangeSquared;
            return isNotApproaching || isTooFar;
        });

        if (monitoredProjectiles.isEmpty()) return;

        FireballEntity nearest = monitoredProjectiles.stream()
                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(user)))
                .orElse(null);

        Rotation lookAngles = computeAimRotation(user, nearest);
        Client.INSTANCE.getRotationComponent().setRotations(lookAngles, 1, MovementFix.SILENT, turnSpeed.get().floatValue());

        if (RaycastUtils.rayCastEntityHit(RotationUtils.getRotationOrElseMC(), strikeDistance.get(), false) != null && Objects.requireNonNull(RaycastUtils.rayCastEntityHit(RotationUtils.getRotationOrElseMC(), strikeDistance.get(), false)).getEntity() == nearest) {
            Objects.requireNonNull(mc.interactionManager).syncSelectedSlot();
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(nearest, user.isSneaking()));

            if (bypassSprintFlag.get() && user.isSprinting()) {
                user.setVelocity(user.getVelocity().multiply(0.6, 1.0, 0.6));
                user.setSprinting(false);
            }

            if (clientSideSwing.get()) {
                user.swingHand(Hand.MAIN_HAND);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
    }

    private Rotation computeAimRotation(ClientPlayerEntity source, FireballEntity target) {
        Vec3d eyes = source.getPos().add(0, source.getEyeHeight(source.getPose()), 0);
        Vec3d center = target.getPos().add(0, target.getHeight() * 0.5, 0);

        double dx = center.x - eyes.x;
        double dy = center.y - eyes.y;
        double dz = center.z - eyes.z;

        double groundDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, groundDist));

        return new Rotation(MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch));
    }
}