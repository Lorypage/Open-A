package cn.lazymoon.ingameui.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;

@Getter
@AllArgsConstructor
public enum NotificationType {
    SUCCESS("P", new Color(65, 252, 65, 200)),
    NOTIFY("A", new Color(127, 174, 210, 200)),
    WARRING("B", new Color(255, 255, 94, 200)),
    FAILED("C", new Color(226, 87, 76, 200)),
    AUTO_PLAY("D", new Color(221, 72, 30, 200));
    private final String name;
    private final Color color;
}