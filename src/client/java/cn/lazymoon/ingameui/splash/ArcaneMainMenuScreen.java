package cn.lazymoon.ingameui.splash;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.render.PostProcessing;
import cn.lazymoon.ingameui.auth.AuthScreen;
import cn.lazymoon.nanovg.gl.States;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static cn.lazymoon.utils.InstanceAccess.mc;

public class ArcaneMainMenuScreen extends Screen {

    private long initTime = 0L;
    private boolean extraBackgroundEnabled = false;

    private final List<MenuEntry> entries = new ArrayList<>();
    private final Animation introFade = new DecelerateAnimation(520, 1);
    private long menuFadeStart = -1L;

    public ArcaneMainMenuScreen() {
        super(Text.empty());

        entries.add(new MenuEntry("Singleplayer", "I", this::openSingleplayer));
        entries.add(new MenuEntry("Multiplayer", "K", this::openMultiplayer));
        entries.add(new MenuEntry("Account Manager", "O", this::openAuthScreen));
        entries.add(new MenuEntry("Options", "J", this::openOptions));
        entries.add(new MenuEntry("Accessibility", "F", this::openAccessibility));
    }

    private void openSingleplayer() {
        if (this.client != null) {
            MinecraftClient client = this.client;
            client.execute(() -> client.setScreen(new SelectWorldScreen(this)));
        }
    }

    private void openMultiplayer() {
        if (this.client != null) {
            MinecraftClient client = this.client;
            client.execute(() -> client.setScreen(new MultiplayerScreen(this)));
        }
    }

    private void openOptions() {
        if (this.client != null) {
            MinecraftClient client = this.client;
            client.execute(() -> client.setScreen(new OptionsScreen(this, client.options)));
        }
    }

    private void openAccessibility() {
        if (this.client != null) {
            MinecraftClient client = this.client;
            client.execute(() -> client.setScreen(new AccessibilityOptionsScreen(this, client.options)));
        }
    }

    private void openAuthScreen() {
        MinecraftClient client = this.client != null ? this.client : MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> {
            try {
                client.setScreen(new AuthScreen(this));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private static final Map<String, Identifier> AVATAR_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> AVATAR_LOADING = new ConcurrentHashMap<>();

    @Override
    protected void init() {
        super.init();
        initTime = System.currentTimeMillis();
        menuFadeStart = System.currentTimeMillis();
    }

    private static float easeOut(float x) {
        return 1f - (float) Math.pow(1f - x, 3);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (initTime == 0L) {
            initTime = System.currentTimeMillis();
        }

        long now = System.currentTimeMillis();
        float elapsed = now - initTime;

        if (menuFadeStart == -1L) {
            menuFadeStart = now;
        }

        float fadeRaw = Math.min(1.0f, (now - menuFadeStart) / 700.0f);
        float fade = easeOut(fadeRaw);

        Color accent = Theme.getCurrentTheme().getColors().first;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        States.push();
        RenderHelper.beginRender();
        try {
            RenderHelper.drawRoundedRect(0, 0, width, height, new Color(0, 0, 0, (int) ((1f - fade) * 255)), 0);

            drawBackground(elapsed, accent, fade);
            if (extraBackgroundEnabled) {
                drawOverlayBackground(elapsed, accent, fade);
            }

            drawLogo(accent, fade);
            drawProfileCard(context, accent, fade);
            drawMenu(mouseX, mouseY, accent, fade);
            drawBottomLeftControls(accent, fade);
            drawBottomRightInfo(accent, fade);

            RenderHelper.drawRoundedRect(0, 0, width, height, new Color(0, 0, 0, (int) ((1f - fade) * 255)), 0);
        } finally {
            RenderHelper.endRender();
            States.pop();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableDepthTest();
        }
    }

    private void drawBackground(float elapsed, Color accent, float fade) {
        int w = width;
        int h = height;
        float t = elapsed * 0.001f;

        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloomApple(0, 0, w, h, 10, 2, new Color(255, 255, 255, 10));
        }
        RenderHelper.drawGradientAppleRoundedRectLR(0, 0, w, h,
                 new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),10);

        RenderHelper.drawGradientCircle(
                w * 0.11f + (float) Math.sin(t * 0.14f) * 18f,
                h * 0.15f + (float) Math.cos(t * 0.13f) * 12f,
                480f,
                new Color(255, 155, 110, (int) (28 * fade)),
                new Color(255, 155, 110, 0)
        );

        RenderHelper.drawGradientCircle(
                w * 0.88f + (float) Math.cos(t * 0.12f) * 18f,
                h * 0.18f + (float) Math.sin(t * 0.11f) * 10f,
                420f,
                new Color(100, 150, 255, (int) (24 * fade)),
                new Color(100, 150, 255, 0)
        );

        RenderHelper.drawGradientCircle(
                w * 0.50f + (float) Math.sin(t * 0.20f) * 10f,
                h * 0.40f + (float) Math.cos(t * 0.18f) * 8f,
                600f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (22 * fade)),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)
        );

