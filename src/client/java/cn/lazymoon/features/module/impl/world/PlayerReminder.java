package cn.lazymoon.features.module.impl.world;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.exploit.AntiBot;
import cn.lazymoon.features.module.impl.player.Teams;
import cn.lazymoon.ingameui.notification.Notification;
import cn.lazymoon.ingameui.notification.NotificationManager;
import cn.lazymoon.ingameui.notification.NotificationType;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static cn.lazymoon.utils.client.ClientData.getDistanceToEntityBox;

@ModuleInfo(name = "PlayerReminder", description = "Remind you if there are enemies approaching within range",key = 0, category = Category.World,hidden = false)
public class PlayerReminder extends Module {

    private final List<PlayerEntity> targets = new CopyOnWriteArrayList<>();

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (ClientUtils.isNull())
            return;
        targets.removeIf(e -> {
            if (e.isDead()) return true;

            boolean stillExists = mc.world.getEntityById(e.getId()) != null;
            if (!stillExists) return true;

            return getDistanceToEntityBox(mc.player, e) > 10F;
        });

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity != mc.player && !entity.isDead()) {
                if (Teams.isInYourTeam(entity) && Client.INSTANCE.getModuleManager().getModule(Teams.class).isState()) return;
                AntiBot antiBot = Client.INSTANCE.getModuleManager().getModule(AntiBot.class);
                if (antiBot != null && antiBot.isState() && AntiBot.isBot(entity)) return;
                if (entity.getUuid() == null) {
                    return;
                }

                if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) {
                    return;
                }

                if (entity.getDisplayName() == null) {
                    return;
                }

                if (Objects.requireNonNull(entity.getDisplayName()).getString().contains("[NPC]")) {
                    return;
                }
                if (Objects.requireNonNull(entity.getDisplayName()).getString().contains("CIT-")) {
                    return;
                }
                double dist = getDistanceToEntityBox(mc.player, entity);

                if (dist <= 10F) {
                    if (!targets.contains(entity)) {
                        targets.add(entity);
                        NotificationManager.post(new Notification(NotificationType.WARRING,"PlayerReminder", entity.getName().getString() + " Preparing to attack you."));
                    }
                }
            }
        }
    }
}
