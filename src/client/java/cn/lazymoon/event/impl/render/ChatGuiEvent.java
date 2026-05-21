package cn.lazymoon.event.impl.render;

import cn.lazymoon.event.api.event.Event;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;

/**
 * @Author：Gu-Yuemang
 * @Date：11/21/2025 8:48 PM
 */
@Getter
public class ChatGuiEvent implements Event {
    private final DrawContext guiGraphics;
    private final int mouseX;
    private final int mouseY;

    public ChatGuiEvent(DrawContext guiGraphics, int mouseX, int mouseY) {
        this.guiGraphics = guiGraphics;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
