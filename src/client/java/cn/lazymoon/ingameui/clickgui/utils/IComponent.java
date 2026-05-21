package cn.lazymoon.ingameui.clickgui.utils;

import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public interface IComponent extends InstanceAccess {
    default void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    default boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int state) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {return false;}

    default boolean charTyped(char chr, int modifiers) {
        return false;
    }
}
