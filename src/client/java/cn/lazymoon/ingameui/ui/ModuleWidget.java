package cn.lazymoon.ingameui.ui;

import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.time.TimerUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-29
 */
@Getter
@Setter
public abstract class ModuleWidget extends Module {
    @Expose
    @SerializedName("x")
    public float x;

    @Expose
    @SerializedName("y")
    public float y;

    protected float renderX, renderY;
    public float width;
    public float height;
    public boolean dragging;
    private int dragX, dragY;
    protected Window window;
    protected DrawContext drawContext;

    TimerUtils timerUtil = new TimerUtils();

    public ModuleWidget() {
        ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
        Objects.requireNonNull(moduleInfo, "ModuleInfo annotation is missing on " + getClass().getName());

        this.name = moduleInfo.name();
        this.description = moduleInfo.description();

        this.key = moduleInfo.key();

        this.category = moduleInfo.category();

        this.hidden = moduleInfo.hidden();

        this.values = new ArrayList<>();

        this.x = 0f;
        this.y = 0f;
        this.width = 0f;
        this.height = 0f;
    }

    public abstract void render(RenderNvgEvent event);

    public abstract boolean shouldRender();

    public void updatePos() {
        window = MinecraftClient.getInstance().getWindow();
        renderX = x * window.getScaledWidth();
        renderY = y * window.getScaledHeight();

        x = renderX / window.getScaledWidth();
        y = renderY / window.getScaledHeight();
    }

    public final void onChatGUI(int mouseX, int mouseY, boolean drag) {
        boolean hovering = RenderUtils.isHovering(renderX, renderY, width, height, mouseX, mouseY);
        boolean isLeftMouseDown = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (hovering && isLeftMouseDown && !dragging && drag) {
            dragging = true;
            dragX = mouseX;
            dragY = mouseY;
        }

        if (!isLeftMouseDown) dragging = false;

        if (dragging) {
            float deltaX = (float) (mouseX - dragX) / window.getScaledWidth();
            float deltaY = (float) (mouseY - dragY) / window.getScaledHeight();

            x += deltaX;
            y += deltaY;

            dragX = mouseX;
            dragY = mouseY;
        }
    }
}
