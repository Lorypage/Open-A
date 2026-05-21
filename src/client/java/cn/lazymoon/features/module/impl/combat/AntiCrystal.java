package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientData;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.entity.RaycastUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(name = "AntiCrystal", description = "Help you attack the crystals within range", key = 0, category = Category.Combat, hidden = false)
public class AntiCrystal extends Module {
    public static NumberValue range = new NumberValue("Range", 4.5, 0, 6,0.01);
    public static NumberValue throughRange = new NumberValue("ThroughWall Range", 2, 0, 6,0.01);

    private final List<EndCrystalEntity> crystalTargets = new ArrayList<>();
    public static EndCrystalEntity currentTarget = null;
    private int currentIndex = 0;

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (ClientUtils.isNull() || mc.getNetworkHandler() == null) return;

        /// 清理不存在或无效的水晶
        Iterator<EndCrystalEntity> iterator = crystalTargets.iterator();
        while (iterator.hasNext()) {
            EndCrystalEntity crystal = iterator.next();
            if (crystal == null || crystal.isRemoved()) {
                iterator.remove();
                continue;
            }

            /// 敌人不在 5格内或自己不在范围内也移除
            if (hasNoEnemyNearby(crystal, mc.world, mc.player) || isNotInValidRange(mc.player, crystal)) {
                iterator.remove();
            }
        }

        /// Search
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (crystal.isRemoved()) continue;
            if (crystalTargets.contains(crystal)) continue;

            if (hasNoEnemyNearby(crystal, mc.world, mc.player)) continue;
            if (isNotInValidRange(mc.player, crystal)) continue;

            crystalTargets.add(crystal);
        }

        if (crystalTargets.isEmpty()) {
            currentTarget = null;
            return;
        }

        /// Switch逻辑
        if (currentIndex >= crystalTargets.size()) currentIndex = 0;
        currentTarget = crystalTargets.get(currentIndex);

        /// 如果当前目标失效则切换
        if (currentTarget == null || currentTarget.isRemoved() || isNotInValidRange(mc.player, currentTarget)) {
            crystalTargets.remove(currentTarget);
            currentIndex++;
            return;
        }

        /// 转头
        Vec3d crystalPos = currentTarget.getPos().add(0, 0.5, 0);
        Rotation rotation = RotationUtils.toRotation(crystalPos,false);
        Client.INSTANCE.getRotationComponent().setRotations(rotation,1, MovementFix.SILENT);

        /// 击打水晶
        EntityHitResult hit = RaycastUtils.rayCastEntityHit(rotation, range.get(), ClientData.getDistanceToEntityBox(mc.player,currentTarget) <= throughRange.get());
        if (hit != null && hit.getEntity() == currentTarget) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(currentTarget, mc.player.isSneaking()));
            mc.player.swingHand(Hand.MAIN_HAND);
            ClientUtils.displayChat("Hit crystal success!");
            currentIndex++;
        }
    }

    /// 可以在这里加一下Teams的判断（布吉岛有多人空岛吗我不知道）
    private boolean hasNoEnemyNearby(EndCrystalEntity crystal, ClientWorld world, ClientPlayerEntity player) {
        for (PlayerEntity target : world.getPlayers()) {
            if (target == player || target.isRemoved() || target.isSpectator()) continue;
            if (target.squaredDistanceTo(crystal) <= 25.0) { /// 5格半径（爆炸范围内，可以考虑弄一下自定义的）
                return false;
            }
        }
        return true;
    }

    private boolean isNotInValidRange(ClientPlayerEntity player, EndCrystalEntity crystal) {
        boolean canSee = player.canSee(crystal);
        double dist = ClientData.getDistanceToEntityBox(mc.player,crystal);
        double maxRange = canSee ? range.get() : throughRange.get();
        return !(dist <= maxRange);
    }
}
