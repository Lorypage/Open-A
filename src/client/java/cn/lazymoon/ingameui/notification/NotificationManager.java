package cn.lazymoon.ingameui.notification;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.render.PostProcessing;
import cn.lazymoon.sxmurxy.builders.Builder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.BlurTaskInstance;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-30
 */
public class NotificationManager {
    @Getter
    private static final Deque<Notification> notifications = new ConcurrentLinkedDeque<>();
    @Getter
    private static final float toggleTime = 2;

    @SneakyThrows
    public static void post(NotificationType type,String name, String description) {
        post(new Notification(type,name, description));
    }

    @SneakyThrows
    public static void post(NotificationType type,String name, String description, float time) {
        post(new Notification(type, name,description, time));
    }

    @SneakyThrows
    public static void post(Notification notification) {
        if (Client.INSTANCE.getModuleManager().getModule(cn.lazymoon.features.module.impl.render.Notification.class).isState()) {
            notifications.add(notification);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @SneakyThrows
    public static void publish(Window sr, Matrix4f matrix4f) {
        if (ClientUtils.isNull()) return;
        float yOffset = sr.getScaledHeight() - 42 ;
        RenderSystem.getModelViewStack().pushMatrix();
        for (Notification notification : getNotifications()) {
            float width = (float) notification.getWidth();
            float height = (float) notification.getHeight();

            Animation animation = notification.getAnimation();
            animation.setDirection(notification.getTimerUtils().hasTimeElapsed((long) notification.getTime()) ? Direction.BACKWARDS : Direction.FORWARDS);

            if (animation.finished(Direction.BACKWARDS)) {
                getNotifications().remove(notification);
            }

            if (!animation.finished(Direction.BACKWARDS)) {
                int scaledWidth = sr.getScaledWidth();
                float smoothness = 5f;

                if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
                    BuiltBlur blur = Builder.blur()
                            .size(new SizeState(width + smoothness, height + smoothness))
                            .radius(new QuadRadiusState(25))
                            .blurRadius(20)
                            .smoothness(smoothness)
                            .color(QuadColorState.TRANSPARENT)
                            .position(new PositionState((scaledWidth - width - 5f + (width + 5f) * (1 - animation.getOutput())) + smoothness / 2, yOffset + smoothness / 2))
                            .matrix4f(matrix4f)
                            .build();
                    BlurTaskInstance.addTask(blur);
                }

                if (PostProcessing.bloom.get()) {
                    RenderHelper.drawRoundRectBloomApple((float) (scaledWidth - width - 5f + (width + 5f) * (1 - animation.getOutput())), yOffset, width, height, 12.5f, 2, new Color(255, 255, 255, 10));
                }
                RenderHelper.drawAppleRoundedRect((float) (scaledWidth - width - 5f + (width + 5f) * (1 - animation.getOutput())), yOffset, width, height, new Color(255, 255, 255, 40), 12.5f);

                RenderHelper.scissorStart((float) (scaledWidth - width - 5f + (width + 5f) * (1 - animation.getOutput())), yOffset, width, 20);
                RenderHelper.drawAppleRoundedRect((float) (scaledWidth - width - 5f + (width + 5f) * (1 - animation.getOutput())), yOffset, width, height, new Color(255, 255, 255, 40), 12.5f);
                RenderHelper.scissorEnd();

                FontManager.FilledMaterial.drawString(22f, notification.getNotificationType().getName(), (float) (scaledWidth - width - 5f + (width + 5f) * (1f - animation.getOutput())) + 6, 5 + yOffset, notification.getNotificationType().getColor(), false);
                FontManager.semibold.drawString(17, notification.getName(), (float) (scaledWidth - width - 5f + (width + 5f) * (1f - animation.getOutput())) + 22, 7 + yOffset, new Color(255,255,255,255), false);
                FontManager.semibold.drawString(17, notification.getDescription(), (float) (scaledWidth - width - 5f + (width + 5f) * (1f - animation.getOutput())) + 10, 27 + yOffset, new Color(255,255,255,249), false);

                yOffset -= (float) (45 * animation.getOutput());
            }
        }
        RenderSystem.getModelViewStack().popMatrix();
    }
}
