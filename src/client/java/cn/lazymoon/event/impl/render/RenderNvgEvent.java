package cn.lazymoon.event.impl.render;

import cn.lazymoon.event.api.event.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;

public record RenderNvgEvent(DrawContext drawContext, Matrix4f matrix4f, RenderTickCounter renderTickCounter) implements Event {}
