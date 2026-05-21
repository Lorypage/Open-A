package cn.lazymoon.utils.render;

import cn.lazymoon.nanovg.NanoVG;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.color.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class RenderUtils implements InstanceAccess {
    private static long context = NanoVG.INSTANCE.getContext();

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }

    public static void endBuilding(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);
    }

    public static void drawTextureOnEntity(MatrixStack matrices, int xPos, int yPos, int width, int height, float textureWidth, float textureHeight, Entity entity, Identifier texture, boolean rotate, Color c0, Color c1, Color c2, Color c3, float additionalRotation) {
        Vec3d entPos = entity.getLerpedPos(getTickDelta()).add(0, 1, 0);
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d camPos = camera.getPos();

        double x = entPos.x - camPos.x;
        double y = entPos.y - camPos.y;
        double z = entPos.z - camPos.z;

        Quaternionf cameraRotation = camera.getRotation();

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(cameraRotation);

        if (rotate) {
            matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(Math.sin(System.currentTimeMillis() / 800.0) * 360)));
        }

        if (additionalRotation != 0f) {
            matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(additionalRotation)));
        }

        matrices.scale(0.03f, 0.03f, 0.03f);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        float uMin = 0f / textureWidth;
        float vMin = 0f / textureHeight;
        float uMax = width / textureWidth;
        float vMax = height / textureHeight;

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix4f, xPos, yPos, 0).texture(uMin, vMin).color(c0.getRed(), c0.getGreen(), c0.getBlue(), c0.getAlpha());
        buf.vertex(matrix4f, xPos + width, yPos, 0).texture(uMax, vMin).color(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
        buf.vertex(matrix4f, xPos + width, yPos + height, 0).texture(uMax, vMax).color(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
        buf.vertex(matrix4f, xPos, yPos + height, 0).texture(uMin, vMax).color(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        matrices.pop();
    }

    public static Pair<Vec3d, Boolean> project(Matrix4f modelView, Matrix4f projection, Vec3d vector) {
        Vec3d camPos = vector.subtract(mc.gameRenderer.getCamera().getPos());
        Vector4f vec = new Vector4f((float) camPos.x, (float) camPos.y, (float) camPos.z, 1F);

        vec.mul(modelView);
        vec.mul(projection);

        boolean isVisible = vec.w() > 0.0;

        if (vec.w() != 0) {
            vec.x /= vec.w();
            vec.y /= vec.w();
            vec.z /= vec.w();
        }

        double screenX = (vec.x() * 0.5 + 0.5) * mc.getWindow().getScaledWidth();
        double screenY = (0.5 - vec.y() * 0.5) * mc.getWindow().getScaledHeight();

        Vec3d position = new Vec3d(screenX, screenY, vec.z());

        return new Pair<>(position, isVisible);
    }

    public static void drawBox(MatrixStack matrixStack, Box box, Color color, boolean outline, @Nullable Color outlineColor, boolean cameraTranslate) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        double camX = cameraTranslate ? dispatcher.camera.getPos().x : 0;
        double camY = cameraTranslate ? dispatcher.camera.getPos().y : 0;
        double camZ = cameraTranslate ? dispatcher.camera.getPos().z : 0;

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float minX = (float) (box.minX - camX);
        float minY = (float) (box.minY - camY);
        float minZ = (float) (box.minZ - camZ);
        float maxX = (float) (box.maxX - camX);
        float maxZ = (float) (box.maxZ - camZ);
        float maxY = (float) (box.maxY - camY);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        if (outline && outlineColor != null) {
            drawBoxLine(matrixStack, box, outlineColor, cameraTranslate);
        }
    }

    public static void drawBoxLine(MatrixStack matrixStack, Box box, Color color, boolean cameraTranslate) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        double camX = cameraTranslate ? dispatcher.camera.getPos().x : 0;
        double camY = cameraTranslate ? dispatcher.camera.getPos().y : 0;
        double camZ = cameraTranslate ? dispatcher.camera.getPos().z : 0;

        float minX = (float) (box.minX - camX);
        float minY = (float) (box.minY - camY);
        float minZ = (float) (box.minZ - camZ);
        float maxX = (float) (box.maxX - camX);
        float maxZ = (float) (box.maxZ - camZ);
        float maxY = (float) (box.maxY - camY);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    public static float animate(float end, float start, float multiple) {
        return (1 - clamp_float((float) (deltaTime() * multiple), 0, 1)) * end + clamp_float((float) (deltaTime() * multiple), 0, 1) * start;
    }

    public static double clamp_double(double num, double min, double max)
    {
        return num < min ? min : (num > max ? max : num);
    }

    public static double deltaTime() {
        return MinecraftClient.getInstance().getCurrentFps() > 0 ? (1.0000 / MinecraftClient.getInstance().getCurrentFps()) : 1;
    }

    public static float clamp_float(float num, float min, float max)
    {
        return num < min ? min : (num > max ? max : num);
    }

    public static void renderItemAtFloatPos(DrawContext context, ItemStack stack, float x, float y) {
        if (stack.isEmpty()) return;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableDepthTest();
        context.drawItem(stack, 0, 0);
        RenderSystem.enableDepthTest();
        context.getMatrices().pop();
    }

    public static void strokeColor(Color color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f)
                    .g(color.getGreen() / 255f)
                    .b(color.getBlue() / 255f)
                    .a(color.getAlpha() / 255f);
            nvgStrokeColor(context, nvgColor);
        }
    }

    public static void fillColor(Color color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.getRed() / 255f)
                    .g(color.getGreen() / 255f)
                    .b(color.getBlue() / 255f)
                    .a(color.getAlpha() / 255f);
            nvgFillColor(context, nvgColor);
        }
    }

    public static NVGPaint createGradient(float startX, float startY, float endX, float endY, Color colorLeft, Color colorRight) {
        NVGPaint paint = NVGPaint.calloc();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor left = NVGColor.malloc(stack);
            NVGColor right = NVGColor.malloc(stack);

            left.r(colorLeft.getRed() / 255f)
                    .g(colorLeft.getGreen() / 255f)
                    .b(colorLeft.getBlue() / 255f)
                    .a(colorLeft.getAlpha() / 255f);

            right.r(colorRight.getRed() / 255f)
                    .g(colorRight.getGreen() / 255f)
                    .b(colorRight.getBlue() / 255f)
                    .a(colorRight.getAlpha() / 255f);

            nvgLinearGradient(context, startX, startY, endX, endY, left, right, paint);
        }
        return paint;
    }

    public static NVGColor getColor(Color color) {
        NVGColor nvgColor = NVGColor.calloc();
        nvgColor.r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);

        return nvgColor;
    }

    public static boolean isHovering(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public static int colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed) {
        return colorSwitch(firstColor, secondColor, time, index, timePerIndex, speed, 255);
    }

    public static int getRainbow(long currentMillis, int speed, int offset) {
        return getRainbow(currentMillis, speed, offset, 1.0F);
    }

    public static int getRainbow(long currentMillis, int speed, int offset, float alpha) {
        int rainbow = Color.HSBtoRGB(1.0F - ((currentMillis + (offset * 100)) % speed) / (float) speed, 0.9F, 0.9F);
        int r = (rainbow >> 16) & 0xFF;
        int g = (rainbow >> 8) & 0xFF;
        int b = rainbow & 0xFF;
        int a = (int) (alpha * 255.0F);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed, double alpha) {
        long now = (long) (speed * System.currentTimeMillis() + index * timePerIndex);
        float redDiff = (firstColor.getRed() - secondColor.getRed()) / time;
        float greenDiff = (firstColor.getGreen() - secondColor.getGreen()) / time;
        float blueDiff = (firstColor.getBlue() - secondColor.getBlue()) / time;
        int red = Math.round(secondColor.getRed() + redDiff * (now % (long) time));
        int green = Math.round(secondColor.getGreen() + greenDiff * (now % (long) time));
        int blue = Math.round(secondColor.getBlue() + blueDiff * (now % (long) time));
        float redInverseDiff = (secondColor.getRed() - firstColor.getRed()) / time;
        float greenInverseDiff = (secondColor.getGreen() - firstColor.getGreen()) / time;
        float blueInverseDiff = (secondColor.getBlue() - firstColor.getBlue()) / time;
        int inverseRed = Math.round(firstColor.getRed() + redInverseDiff * (now % (long) time));
        int inverseGreen = Math.round(firstColor.getGreen() + greenInverseDiff * (now % (long) time));
        int inverseBlue = Math.round(firstColor.getBlue() + blueInverseDiff * (now % (long) time));
        if (now % ((long) time * 2) < (long) time)
            return ColorUtils.getColor(inverseRed, inverseGreen, inverseBlue, (int) alpha);
        else return ColorUtils.getColor(red, green, blue, (int) alpha);
    }
}
