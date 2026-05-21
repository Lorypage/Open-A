package cn.lazymoon.ingameui.splash;

import cn.lazymoon.nanovg.gl.States;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

import java.awt.*;

public class ArcaneSplashRenderer {
    private static final long MIN_SHOW_TIME = 5000L;
    private static final long EXIT_ANIM_TIME = 900L;
    private static boolean pendingSwitch = false;
    private static long pendingSwitchTime = -1L;
    private static long startTime = -1L;
    private static long exitStartTime = -1L;

    private static boolean exiting = false;
    private static boolean switched = false;
    private static float smoothProgress = 0f;

    public static void render(DrawContext context, float alpha, MinecraftClient client) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        // 先盖住原版
        context.fill(0, 0, width, height, 0xFF000000);
        // 你的标题
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                "Arcane",
                width / 2,
                height / 2 - 30,
                0xFFFFFF
        );
    }
    public static void renderProgress(DrawContext context, int width, int height, float progress, float alpha) {
        int barWidth = 220;
        int barHeight = 6;
        int x1 = (width - barWidth) / 2;
        int y1 = height / 2 + 10;
        int x2 = x1 + barWidth;
        int y2 = y1 + barHeight;
        context.fill(x1, y1, x2, y2, 0xFF2A2A2A);
        context.fill(x1, y1, x1 + (int) (barWidth * progress), y2, 0xFF7A5CFF);
    }

    public static void render(DrawContext context, float progress) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        RenderHelper.drawRoundedRect(0, 0, width, height,new Color(0, 0, 0, 255),0);
        // 先盖一个纯黑背景，彻底遮住原版 splash

        Window window = mc.getWindow();

        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
        }

        long now = System.currentTimeMillis();
        float elapsed = now - startTime;

        smoothProgress = animate(smoothProgress, progress, 0.08f);

        boolean canStartExit = elapsed >= MIN_SHOW_TIME;

        if (canStartExit && !exiting) {
            exiting = true;
            exitStartTime = now;
        }

        float exitProgress = exiting ? clamp((now - exitStartTime) / (float) EXIT_ANIM_TIME, 0f, 1f) : 0f;
        float exitEase = easeOut(exitProgress);

        // 到时间后只设置标记，不要直接 return
        if (exiting && exitProgress >= 1f && !pendingSwitch && !switched) {
            pendingSwitch = true;
            pendingSwitchTime = now;
        }

        float sw = window.getScaledWidth();
        float sh = window.getScaledHeight();
        float cx = sw / 2f;
        float cy = sh / 2f;

        Color accent = Theme.getCurrentTheme().getColors().first;

        float globalAlpha = 1f - exitEase;
        float globalScale = 1f + 0.035f * exitEase;
        float logoYOffset = -20f * exitEase;

        boolean requestSwitch = pendingSwitch && !switched && now - pendingSwitchTime >= 50L;

        RenderSystem.enableBlend();
        States.push();
        RenderHelper.beginRender();

        try {
            // 删掉这句，别污染全局 alpha

            RenderHelper.scaleStart(cx, cy, globalScale);

            drawBackground(sw, sh, elapsed, accent, globalAlpha);
            drawDecorations(sw, sh, elapsed, accent, globalAlpha);
            drawFloatingParticles(cx, cy, elapsed, accent, globalAlpha);
            drawMainLogo(cx, cy + logoYOffset, elapsed, accent, globalAlpha);
            drawBottomStatus(sw, sh, elapsed, smoothProgress, accent, globalAlpha);

            RenderHelper.scaleEnd();
        } finally {
            RenderHelper.endRender();
            States.pop();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
        }

    }

    private static void drawBackground(float sw, float sh, float elapsed, Color accent, float alphaMul) {
        RenderHelper.drawRoundedRect(0, 0, sw, sh, alpha(new Color(5, 6, 10, 255), alphaMul), 0);

        float t = elapsed * 0.001f;

        // 大型深色背景球
        RenderHelper.drawGradientCircle(
                sw * 0.14f + (float) Math.sin(t * 0.42f) * 24f,
                sh * 0.16f + (float) Math.cos(t * 0.37f) * 20f,
                300f,
                alpha(new Color(255, 255, 255, 12), alphaMul),
                alpha(new Color(255, 255, 255, 0), alphaMul)
        );

        RenderHelper.drawGradientCircle(
                sw * 0.88f + (float) Math.cos(t * 0.35f) * 26f,
                sh * 0.22f + (float) Math.sin(t * 0.41f) * 18f,
                250f,
                alpha(new Color(255, 255, 255, 10), alphaMul),
                alpha(new Color(255, 255, 255, 0), alphaMul)
        );

        RenderHelper.drawGradientCircle(
                sw * 0.80f + (float) Math.sin(t * 0.28f) * 36f,
                sh * 0.86f + (float) Math.cos(t * 0.31f) * 28f,
                340f,
                alpha(new Color(255, 255, 255, 8), alphaMul),
                alpha(new Color(255, 255, 255, 0), alphaMul)
        );

        // 主题色大型辉光
        RenderHelper.drawGradientCircle(
                sw * 0.50f + (float) Math.sin(t * 0.75f) * 18f,
                sh * 0.47f + (float) Math.cos(t * 0.62f) * 16f,
                220f,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 34), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0), alphaMul)
        );

        RenderHelper.drawGradientCircle(
                sw * 0.61f + (float) Math.cos(t * 0.57f) * 24f,
                sh * 0.65f + (float) Math.sin(t * 0.48f) * 20f,
                175f,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0), alphaMul)
        );

        RenderHelper.drawGradientCircle(
                sw * 0.39f + (float) Math.sin(t * 0.68f) * 20f,
                sh * 0.37f + (float) Math.cos(t * 0.54f) * 14f,
                150f,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 16), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0), alphaMul)
        );

        // 边缘点缀球
        RenderHelper.drawGradientCircle(
                sw * 0.52f,
                sh * 0.82f,
                95f,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 8), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0), alphaMul)
        );

        RenderHelper.drawGradientCircle(
                sw * 0.48f,
                sh * 0.26f,
                88f,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 10), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0), alphaMul)
        );
    }

    private static void drawDecorations(float sw, float sh, float elapsed, Color accent, float alphaMul) {
        float t = elapsed * 0.001f;

        // 横向/斜向光带
        drawLightBand(sw * 0.08f, sh * 0.24f + (float) Math.sin(t * 1.0f) * 7f, sw * 0.28f, 2.4f, accent, (int) (22 * alphaMul));
        drawLightBand(sw * 0.58f, sh * 0.18f + (float) Math.cos(t * 0.92f) * 6f, sw * 0.20f, 2.2f, Color.WHITE, (int) (14 * alphaMul));
        drawLightBand(sw * 0.60f, sh * 0.73f + (float) Math.sin(t * 0.86f) * 8f, sw * 0.18f, 2.2f, accent, (int) (18 * alphaMul));
        drawLightBand(sw * 0.22f, sh * 0.82f + (float) Math.cos(t * 0.74f) * 6f, sw * 0.22f, 2.0f, Color.WHITE, (int) (10 * alphaMul));

        // 中央外围细环粒子轨道
        float cx = sw / 2f;
        float cy = sh / 2f - 10f;

        for (int i = 0; i < 44; i++) {
            float angle = t * 0.55f + i * 8.2f;
            double rad = Math.toRadians(angle);
            float rx = 255f;
            float ry = 118f;

            float px = cx + (float) Math.cos(rad) * rx;
            float py = cy + (float) Math.sin(rad) * ry;

            RenderHelper.drawCircle(px, py, 1.1f,
                    alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 26), alphaMul));
        }

        for (int i = 0; i < 26; i++) {
            float angle = -t * 0.42f + i * 13.5f;
            double rad = Math.toRadians(angle);
            float rx = 185f;
            float ry = 78f;

            float px = cx + (float) Math.cos(rad) * rx;
            float py = cy + (float) Math.sin(rad) * ry;

            RenderHelper.drawCircle(px, py, 0.9f,
                    alpha(new Color(255, 255, 255, 20), alphaMul));
        }
    }

    private static void drawMainLogo(float cx, float cy, float elapsed, Color accent, float alphaMul) {
        float intro = clamp(elapsed / 1100f, 0f, 1f);
        float introEase = easeOutBack(intro);

        float logoScale = 0.72f + 0.28f * introEase;
        float logoY = cy - 36f + (1f - easeOut(intro)) * 22f;
        int logoAlpha = (int) (255 * easeOut(intro) * alphaMul);

        float auraPulse = 0.92f + 0.08f * ((float) Math.sin(elapsed * 0.0032f) * 0.5f + 0.5f);

        RenderHelper.drawGradientCircle(
                cx,
                logoY + 34,
                170f * auraPulse,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0), alphaMul)
        );

        String first = "A";
        String rest = "rcane";

        int size = 86;

        float firstWidth = FontManager.medium.getStringWidth(first, size);
        float restWidth = FontManager.medium.getStringWidth(rest, size);
        float totalRawWidth = firstWidth + restWidth;
        float totalWidth = totalRawWidth * logoScale;

        float startX = cx - totalWidth / 2f;

        // A：主题色 glow
        drawScaledMedium(
                first,
                startX,
                logoY,
                logoScale,
                size,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), logoAlpha), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), logoAlpha), alphaMul),
                6
        );

        // rcane：白色 glow
        drawScaledMedium(
                rest,
                startX + firstWidth * logoScale - 2f,
                logoY,
                logoScale,
                size,
                alpha(new Color(255, 255, 255, logoAlpha), alphaMul),
                alpha(new Color(255, 255, 255, logoAlpha), alphaMul),
                5
        );

        // 2.0 出现在 Arcane 右上角
        float versionAppear = clamp((elapsed - 720f) / 520f, 0f, 1f);
        float versionScale = 0.45f + 0.55f * easeOutBack(versionAppear);
        int versionAlpha = (int) (255 * easeOut(versionAppear) * alphaMul);

        float versionRawWidth = FontManager.semibold.getStringWidth("S1.0", 34);
        float versionWidth = versionRawWidth * versionScale;

        float versionX = startX + totalWidth - versionWidth * 0.55f;
        float versionY = logoY - 10f;

        drawScaledSemibold(
                "S1.0",
                versionX,
                versionY,
                versionScale,
                34,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), versionAlpha), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), versionAlpha), alphaMul),
                4
        );

        float subtitleAppear = clamp((elapsed - 380f) / 650f, 0f, 1f);
        int subtitleAlpha = (int) (150 * easeOut(subtitleAppear) * alphaMul);
        String subtitle = "Next Generation Experience";

        float subtitleWidth = FontManager.semibold.getStringWidth(subtitle, 15);
        FontManager.semibold.drawString(
                15,
                subtitle,
                cx - subtitleWidth / 2f,
                logoY + 86,
                new Color(255, 255, 255, subtitleAlpha),
                false
        );

        drawSweepLine(cx, logoY + 54, elapsed, accent, alphaMul);
    }

    private static void drawSweepLine(float cx, float y, float elapsed, Color accent, float alphaMul) {
        float period = 2600f;
        float p = (elapsed % period) / period;
        float width = 220f;
        float sweepX = cx - width / 2f + width * p;

        RenderHelper.drawRoundRectBloom(
                sweepX - 20f,
                y,
                40f,
                2.4f,
                alpha(new Color(255, 255, 255, 28), alphaMul),
                10
        );

        RenderHelper.drawGradientRoundedRectLR(
                sweepX - 20f,
                y,
                40f,
                2.0f,
                alpha(new Color(255, 255, 255, 0), alphaMul),
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70), alphaMul),
                2
        );
    }

    private static void drawBottomStatus(float sw, float sh, float elapsed, float progress, Color accent, float alphaMul) {
        String status = resolveStatus(progress, elapsed);
        float textWidth = FontManager.semibold.getStringWidth(status, 14);
        float boxWidth = textWidth + 34;
        float boxHeight = 24;
        float x = sw / 2f - boxWidth / 2f;
        float y = sh - 66;

        RenderHelper.drawRoundRectBloom(x, y, boxWidth, boxHeight, alpha(new Color(25, 25, 26, 80), alphaMul), 12);
        RenderHelper.drawRoundedRect(x, y, boxWidth, boxHeight, alpha(new Color(18, 18, 22, 135), alphaMul), 12);

        float pulse = 0.55f + 0.45f * ((float) Math.sin(elapsed * 0.006f) * 0.5f + 0.5f);

        RenderHelper.drawCircle(
                x + 14,
                y + boxHeight / 2f,
                4.2f,
                alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (190 * pulse)), alphaMul)
        );

        FontManager.semibold.drawString(
                14,
                status,
                x + 24,
                y + 9.5F,
                alpha(new Color(255, 255, 255, 185), alphaMul),
                false
        );
    }

    private static void drawFloatingParticles(float cx, float cy, float elapsed, Color accent, float alphaMul) {
        float t = elapsed * 0.001f;

        for (int i = 0; i < 34; i++) {
            float angle = t * (0.22f + i * 0.009f) + i * 0.82f;
            float radius = 125f + (float) Math.sin(t * 1.2f + i) * 38f;

            float px = cx + (float) Math.cos(angle) * radius * 1.7f;
            float py = cy + (float) Math.sin(angle) * radius * 0.78f;

            float size = 0.8f + ((float) Math.sin(t * 2.2f + i) * 0.5f + 0.5f) * 2.0f;
            int alpha = 18 + (int) ((((float) Math.cos(t * 1.8f + i) * 0.5f + 0.5f)) * 78f);

            Color color = i % 5 == 0
                    ? alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha), alphaMul)
                    : alpha(new Color(255, 255, 255, alpha), alphaMul);

            RenderHelper.drawCircle(px, py, size, color);
        }

        for (int i = 0; i < 18; i++) {
            float angle = -t * (0.16f + i * 0.007f) + i * 1.18f;
            float radius = 245f + (float) Math.cos(t + i) * 20f;

            float px = cx + (float) Math.cos(angle) * radius;
            float py = cy + (float) Math.sin(angle) * radius * 0.60f;

            RenderHelper.drawCircle(
                    px,
                    py,
                    1.1f,
                    alpha(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 32), alphaMul)
            );
        }
    }

    private static void drawLightBand(float x, float y, float width, float height, Color color, int alpha) {
        RenderHelper.drawGradientRoundedRectLR(
                x, y, width, height,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha),
                2
        );
    }

    private static void drawScaledMedium(String text, float x, float y, float scale, int size, Color color, Color glow, int glowSize) {
        float width = FontManager.medium.getStringWidth(text, size);
        float height = FontManager.medium.getHeight(size);

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        RenderHelper.scaleStart(centerX, centerY, scale);
        FontManager.medium.drawGlowString(
                size,
                text,
                centerX - width / 2f,
                centerY - height / 2f,
                color,
                glow,
                false,
                glowSize
        );
        RenderHelper.scaleEnd();
    }

    private static void drawScaledSemibold(String text, float x, float y, float scale, int size, Color color, Color glow, int glowSize) {
        float width = FontManager.semibold.getStringWidth(text, size);
        float height = FontManager.semibold.getHeight(size);

        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        RenderHelper.scaleStart(centerX, centerY, scale);
        FontManager.semibold.drawGlowString(
                size,
                text,
                centerX - width / 2f,
                centerY - height / 2f,
                color,
                glow,
                false,
                glowSize
        );
        RenderHelper.scaleEnd();
    }

    private static String resolveStatus(float progress, float elapsed) {
        if (elapsed < 900) return "Bootstrapping Arcane...";
        if (elapsed < 1800) return "Initializing client resources...";
        if (elapsed < 2800) return "Loading modules and values...";
        if (elapsed < 3800) return "Building interface and theme...";
        if (elapsed < 4700) return "Preparing rendering pipeline...";
        if (elapsed < MIN_SHOW_TIME) return "Finalizing Arcane experience...";
        return "Launching Arcane Menu...";
    }

    private static Color alpha(Color color, float mul) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.max(0, Math.min(255, (int) (color.getAlpha() * mul)))
        );
    }

    private static float animate(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float easeOut(float x) {
        return 1f - (float) Math.pow(1f - x, 3);
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    public static void reset() {
        startTime = -1L;
        exitStartTime = -1L;
        pendingSwitchTime = -1L;
        exiting = false;
        switched = false;
        pendingSwitch = false;
        smoothProgress = 0f;
    }

}