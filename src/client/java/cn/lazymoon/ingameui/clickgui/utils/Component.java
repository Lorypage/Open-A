package cn.lazymoon.ingameui.clickgui.utils;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Component implements IComponent {
    private float x, y, width, height;
    protected float scale = 1.0f;

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isHovered(float mouseX, float mouseY, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isVisible() {
        return true;
    }
}