        RenderHelper.drawGradientCircle(
                w * 0.50f,
                h * 0.52f,
                350f,
                new Color(0, 0, 0, (int) (50 * fade)),
                new Color(0, 0, 0, 0)
        );

        RenderHelper.drawGradientCircle(
                w * 0.24f,
                h + 160f,
                760f,
                new Color(18, 26, 44, (int) (230 * fade)),
                new Color(18, 26, 44, 0)
        );

        RenderHelper.drawGradientCircle(
                w * 0.60f,
                h + 220f,
                860f,
                new Color(10, 16, 30, (int) (245 * fade)),
                new Color(10, 16, 30, 0)
        );

        RenderHelper.drawGradientCircle(
                w * 0.92f,
                h + 160f,
                560f,
                new Color(6, 10, 18, (int) (255 * fade)),
                new Color(6, 10, 18, 0)
        );

        float cx = w / 2f;
        float cy = h / 2f - 10f;

        for (int i = 0; i < 56; i++) {
            float a = t * 0.58f + i * 0.17f;
            float rx = 300f + (float) Math.sin(t * 0.7f + i) * 12f;
            float ry = 130f + (float) Math.cos(t * 0.66f + i * 0.8f) * 8f;

            float px = cx + (float) Math.cos(a) * rx;
            float py = cy + (float) Math.sin(a) * ry;

            Color c;
            switch (i % 6) {
                case 0 -> c = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 26);
                case 1 -> c = new Color(255, 255, 255, 20);
                case 2 -> c = new Color(120, 170, 255, 18);
                case 3 -> c = new Color(255, 140, 180, 16);
                case 4 -> c = new Color(140, 255, 200, 16);
                default -> c = new Color(200, 140, 255, 16);
            }

