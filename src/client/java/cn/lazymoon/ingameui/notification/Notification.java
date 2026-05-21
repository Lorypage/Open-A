package cn.lazymoon.ingameui.notification;

import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.impl.EaseOutSine;
import cn.lazymoon.utils.render.font.FontManager;
import cn.lazymoon.utils.time.TimerUtils;
import lombok.Getter;
import lombok.SneakyThrows;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-30
 */
@Getter
public class Notification {
    private final NotificationType notificationType;
    private final String name;
    private final String description;
    private final float time;
    private final TimerUtils timerUtils;
    private final Animation animation;

    public Notification(NotificationType type,String name, String description) {
        this(type,name, description, NotificationManager.getToggleTime());
    }

    public Notification(NotificationType type,String name, String description, float time) {
        this.name = name;
        this.description = description;
        this.time = (long) (time * 1000);
        timerUtils = new TimerUtils();
        this.notificationType = type;
        this.animation = new EaseOutSine(250, 1);
    }

    @SneakyThrows
    public double getWidth() {
        return FontManager.semibold.getStringWidth(getDescription(), 17) + 22;
    }

    public double getHeight() {
        return 42;
    }
}
