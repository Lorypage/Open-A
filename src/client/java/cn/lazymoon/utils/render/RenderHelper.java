package cn.lazymoon.utils.render;

import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.color.ColorPanel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.awt.*;

import static cn.lazymoon.utils.render.RenderUtils.getColor;
import static org.lwjgl.nanovg.NanoVG.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class RenderHelper implements InstanceAccess {
    public static long context = cn.lazymoon.nanovg.NanoVG.INSTANCE.getContext();
    private static final float BezierCurve = 0.25f;

    public static void scaleStart(float centerX, float centerY, float scale) {
        nvgSave(context);
        nvgTranslate(context, centerX, centerY);
        nvgScale(context, scale, scale);
        nvgTranslate(context, -centerX, -centerY);
    }

    public static void drawGradientRRect3(float x, float y, float w, float h, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        drawGradientRect3(x, y, w, h, radius, bottomLeft, topLeft, bottomRight, topRight);
    }

    private static Color interpolateColor(Color c1, Color c2, float t) {
        t = Math.max(0, Math.min(1, t));

        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * t);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        a = Math.max(0, Math.min(255, a));

        return new Color(r, g, b, a);
    }

    public static void drawGradientRect3(float x, float y, float w, float h, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        long vg = context;

        int strips = 40;
        float stripHeight = h / strips;

        for (int i = 0; i <= strips; i++) {
            float ty = (float) i / strips;
            float stripY = y + i * stripHeight;

            Color leftColor = interpolateColor(topLeft, bottomLeft, ty);
            Color rightColor = interpolateColor(topRight, bottomRight, ty);

            NVGPaint paint = NVGPaint.create();
            NVGColor color1 = nvgColor(leftColor);
            NVGColor color2 = nvgColor(rightColor);

            nvgLinearGradient(vg, x, stripY, x + w, stripY, color1, color2, paint);

            nvgBeginPath(vg);

            if (radius > 0) {
                if (i == 0) {
                    nvgRoundedRectVarying(vg, x, stripY, w, stripHeight + 0.5f, radius, radius, 0, 0);
                } else if (i == strips) {
                    nvgRoundedRectVarying(vg, x, stripY - 0.5f, w, stripHeight + 0.5f, 0, 0, radius, radius);
                } else {
                    nvgRect(vg, x, stripY - 0.5f, w, stripHeight + 1.0f);
                }
            } else {
                nvgRect(vg, x, stripY - 0.5f, w, stripHeight + 1.0f);
            }

            nvgFillPaint(vg, paint);
            nvgFill(vg);
        }
    }

    public static void scale(long vg, float x, float y) {
        nvgScale(vg, x, y);
    }

    public static void translate(float x, float y) {
        nvgTranslate(context, x, y);
    }

    public static void translate(long vg, float x, float y) {
        nvgTranslate(vg, x, y);
    }

    public static void globalAlpha(long vg, float alpha) {
        nvgGlobalAlpha(vg, alpha);
    }

    public static void scaleEnd() {
        nvgRestore(context);
    }

    public static void scissorStart(float x, float y, float width, float height) {
        nvgSave(context);
        nvgScissor(context, x, y, width, height);
    }

    public static void scissorEnd() {
        nvgRestore(context);
    }

    public static void drawImage(int image, float x, float y, float width, float height, float alpha) {
        long vg = context;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGPaint paint = NVGPaint.malloc(stack);
            nvgImagePattern(vg, x, y, width, height, 0, image, alpha, paint);
            nvgBeginPath(vg);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        }
    }
    public static void drawRoundImage(int image, float x, float y, float width, float height, float alpha,float radius){
        long vg = context;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGPaint paint = NVGPaint.malloc(stack);
            nvgImagePattern(vg, x, y, width, height, 0, image, alpha, paint);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x, y, width, height,radius);
            nvgFillPaint(vg, paint);
            nvgFill(vg);
        }
    }

    public static void beginRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        int fbWidth = mc.getWindow().getFramebufferWidth();
        int fbHeight = mc.getWindow().getFramebufferHeight();
        float scaleFactor = (float) mc.getWindow().getScaleFactor();

        nvgBeginFrame(context, fbWidth, fbHeight, 1f);
        nvgSave(context);
        nvgScale(context, scaleFactor, scaleFactor);
    }

    public static void endRender() {
        nvgRestore(context);
        nvgEndFrame(context);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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

    public static void drawRoundRectBloom(float x, float y, float w, float h, Color color, float radius) {
        drawRoundRectBloom(x, y, w, h, radius, 5.0f, color);
    }

    public static void drawRoundRectBloom(float x, float y, float w, float h, float radius, float glowRadius, Color color) {
        long vg = context;

        float baseAlpha = color.getAlpha() / 255.0f;
        int glowSteps = (int) Math.max(10, glowRadius * 2);

        nvgSave(vg);
        for (int i = 0; i < glowSteps; i++) {
            float progress = (float) i / glowSteps;
            float offset = (glowRadius * progress);

            float alphaFactor = (float) Math.cos(progress * Math.PI / 2);
            float currentAlpha = baseAlpha * alphaFactor * 0.3f;

            if (currentAlpha <= 0.005f) continue;

            Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (currentAlpha * 255));
            NVGColor nvgGlowColor = nvgColor(glowColor);

            nvgBeginPath(vg);
            nvgRoundedRect(vg, x - offset, y - offset, w + offset * 2, h + offset * 2, radius + offset);
            nvgFillColor(vg, nvgGlowColor);
            nvgFill(vg);
        }
        nvgRestore(vg);
    }

    public static void drawRoundRectBloomApple(float x, float y, float w, float h, Color color, float radius) {
        drawRoundRectBloomApple(x, y, w, h, radius, 5.0f, color);
    }

    public static void drawRoundRectBloomApple(float x, float y, float w, float h, float radius, float glowRadius, Color color) {
        long vg = context;

        float baseAlpha = color.getAlpha() / 255.0f;
        int glowSteps = (int) Math.max(10, glowRadius * 2);

        nvgSave(vg);
        for (int i = 0; i < glowSteps; i++) {
            float progress = (float) i / glowSteps;
            float offset = (glowRadius * progress);

            float alphaFactor = (float) Math.cos(progress * Math.PI / 2);
            float currentAlpha = baseAlpha * alphaFactor * 0.3f;

            if (currentAlpha <= 0.005f) continue;

            Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (currentAlpha * 255));
            NVGColor nvgGlowColor = nvgColor(glowColor);

            nvgBeginPath(vg);
            drawAppleRoundedRect(x - offset, y - offset, w + offset * 2, h + offset * 2, new Color(0,0,0,0),radius + offset);
            nvgFillColor(vg, nvgGlowColor);
            nvgFill(vg);
        }
        nvgRestore(vg);
    }

    public static NVGColor nvgColor(Color color) {
        NVGColor nvgColor = NVGColor.create();
        nvgRGBA((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha(), nvgColor);
        return nvgColor;
    }

    public static void drawAppleRoundedRect(float x, float y, float width, float height, ColorPanel color, float radius) {
        radius = Math.max(0f, Math.min(radius, height / 2f));

        float c = radius * BezierCurve;

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgMoveTo(context, x + radius, y);

        NanoVG.nvgLineTo(context, x + width - radius, y);
        NanoVG.nvgBezierTo(context, x + width - c, y, x + width, y + c, x + width, y + radius);

        NanoVG.nvgLineTo(context, x + width, y + height - radius);
        NanoVG.nvgBezierTo(context, x + width, y + height - c, x + width - c, y + height, x + width - radius, y + height);

        NanoVG.nvgLineTo(context, x + radius, y + height);
        NanoVG.nvgBezierTo(context, x + c, y + height, x, y + height - c, x, y + height - radius);

        NanoVG.nvgLineTo(context, x, y + radius);
        NanoVG.nvgBezierTo(context, x, y + c, x + c, y, x + radius, y);

        RenderHelper.fillColor(color);
        NanoVG.nvgFill(context);
    }

    public static void fillColor(ColorPanel color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgColor.r(color.red).g(color.green).b(color.blue).a(color.alpha);
            nvgFillColor(context, nvgColor);
        }
    }

    public static void drawGradientAppleRoundedRect(
            float x, float y, float width, float height, float radius,
            Color topLeft, Color topRight, Color bottomRight, Color bottomLeft
    ) {
        radius = Math.max(0f, Math.min(radius, height / 2f));

        long vg = context;

        // 分片不要太多，否则每帧会很重
        int strips = 12;
        float stripHeight = height / strips;

        float c = radius * BezierCurve;

        for (int i = 0; i < strips; i++) {
            float t0 = (float) i / strips;
            float t1 = (float) (i + 1) / strips;

            float stripY = y + i * stripHeight;
            float currentH = stripHeight + 1.0f;

            Color leftColor = interpolateColor(topLeft, bottomLeft, (t0 + t1) * 0.5f);
            Color rightColor = interpolateColor(topRight, bottomRight, (t0 + t1) * 0.5f);

            NVGColor nvgLeft = getColor(leftColor);
            NVGColor nvgRight = getColor(rightColor);

            NVGPaint paint = NVGPaint.calloc();
            nvgLinearGradient(vg, x, stripY, x + width, stripY, nvgLeft, nvgRight, paint);

            nvgBeginPath(vg);

            if (i == 0) {
                // 顶部条：保留上面两个圆角
                nvgMoveTo(vg, x + radius, y);
                nvgLineTo(vg, x + width - radius, y);
                nvgBezierTo(vg, x + width - c, y, x + width, y + c, x + width, y + radius);
                nvgLineTo(vg, x + width, stripY + currentH);
                nvgLineTo(vg, x, stripY + currentH);
                nvgLineTo(vg, x, y + radius);
                nvgBezierTo(vg, x, y + c, x + c, y, x + radius, y);
            } else if (i == strips - 1) {
                // 底部条：保留下面两个圆角
                nvgMoveTo(vg, x, stripY);
                nvgLineTo(vg, x + width, stripY);
                nvgLineTo(vg, x + width, y + height - radius);
                nvgBezierTo(vg, x + width, y + height - c, x + width - c, y + height, x + width - radius, y + height);
                nvgLineTo(vg, x + radius, y + height);
                nvgBezierTo(vg, x + c, y + height, x, y + height - c, x, y + height - radius);
                nvgLineTo(vg, x, stripY);
            } else {
                // 中间条：矩形即可
                nvgRect(vg, x, stripY - 0.5f, width, currentH);
            }

            nvgFillPaint(vg, paint);
            nvgFill(vg);

            nvgLeft.free();
            nvgRight.free();
            paint.free();
        }
    }

    public static void drawAppleRoundedRect(float x, float y, float width, float height, Color color, float radius) {
        radius = Math.max(0f, Math.min(radius, height / 2f));

        float c = radius * BezierCurve;

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgMoveTo(context, x + radius, y);

        NanoVG.nvgLineTo(context, x + width - radius, y);
        NanoVG.nvgBezierTo(context, x + width - c, y, x + width, y + c, x + width, y + radius);

        NanoVG.nvgLineTo(context, x + width, y + height - radius);
        NanoVG.nvgBezierTo(context, x + width, y + height - c, x + width - c, y + height, x + width - radius, y + height);

        NanoVG.nvgLineTo(context, x + radius, y + height);
        NanoVG.nvgBezierTo(context, x + c, y + height, x, y + height - c, x, y + height - radius);

        NanoVG.nvgLineTo(context, x, y + radius);
        NanoVG.nvgBezierTo(context, x, y + c, x + c, y, x + radius, y);

        RenderUtils.fillColor(color);
        NanoVG.nvgFill(context);
    }

    public static void endBuilding(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);
    }

    public static Color injectAlpha(final Color color, final int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
    }

    public static Vec3d interpolatePos(float prevposX, float prevposY, float prevposZ, float posX, float posY, float posZ) {
        double x = prevposX + ((posX - prevposX) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = prevposY + ((posY - prevposY) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = prevposZ + ((posZ - prevposZ) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        return new Vec3d(x, y, z);
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickDelta(true);
    }

    public static void drawImageNative(MatrixStack matrices, float x, float y, float width, float height, Identifier texture) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, texture);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        bufferBuilder.vertex(matrix, x, y + height, 0).texture(0, 1);
        bufferBuilder.vertex(matrix, x + width, y + height, 0).texture(1, 1);
        bufferBuilder.vertex(matrix, x + width, y, 0).texture(1, 0);
        bufferBuilder.vertex(matrix, x, y, 0).texture(0, 0);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
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


    public static void renderScaledItem(DrawContext context, ItemStack stack, float x, float y, float scale) {
        if (stack.isEmpty()) return;

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, scale);

        float scaledX = x / scale + 0.65f;
        float scaledY = y / scale;

        RenderUtils.renderItemAtFloatPos(context, stack, scaledX, scaledY);
        context.getMatrices().pop();
    }

    public static void drawTextureOnEntity(MatrixStack matrices, int xPos, int yPos, int width, int height, float textureWidth, float textureHeight, Entity entity, Identifier texture, boolean rotate, Color c0, Color c1, Color c2, Color c3) {
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

    public static void drawCircleOutline(BlockPos pos, double radius, int color) {
        if (mc.world == null || mc.player == null) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double x = pos.getX() + 0.5 - cameraPos.x;
        double y = pos.getY() + 0.01 - cameraPos.y;
        double z = pos.getZ() + 0.5 - cameraPos.z;

        float a = (color >> 24 & 255) / 255.0f;
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(x, y, z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);

        int points = 100;
        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            buffer.vertex(matrix, (float) dx, 0f, (float) dz).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();
    }

    public static void drawBox(BufferBuilder bufferBuilder, Box box, Matrix4f m, Color c, boolean fix) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());

        if (!fix) {
            minX = (float) box.minX;
            minY = (float) box.minY;
            minZ = (float) box.minZ;
            maxX = (float) box.maxX;
            maxY = (float) box.maxY;
            maxZ = (float) box.maxZ;
        }

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());

        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
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

    public static void drawBox(MatrixStack matrixStack, double x, double y, double z, double width, double height, double depth, Color color, boolean outline, @Nullable Color outlineColor, boolean cameraTranslate) {
        Box box = new Box(x, y, z, x + width, y + height, z + depth);
        drawBox(matrixStack, box, color, outline, outlineColor, cameraTranslate);
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


    public static void drawGradientAppleRoundedRectUD(float x, float y, float width, float height, Color colorLeft, Color colorRight, float radius) {
        radius = Math.max(0f, Math.min(radius, height / 2f));

        float c = radius * BezierCurve;

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgMoveTo(context, x + radius, y);

        NanoVG.nvgLineTo(context, x + width - radius, y);
        NanoVG.nvgBezierTo(context, x + width - c, y, x + width, y + c, x + width, y + radius);

        NanoVG.nvgLineTo(context, x + width, y + height - radius);
        NanoVG.nvgBezierTo(context, x + width, y + height - c, x + width - c, y + height, x + width - radius, y + height);

        NanoVG.nvgLineTo(context, x + radius, y + height);
        NanoVG.nvgBezierTo(context, x + c, y + height, x, y + height - c, x, y + height - radius);

        NanoVG.nvgLineTo(context, x, y + radius);
        NanoVG.nvgBezierTo(context, x, y + c, x + c, y, x + radius, y);

        NVGPaint gradient = RenderUtils.createGradient(x, y, x, y + height, colorLeft, colorRight);

        nvgFillPaint(context, gradient);
        NanoVG.nvgFill(context);
    }

    public static void drawGradientAppleRoundedRectLRBorderOnly(
            float x, float y, float width, float height,
            float radius,
            Color borderLeft, Color borderRight,
            float lineWidth
    ) {
        radius = Math.max(0f, Math.min(radius, height / 2f));
        float c = radius * BezierCurve;

        // 先构建 Apple 圆角路径
        NanoVG.nvgBeginPath(context);

        NanoVG.nvgMoveTo(context, x + radius, y);

        NanoVG.nvgLineTo(context, x + width - radius, y);
        NanoVG.nvgBezierTo(context, x + width - c, y, x + width, y + c, x + width, y + radius);

        NanoVG.nvgLineTo(context, x + width, y + height - radius);
        NanoVG.nvgBezierTo(context, x + width, y + height - c, x + width - c, y + height, x + width - radius, y + height);

        NanoVG.nvgLineTo(context, x + radius, y + height);
        NanoVG.nvgBezierTo(context, x + c, y + height, x, y + height - c, x, y + height - radius);

        NanoVG.nvgLineTo(context, x, y + radius);
        NanoVG.nvgBezierTo(context, x, y + c, x + c, y, x + radius, y);

        // 做左 -> 右 渐变边框
        NVGColor left = RenderUtils.getColor(borderLeft);
        NVGColor right = RenderUtils.getColor(borderRight);

        NVGPaint paint = NVGPaint.calloc();
        nvgLinearGradient(context, x, y, x + width, y, left, right, paint);

        nvgStrokePaint(context, paint);
        nvgStrokeWidth(context, lineWidth);
        nvgStroke(context);

        left.free();
        right.free();
        paint.free();
    }

    public static void drawGradientAppleRoundedRectFourCornerBorderOnly(
            float x, float y, float width, float height,
            float radius,
            Color topLeft, Color topRight, Color bottomRight, Color bottomLeft,
            float lineWidth
    ) {
        radius = Math.max(0f, Math.min(radius, height / 2f));
        float c = radius * BezierCurve;

        // =========================
        // 上边框：左上 -> 右上
        // =========================
        NanoVG.nvgSave(context);
        NanoVG.nvgScissor(context, x - lineWidth, y - lineWidth, width + lineWidth * 2f, radius + lineWidth * 2f);

        NanoVG.nvgBeginPath(context);
        buildAppleRoundedRectPath(x, y, width, height, radius);

        NVGColor tl = RenderUtils.getColor(topLeft);
        NVGColor tr = RenderUtils.getColor(topRight);

        NVGPaint topPaint = NVGPaint.calloc();
        NanoVG.nvgLinearGradient(context, x, y, x + width, y, tl, tr, topPaint);
        NanoVG.nvgStrokePaint(context, topPaint);
        NanoVG.nvgStrokeWidth(context, lineWidth);
        NanoVG.nvgStroke(context);

        tl.free();
        tr.free();
        topPaint.free();
        NanoVG.nvgRestore(context);

        // =========================
        // 右边框：右上 -> 右下
        // =========================
        NanoVG.nvgSave(context);
        NanoVG.nvgScissor(context, x + width - radius - lineWidth * 2f, y - lineWidth, radius + lineWidth * 2f, height + lineWidth * 2f);

        NanoVG.nvgBeginPath(context);
        buildAppleRoundedRectPath(x, y, width, height, radius);

        NVGColor tr2 = RenderUtils.getColor(topRight);
        NVGColor br = RenderUtils.getColor(bottomRight);

        NVGPaint rightPaint = NVGPaint.calloc();
        NanoVG.nvgLinearGradient(context, x + width, y, x + width, y + height, tr2, br, rightPaint);
        NanoVG.nvgStrokePaint(context, rightPaint);
        NanoVG.nvgStrokeWidth(context, lineWidth);
        NanoVG.nvgStroke(context);

        tr2.free();
        br.free();
        rightPaint.free();
        NanoVG.nvgRestore(context);

        // =========================
        // 下边框：右下 -> 左下
        // =========================
        NanoVG.nvgSave(context);
        NanoVG.nvgScissor(context, x - lineWidth, y + height - radius - lineWidth * 2f, width + lineWidth * 2f, radius + lineWidth * 2f);

        NanoVG.nvgBeginPath(context);
        buildAppleRoundedRectPath(x, y, width, height, radius);

        NVGColor br2 = RenderUtils.getColor(bottomRight);
        NVGColor bl = RenderUtils.getColor(bottomLeft);

        NVGPaint bottomPaint = NVGPaint.calloc();
        NanoVG.nvgLinearGradient(context, x + width, y + height, x, y + height, br2, bl, bottomPaint);
        NanoVG.nvgStrokePaint(context, bottomPaint);
        NanoVG.nvgStrokeWidth(context, lineWidth);
        NanoVG.nvgStroke(context);

        br2.free();
        bl.free();
        bottomPaint.free();
        NanoVG.nvgRestore(context);

        // =========================
        // 左边框：左下 -> 左上
        // =========================
        NanoVG.nvgSave(context);
        NanoVG.nvgScissor(context, x - lineWidth, y - lineWidth, radius + lineWidth * 2f, height + lineWidth * 2f);

        NanoVG.nvgBeginPath(context);
        buildAppleRoundedRectPath(x, y, width, height, radius);

        NVGColor bl2 = RenderUtils.getColor(bottomLeft);
        NVGColor tl2 = RenderUtils.getColor(topLeft);

        NVGPaint leftPaint = NVGPaint.calloc();
        NanoVG.nvgLinearGradient(context, x, y + height, x, y, bl2, tl2, leftPaint);
        NanoVG.nvgStrokePaint(context, leftPaint);
        NanoVG.nvgStrokeWidth(context, lineWidth);
        NanoVG.nvgStroke(context);

        bl2.free();
        tl2.free();
        leftPaint.free();
        NanoVG.nvgRestore(context);
    }
    private static float clampAppleRadius(float width, float height, float radius) {
        return Math.max(0f, Math.min(radius, Math.min(width, height) / 2f));
    }
    private static void buildAppleRoundedRectPath(float x, float y, float width, float height, float radius) {
        radius = clampAppleRadius(width, height, radius);
        float c = radius * BezierCurve;

        NanoVG.nvgMoveTo(context, x + radius, y);

        NanoVG.nvgLineTo(context, x + width - radius, y);
        NanoVG.nvgBezierTo(context, x + width - c, y, x + width, y + c, x + width, y + radius);

        NanoVG.nvgLineTo(context, x + width, y + height - radius);
        NanoVG.nvgBezierTo(context, x + width, y + height - c, x + width - c, y + height, x + width - radius, y + height);

        NanoVG.nvgLineTo(context, x + radius, y + height);
        NanoVG.nvgBezierTo(context, x + c, y + height, x, y + height - c, x, y + height - radius);

        NanoVG.nvgLineTo(context, x, y + radius);
        NanoVG.nvgBezierTo(context, x, y + c, x + c, y, x + radius, y);
    }

    public static void drawGradientAppleRoundedRectLR(float x, float y, float width, float height, Color colorLeft, Color colorRight, float radius) {
        radius = Math.max(0f, Math.min(radius, height / 2f));

        float c = radius * BezierCurve;

        NanoVG.nvgBeginPath(context);

        NanoVG.nvgMoveTo(context, x + radius, y);

        NanoVG.nvgLineTo(context, x + width - radius, y);
        NanoVG.nvgBezierTo(context, x + width - c, y, x + width, y + c, x + width, y + radius);

        NanoVG.nvgLineTo(context, x + width, y + height - radius);
        NanoVG.nvgBezierTo(context, x + width, y + height - c, x + width - c, y + height, x + width - radius, y + height);

        NanoVG.nvgLineTo(context, x + radius, y + height);
        NanoVG.nvgBezierTo(context, x + c, y + height, x, y + height - c, x, y + height - radius);

        NanoVG.nvgLineTo(context, x, y + radius);
        NanoVG.nvgBezierTo(context, x, y + c, x + c, y, x + radius, y);

        NVGPaint gradient = RenderUtils.createGradient(x, y, x + width, y + height, colorLeft, colorRight);

        nvgFillPaint(context, gradient);
        NanoVG.nvgFill(context);
    }

    public static void drawGradientAppleRoundedRectFourCorner(
            float x, float y, float width, float height, float radius,
            Color topLeft, Color topRight, Color bottomRight, Color bottomLeft
    ) {
        radius = Math.max(0f, Math.min(radius, height / 2f));

        // 分片数，越大越平滑，但也越耗性能
        int strips = 12;
        float stripHeight = height / strips;

        for (int i = 0; i < strips; i++) {
            float sy = y + i * stripHeight;
            float ey = (i == strips - 1) ? (y + height) : (sy + stripHeight);

            float t = ((sy + ey) * 0.5f - y) / height;
            t = Math.max(0f, Math.min(1f, t));

            // 左右两边颜色分别按上下插值
            Color leftColor = lerpColor(topLeft, bottomLeft, t);
            Color rightColor = lerpColor(topRight, bottomRight, t);

            NVGColor nvgLeft = RenderUtils.getColor(leftColor);
            NVGColor nvgRight = RenderUtils.getColor(rightColor);

            NVGPaint paint = NVGPaint.calloc();
            NanoVG.nvgLinearGradient(context, x, sy, x + width, sy, nvgLeft, nvgRight, paint);

            NanoVG.nvgBeginPath(context);

            // 计算当前横条在圆角区域的左右收缩量
            float leftInsetTop = getRoundedInset(sy, y, height, radius);
            float leftInsetBottom = getRoundedInset(ey, y, height, radius);

            float leftTopX = x + leftInsetTop;
            float rightTopX = x + width - leftInsetTop;
            float leftBottomX = x + leftInsetBottom;
            float rightBottomX = x + width - leftInsetBottom;

            NanoVG.nvgMoveTo(context, leftTopX, sy);
            NanoVG.nvgLineTo(context, rightTopX, sy);
            NanoVG.nvgLineTo(context, rightBottomX, ey);
            NanoVG.nvgLineTo(context, leftBottomX, ey);
            NanoVG.nvgClosePath(context);

            NanoVG.nvgFillPaint(context, paint);
            NanoVG.nvgFill(context);

            nvgLeft.free();
            nvgRight.free();
            paint.free();
        }
    }

    private static float getRoundedInset(float yy, float y, float height, float radius) {
        if (radius <= 0f) {
            return 0f;
        }

        // 上圆角
        if (yy < y + radius) {
            float dy = yy - (y + radius);
            return radius - (float) Math.sqrt(Math.max(0f, radius * radius - dy * dy));
        }

        // 下圆角
        if (yy > y + height - radius) {
            float dy = yy - (y + height - radius);
            return radius - (float) Math.sqrt(Math.max(0f, radius * radius - dy * dy));
        }

        return 0f;
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));

        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);

        return new Color(r, g, bl, al);
    }

    public static void drawRect(float x, float y, float width, float height, Color color) {
        nvgBeginPath(context);
        nvgRect(context, x, y, width, height);
        RenderUtils.fillColor(color);
        nvgFill(context);
    }

    public static void drawOutlineRect(float x, float y, float width, float height, Color color, float lineWidth) {
        nvgBeginPath(context);
        nvgRect(context, x, y, width, height);
        nvgStrokeColor(context, colorPanelToNVG(color));
        nvgStrokeWidth(context, lineWidth);
        nvgStroke(context);
    }

    public static NVGColor colorPanelToNVG(Color c) {
        NVGColor col = NVGColor.calloc();
        // 假设 ColorPanel 的通道是 0-255
        col.r(c.getRed());
        col.g(c.getGreen());
        col.b(c.getBlue());
        col.a(c.getAlpha());
        return col;
    }

    public static void drawGradientRect(float x, float y, float width, float height, Color colorLeft, Color colorRight) {
        NVGPaint gradient = RenderUtils.createGradient(x, y, x + width, y + height, colorLeft, colorRight);
        nvgBeginPath(context);
        nvgRect(context, x, y, width, height);
        nvgFillPaint(context, gradient);
        nvgFill(context);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, ColorPanel color, float radius) {
        nvgBeginPath(context);
        nvgRoundedRect(context, x, y, width, height, radius);
        RenderHelper.fillColor(color);
        nvgFill(context);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, Color color, float radius) {
        nvgBeginPath(context);
        nvgRoundedRect(context, x, y, width, height, radius);
        RenderUtils.fillColor(color);
        nvgFill(context);
    }

    public static void drawGradientRoundedRectLR(float x, float y, float width, float height, Color colorLeft, Color colorRight, float radius) {
        NVGPaint gradient = RenderUtils.createGradient(x, y, x + width, y + height, colorLeft, colorRight);
        nvgBeginPath(context);
        nvgRoundedRect(context, x, y, width, height, radius);
        nvgFillPaint(context, gradient);
        nvgFill(context);
    }

    public static void drawGradientRoundedRectUD(float x, float y, float width, float height, Color colorLeft, Color colorRight, float radius) {
        NVGPaint gradient = RenderUtils.createGradient(x, y, x, y + height, colorLeft, colorRight);
        nvgBeginPath(context);
        nvgRoundedRect(context, x, y, width, height, radius);
        nvgFillPaint(context, gradient);
        nvgFill(context);
    }

    public static void drawRoundedRectCornerLR(float x, float y, float width, float height, Color color, float a, float b, float c, float d) {
        nvgBeginPath(context);
        nvgRoundedRectVarying(context, x, y, width, height, a, b, c, d);
        RenderUtils.fillColor(color);
        nvgFill(context);
    }

    public static void drawGradientRoundedRectCornerLR(float x, float y, float width, float height, Color colorLeft, Color colorRight, float a, float b, float c, float d) {
        NVGPaint gradient = RenderUtils.createGradient(x, y, x + width, y + height, colorLeft, colorRight);
        nvgBeginPath(context);
        nvgRoundedRectVarying(context, x, y, width, height, a, b, c, d);
        nvgFillPaint(context, gradient);
        nvgFill(context);
    }

    public static void drawGradientRoundedRectCornerUD(float x, float y, float width, float height, Color colorLeft, Color colorRight, float a, float b, float c, float d) {
        NVGPaint gradient = RenderUtils.createGradient(x, y, x, y + height, colorLeft, colorRight);
        nvgBeginPath(context);
        nvgRoundedRectVarying(context, x, y, width, height, a, b, c, d);
        nvgFillPaint(context, gradient);
        nvgFill(context);
    }

    public static void drawShadow(float x, float y, float width, float height, Color color, float strength, float cornerRadius) {
        NVGPaint shadowPaint = NVGPaint.calloc();
        NVGColor nvgColorFirst = NVGColor.calloc();
        NVGColor nvgColorSecond = NVGColor.calloc();
        nvgColorFirst.r(color.getRed()).g(color.getGreen()).b(color.getBlue()).a(color.getAlpha());
        nvgColorSecond.r(color.getRed()).g(color.getGreen()).b(color.getBlue()).a(0f);
        nvgBoxGradient(context, x, y, width, height, cornerRadius, strength, nvgColorFirst, nvgColorSecond, shadowPaint);
        nvgBeginPath(context);
        nvgRoundedRect(context, x - strength, y - strength, width + 2f * strength, height + 2f * strength, cornerRadius);
        nvgRoundedRect(context, x - strength, y - strength, width + 2f * strength, height + 2f * strength, cornerRadius);
        nvgPathWinding(context, NVG_HOLE);
        nvgFillPaint(context, shadowPaint);
        nvgFill(context);
        shadowPaint.free();
    }

    public static void drawCircle(float x, float y, float radius, Color color) {
        nvgBeginPath(context);
        nvgCircle(context, x, y, radius);
        RenderUtils.fillColor(color);
        nvgFill(context);
    }

    public static void drawCircleProgressRing(float x, float y, float radius, float strokeWidth, Color color, float progress) {
        progress = Math.max(0f, Math.min(1f, progress));

        nvgShapeAntiAlias(context, true);

        float startAngle = -90f * (float) Math.PI / 180f;
        float endAngle = startAngle - 2f * (float) Math.PI * progress;

        nvgBeginPath(context);
        nvgArc(context, x, y, radius, startAngle, endAngle, NVG_CCW);
        RenderUtils.strokeColor(color);
        nvgStrokeWidth(context, strokeWidth);
        nvgStroke(context);
    }

    public static void drawCircleOutline(float x, float y, float radius, Color color, float strokeWidth) {
        nvgBeginPath(context);
        nvgCircle(context, x, y, radius);
        RenderUtils.strokeColor(color);
        nvgStrokeWidth(context, strokeWidth);
        nvgStroke(context);
    }

    public static void drawGradientCircle(float x, float y, float radius, Color colorInner, Color colorOuter) {
        NVGPaint gradient = createRadialGradient(x, y, 0f, radius, colorInner, colorOuter);
        nvgBeginPath(context);
        nvgCircle(context, x, y, radius);
        nvgFillPaint(context, gradient);
        nvgFill(context);
    }

    public static NVGPaint createRadialGradient(float cx, float cy, float inr, float outr, Color innerColor, Color outerColor) {
        NVGPaint paint = NVGPaint.calloc();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor icolor = NVGColor.malloc(stack);
            NVGColor ocolor = NVGColor.malloc(stack);

            icolor.r(innerColor.getRed() / 255f)
                    .g(innerColor.getGreen() / 255f)
                    .b(innerColor.getBlue() / 255f)
                    .a(innerColor.getAlpha() / 255f);

            ocolor.r(outerColor.getRed() / 255f)
                    .g(outerColor.getGreen() / 255f)
                    .b(outerColor.getBlue() / 255f)
                    .a(outerColor.getAlpha() / 255f);

            nvgRadialGradient(context, cx, cy, inr, outr, icolor, ocolor, paint);
        }
        return paint;
    }

    public static void drawEllipse(float x, float y, float radiusX, float radiusY, Color color) {
        nvgBeginPath(context);
        nvgEllipse(context, x, y, radiusX, radiusY);
        RenderUtils.fillColor(color);
        nvgFill(context);
    }

    public static void drawEllipseOutline(float x, float y, float radiusX, float radiusY, Color color, float strokeWidth) {
        nvgBeginPath(context);
        nvgEllipse(context, x, y, radiusX, radiusY);
        RenderUtils.strokeColor(color);
        nvgStrokeWidth(context, strokeWidth);
        nvgStroke(context);
    }

    public static void drawArc(float x, float y, float radius, float startAngle, float endAngle, Color color, int direction) {
        nvgBeginPath(context);
        nvgArc(context, x, y, radius, startAngle, endAngle, direction);
        RenderUtils.fillColor(color);
        nvgFill(context);
    }

    public static void drawArcOutline(float x, float y, float radius, float startAngle, float endAngle, Color color, float strokeWidth, int direction) {
        nvgBeginPath(context);
        nvgArc(context, x, y, radius, startAngle, endAngle, direction);
        RenderUtils.strokeColor(color);
        nvgStrokeWidth(context, strokeWidth);
        nvgStroke(context);
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
}
