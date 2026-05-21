package cn.lazymoon.event.impl.render;

import cn.lazymoon.event.api.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

@Getter
@AllArgsConstructor
public class Render3DEvent implements Event {
    private final MatrixStack matrix;
    private final RenderTickCounter tickCounter;
    private final float partialTicks;
    public final Matrix4f projectionMatrix;
}