            RenderHelper.drawCircle(px, py, 1.0f + (i % 3) * 0.12f, c);
        }

        for (int i = 0; i < 22; i++) {
            float fx = w * (0.06f + (i % 6) * 0.15f);
            float fy = h * (0.14f + (i % 4) * 0.18f) + (float) Math.sin(t * 0.95f + i * 0.6f) * 18f;

            Color c;
            switch (i % 5) {
                case 0 -> c = new Color(120, 160, 255, 16);
                case 1 -> c = new Color(255, 120, 160, 14);
                case 2 -> c = new Color(120, 255, 180, 14);
                case 3 -> c = new Color(255, 200, 120, 14);
                default -> c = new Color(180, 130, 255, 14);
            }

            RenderHelper.drawGradientCircle(fx, fy, 44f + (i % 3) * 10f, c, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
        }

        for (int i = 0; i < 76; i++) {
            float px = (w * 0.03f) + i * 20f + (float) Math.sin(t * 0.75f + i * 0.43f) * 10f;
            float py = (h * 0.10f) + (i % 12) * 44f + (float) Math.cos(t * 0.68f + i * 0.8f) * 5f;

            if (px > -10 && px < w + 10 && py > -10 && py < h + 10) {
                RenderHelper.drawCircle(px, py, 1.0f + (i % 4) * 0.12f, new Color(255, 255, 255, 9));
            }
        }

        for (int i = 0; i < 7; i++) {
            float sy = (h * 0.10f) + i * 86f + (float) Math.sin(t * 0.8f + i) * 5f;
            RenderHelper.drawRoundedRect(0, sy, w, 1.2f, new Color(255, 255, 255, (int) (4 * fade)), 0);
        }

        RenderHelper.drawGradientCircle(-40f, -40f, 420f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
        RenderHelper.drawGradientCircle(w + 40f, -40f, 420f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
        RenderHelper.drawGradientCircle(-40f, h + 40f, 520f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
        RenderHelper.drawGradientCircle(w + 40f, h + 40f, 520f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
    }

    private void drawOverlayBackground(float elapsed, Color accent, float fade) {
        int w = width;
        int h = height;
        float t = elapsed * 0.001f;

        for (int i = 0; i < 12; i++) {
            float px = w * (0.08f + (i % 4) * 0.24f) + (float) Math.sin(t * (0.4f + i * 0.03f)) * 18f;
            float py = h * (0.15f + (i / 4) * 0.24f) + (float) Math.cos(t * (0.45f + i * 0.02f)) * 16f;

            Color c = switch (i % 4) {
                case 0 -> new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (14 * fade));
                case 1 -> new Color(255, 255, 255, (int) (10 * fade));
                case 2 -> new Color(255, 140, 180, (int) (12 * fade));
                default -> new Color(120, 170, 255, (int) (12 * fade));
            };

            RenderHelper.drawGradientCircle(px, py, 78f + (i % 3) * 16f, c,
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
        }

        for (int i = 0; i < 8; i++) {
            float sy = h * 0.12f + i * 72f + (float) Math.sin(t * 0.9f + i) * 8f;
            RenderHelper.drawGradientRoundedRectLR(0, sy, w, 1.2f,
                    new Color(255, 255, 255, 0),
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (14 * fade)),
                    0);
        }
    }

    private void drawLogo(Color accent, float fade) {
        Window window = MinecraftClient.getInstance().getWindow();
        float x = window.getScaledWidth() / 2 - 340;
        float y = window.getScaledHeight() / 2 - 10;

        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloomApple(x,
                    y + 4,
                    18,
                    18, 10, 2, new Color(255, 255, 255, 10));
        }
        RenderHelper.drawGradientAppleRoundedRectLR(x,
                y + 4,
                18,
                18, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),10);

        FontManager.icon.drawGlowString(
                20,
                "A",
                x + 4,
                y + 7,
                new Color(255, 255, 255, (int) (255 * fade)),
                new Color(255, 255, 255, (int) (255 * fade)),
                false,
                1
        );

        FontManager.medium.drawGlowString(
                20,
                "Arcane",
                x + 24,
                y + 6,
                new Color(255, 255, 255, (int) (255 * fade)),
                new Color(255, 255, 255, (int) (255 * fade)),
                false,
                1
        );

        FontManager.semibold.drawString(
                10,
                "NextGen",
                x + 24,
                y + 16,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (220 * fade)),
                false
        );

        FontManager.semibold.drawString(
                10,
                "S1.0",
                x + 60,
                y + 4,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (220 * fade)),
                false
        );

        RenderHelper.drawRoundedRect(x, y + 30, 92, 1.6f, accent, 1);
    }

    private void drawProfileCard(DrawContext context, Color accent, float fade) {
        float w = 176;
        float h = 40;
        float x = this.width - w - 22;
        float y = 16;
        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloomApple(x,
                    y,
                    w,
                    h, 10, 2, new Color(255, 255, 255, 10));
        }
        RenderHelper.drawGradientAppleRoundedRectLR(x,
                y,
                w,
                h,
                new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),10);

        String username = "Guyuemang";

        String uuid = null;
        if (this.client != null && this.client.getSession() != null) {
            uuid = String.valueOf(this.client.getSession().getUuidOrNull());
        }

        Identifier avatarTexture = getAvatarTextureByUuid(uuid);
        if (avatarTexture != null) {
            RenderHelper.drawImageNative(
                    context.getMatrices(),
                    x + 8,
                    y + 7,
                    26,
                    26,
                    avatarTexture
            );
        }

        FontManager.semibold.drawString(
                15,
                username,
                x + 12,
                y + 12,
                new Color(255, 255, 255, (int) (255 * fade)),
                false
        );

        String uidText = "Click to login";
        Color uidColor = new Color(255, 255, 255, (int) (120 * fade));

        FontManager.semibold.drawString(
                15,
                uidText,
                x + 12,
                y + 22,
                uidColor,
                false
        );

        FontManager.icon1.drawString(
                15,
                "D",
                x + w - 18,
                y + 11,
                new Color(255, 255, 255, (int) (160 * fade)),
                false
        );
    }


    private static Identifier getAvatarTextureByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        String key = uuid.replace("-", "").toLowerCase(Locale.ROOT);

        Identifier cached = AVATAR_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        if (AVATAR_LOADING.putIfAbsent(key, true) == null) {
            CompletableFuture.runAsync(() -> {
                try {
                    String url = "https://crafatar.com/avatars/" + key + "?size=64&overlay";

                    try (InputStream inputStream = new URL(url).openStream()) {
                        NativeImage nativeImage = NativeImage.read(inputStream);
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                        Identifier id = Identifier.of("lazymoon", "avatar/" + key);

                        mc.execute(() -> {
                            mc.getTextureManager().registerTexture(id, texture);
                            AVATAR_CACHE.put(key, id);
                        });
                    }
                } catch (Exception ignored) {
                } finally {
                    AVATAR_LOADING.remove(key);
                }
            });
        }

        return null;
    }

    private void drawMenu(int mouseX, int mouseY, Color accent, float fade) {
        float itemW = 128;
        float itemH = 38;
        float gap = 12;

        float totalW = entries.size() * itemW + (entries.size() - 1) * gap;
        float startX = width / 2f - totalW / 2f;
        float startY = height / 2f + 42f; // 居中偏下一点，更像主菜单

        for (int i = 0; i < entries.size(); i++) {
            MenuEntry entry = entries.get(i);

            float x = startX + i * (itemW + gap);
            float y = startY;

            entry.x = x;
            entry.y = y;
            entry.w = itemW;
            entry.h = itemH;

            entry.render(mouseX, mouseY, accent, fade);
        }
    }

    private void drawBottomLeftControls(Color accent, float fade) {
        float x = 28;
        float y = this.height - 50;

        RenderHelper.drawRoundedRect(
                x,
                y,
                90,
                28,
                new Color(8, 11, 16, (int) (200 * fade)),
                8
        );

        RenderHelper.drawRoundedRect(
                x + 1,
                y + 1,
                88,
                26,
                new Color(255, 255, 255, (int) (8 * fade)),
                7
        );

        RenderHelper.drawRoundedRect(
                x + 5,
                y + 5,
                18,
                18,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (255 * fade)),
                6
        );

        FontManager.icon1.drawString(
                24,
                "H",
                x + 9,
                y + 6.5f,
                Color.WHITE,
                false
        );

        FontManager.semibold.drawString(
                20,
                "Exit",
                x + 30,
                y + 10,
                new Color(255, 255, 255, (int) (145 * fade)),
                false
        );
    }

    private void drawBottomRightInfo(Color accent, float fade) {
    }

    private void drawSocialIcon(float x, float y, float size, String icon, Color accent, float fade) {
        RenderHelper.drawRoundedRect(
                x,
                y,
                size,
                size,
                new Color(255, 255, 255, (int) (10 * fade)),
                7
        );

        RenderHelper.drawRoundedRect(
                x + 1,
                y + 1,
                size - 2,
                size - 2,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (28 * fade)),
                6
        );

        FontManager.icon1.drawString(
                26,
                icon,
                x + size / 2f - FontManager.icon1.getStringWidth(icon, 26) / 2f +0.5f,
                y + 14 / 2f - FontManager.icon1.getHeight(26) / 2f,
                Color.WHITE,
                false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (MenuEntry entry : entries) {
                if (entry.isHovered(mouseX, mouseY)) {
                    try {
                        entry.action.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    return true;
                }
            }

            float x = 28;
            float y = this.height - 50;

            if (RenderUtils.isHovering(x, y, 90, 28, mouseX, mouseY)) {
                if (this.client != null) this.client.scheduleStop();
                return true;
            }

            if (RenderUtils.isHovering(x + 98, y, 120, 28, mouseX, mouseY)) {
                extraBackgroundEnabled = !extraBackgroundEnabled;
                return true;
            }

            float infoX = this.width - 330 - 24;
            float infoY = this.height - 54;
            float iconSize = 22;
            float gap = 6;
            float startX = infoX + 10;
            float iconY = infoY + 6;

            float profileX = this.width - 176 - 22;
            float profileY = 16;
            if (RenderUtils.isHovering(profileX, profileY, 176, 40, mouseX, mouseY)) {
                openAuthScreen();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_O) {
            openAuthScreen();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static class MenuEntry {
        private final String title;
        private final String iconText;
        private final Runnable action;

        private float x, y, w, h;
        private final Animation hover = new DecelerateAnimation(120, 1);

        private MenuEntry(String title, String iconText, Runnable action) {
            this.title = title;
            this.iconText = iconText;
            this.action = action;
        }

        private boolean isHovered(double mouseX, double mouseY) {
            return RenderUtils.isHovering(x, y, w, h, mouseX, mouseY);
        }

        private void render(int mouseX, int mouseY, Color accent, float fade) {
            boolean hovered = isHovered(mouseX, mouseY);
            hover.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);

            float hv = hover.getOutput().floatValue();
            int bgAlpha = (int) ((18 + 22 * hv) * fade);

            // 长条背景
            RenderHelper.drawRoundedRect(
                    x,
                    y,
                    w,
                    h,
                    new Color(8, 11, 16, bgAlpha),
                    12
            );

            // 边框高亮
            RenderHelper.drawRoundedRect(
                    x + 1,
                    y + 1,
                    w - 2,
                    h - 2,
                    new Color(255, 255, 255, (int) ((6 + 10 * hv) * fade)),
                    11
            );

            // 左侧 icon 背景块
            RenderHelper.drawRoundedRect(
                    x + 8,
                    y + 7,
                    24,
                    24,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) ((200 + 40 * hv) * fade)),
                    8
            );

            // 只画 icon，不画文字
            FontManager.icon1.drawGlowString(
                    24,
                    iconText,
                    x + 20.5f - FontManager.icon1.getStringWidth(iconText, 24) / 2f,
                    y + 14.5f - FontManager.icon1.getHeight(24) / 2f,
                    Color.WHITE,
                    Color.WHITE,
                    false,
                    1
            );
            FontManager.semibold.drawGlowString(
                    18,
                    title,
                    x + 38,
                    y + 15,
                    hovered ? Color.WHITE : new Color(255, 255, 255, (int) (230 * fade)),
                    hovered ? Color.WHITE : new Color(255, 255, 255, (int) (230 * fade)),
                    false,
                    1
            );
            // 悬停时的右侧箭头
            if (hovered) {
                FontManager.medium.drawString(
                        22,
                        "›",
                        x + w - 10,
                        y + 15,
                        accent,
                        false
                );
            }
        }
    }
}