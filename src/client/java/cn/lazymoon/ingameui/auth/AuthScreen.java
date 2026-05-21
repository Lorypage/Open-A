package cn.lazymoon.ingameui.auth;

import cn.lazymoon.Client;
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
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuthScreen extends Screen {

    private enum InputFocus {
        NONE, TOKEN, OFFLINE
    }

    private final Screen parent;
    private final Animation open = new DecelerateAnimation(220, 1);
    private int hoveredDeleteIndex = -1;
    private AuthMode mode = AuthMode.MICROSOFT;

    private String refreshToken = "";
    private String offlineName = "";

    private String microsoftUserCode = "";
    private String microsoftVerificationUri = "";
    private String microsoftVerificationUriComplete = "";

    private String status = "Select a login method";
    private String statusDetail = "";
    private Color statusColor = Color.WHITE;

    private boolean waitingMicrosoft = false;
    private boolean waitingToken = false;
    private boolean waitingOffline = false;

    private InputFocus focus = InputFocus.NONE;

    private float historyScroll = 0f;
    private int lastHistoryClickIndex = -1;
    private long lastHistoryClickTime = 0L;
    private static final long DOUBLE_CLICK_MS = 280L;

    private boolean loading = false;
    private String loadingTitle = "";
    private String loadingDetail = "";
    private float loadingSpin = 0f;

    public AuthScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        open.setDirection(Direction.FORWARDS);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        States.push();
        RenderHelper.beginRender();

        try {
            float uiAlpha = Math.max(0.25f, open.getOutput().floatValue());
            Color accent = Theme.getCurrentTheme().getColors().first;

            drawBackground(uiAlpha);

            float pw = 760;
            float ph = 420;
            float px = (width - pw) / 2f;
            float py = (height - ph) / 2f;

            RenderHelper.drawRoundRectBloom(px, py, pw, ph, new Color(28, 30, 35, (int) (238 * uiAlpha)), 24);
            RenderHelper.drawRoundedRect(px, py, pw, ph, new Color(28, 30, 35, (int) (238 * uiAlpha)), 24);

            float leftW = 230;

            RenderHelper.drawRoundedRect(px + 22, py + 22, 30, 30, accent, 8);
            FontManager.icon.drawGlowString(32, "A", px + 29, py + 27, Color.WHITE, Color.WHITE, false, 1);

            FontManager.medium.drawGlowString(28, "Arcane", px + 22, py + 76, Color.WHITE, Color.WHITE, false, 1);
            FontManager.semibold.drawString(11, "Account Center", px + 22, py + 102, new Color(255, 255, 255, (int) (130 * uiAlpha)), false);
            RenderHelper.drawRoundedRect(px + 22, py + 118, 140, 1.4f, accent, 1);

            FontManager.medium.drawString(14, "History", px + 22, py + 132, new Color(255, 255, 255, (int) (210 * uiAlpha)), false);

            float historyBoxX = px + 18;
            float historyBoxY = py + 152;
            float historyBoxW = leftW - 36;
            float historyBoxH = ph - 172;

            RenderHelper.drawRoundedRect(historyBoxX, historyBoxY, historyBoxW, historyBoxH, new Color(255, 255, 255, 8), 10);
            RenderHelper.drawRoundedRect(historyBoxX + 1, historyBoxY + 1, historyBoxW - 2, historyBoxH - 2, new Color(255, 255, 255, 4), 9);

            drawHistoryList(historyBoxX + 6, historyBoxY + 6, historyBoxW - 12, historyBoxH - 12, mouseX, mouseY, uiAlpha);

            float contentX = px + leftW + 18;
            float contentY = py + 18;
            float contentW = pw - leftW - 36;
            float contentH = ph - 36;

            RenderHelper.drawRoundedRect(contentX, contentY, contentW, contentH, new Color(255, 255, 255, 8), 18);

            FontManager.medium.drawGlowString(22, "Account Manager", contentX + 14, contentY + 12, Color.WHITE, Color.WHITE, false, 1);
            FontManager.semibold.drawString(12, "Microsoft / Token / Offline", contentX + 14, contentY + 36, new Color(255, 255, 255, (int) (120 * uiAlpha)), false);

            drawStatusChip(contentX + contentW - 126, contentY + 12, 112, 24, status, statusColor);

            float tabY = contentY + 58;
            float tabW = 112;
            float tabH = 26;
            float tabGap = 8;
            float tabX = contentX + 14;

            drawTab(tabX, tabY, tabW, tabH, "Microsoft", AuthMode.MICROSOFT, mouseX, mouseY);
            drawTab(tabX + tabW + tabGap, tabY, tabW, tabH, "Token", AuthMode.TOKEN, mouseX, mouseY);
            drawTab(tabX + (tabW + tabGap) * 2, tabY, tabW, tabH, "Offline", AuthMode.OFFLINE, mouseX, mouseY);

            float sectionX = contentX + 14;
            float sectionY = contentY + 96;
            float sectionW = contentW - 28;
            float sectionH = 150;

            RenderHelper.drawRoundedRect(sectionX, sectionY, sectionW, sectionH, new Color(255, 255, 255, 8), 14);

            if (mode == AuthMode.MICROSOFT) {
                drawMicrosoftPage(sectionX, sectionY, sectionW, sectionH);
            } else if (mode == AuthMode.TOKEN) {
                drawTokenPage(sectionX, sectionY, sectionW, sectionH, mouseX, mouseY);
            } else {
                drawOfflinePage(sectionX, sectionY, sectionW, sectionH, mouseX, mouseY);
            }

            float detailY = sectionY + sectionH + 10;
            float detailH = 38;
            if (statusDetail != null && !statusDetail.isBlank()) {
                drawDetailBox(sectionX, detailY, sectionW, detailH, statusDetail, uiAlpha);
            }

            float btnY = contentY + contentH - 34;
            float backW = 80;
            float actionW = 136;

            renderButton(contentX + contentW - 14 - backW, btnY, backW, 24, "Back", mouseX, mouseY, false);
            renderButton(
                    contentX + contentW - 14 - backW - 10 - actionW,
                    btnY,
                    actionW,
                    24,
                    mode == AuthMode.MICROSOFT ? "Start Login" : "Login",
                    mouseX,
                    mouseY,
                    true
            );

            if (loading) {
                drawLoadingOverlay(delta);
            }
        } finally {
            RenderHelper.endRender();
            States.pop();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableDepthTest();
        }
    }

    private void drawBackground(float alphaMul) {
        int w = width;
        int h = height;
        float t = System.currentTimeMillis() * 0.001f;
        Color accent = Theme.getCurrentTheme().getColors().first;

        RenderHelper.drawRoundedRect(0, 0, w, h, new Color(4, 6, 10, 255), 0);

        RenderHelper.drawGradientCircle(w * 0.16f + (float) Math.sin(t * 0.28f) * 20f, h * 0.18f + (float) Math.cos(t * 0.22f) * 12f, 420f,
                new Color(255, 155, 110, (int) (26 * alphaMul)), new Color(255, 155, 110, 0));

        RenderHelper.drawGradientCircle(w * 0.84f + (float) Math.cos(t * 0.24f) * 16f, h * 0.20f + (float) Math.sin(t * 0.20f) * 10f, 360f,
                new Color(110, 160, 255, (int) (24 * alphaMul)), new Color(110, 160, 255, 0));

        RenderHelper.drawGradientCircle(w * 0.50f + (float) Math.sin(t * 0.18f) * 10f, h * 0.42f + (float) Math.cos(t * 0.17f) * 8f, 560f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (22 * alphaMul)), new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0));

        RenderHelper.drawGradientCircle(w * 0.50f, h * 0.52f, 320f, new Color(0, 0, 0, (int) (30 * alphaMul)), new Color(0, 0, 0, 0));

        RenderHelper.drawGradientCircle(w * 0.24f, h + 160f, 720f, new Color(18, 26, 44, (int) (228 * alphaMul)), new Color(18, 26, 44, 0));
        RenderHelper.drawGradientCircle(w * 0.60f, h + 220f, 820f, new Color(10, 16, 30, (int) (245 * alphaMul)), new Color(10, 16, 30, 0));
        RenderHelper.drawGradientCircle(w * 0.92f, h + 160f, 560f, new Color(6, 10, 18, (int) (255 * alphaMul)), new Color(6, 10, 18, 0));

        float cx = w / 2f;
        float cy = h / 2f - 12f;

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
            RenderHelper.drawRoundedRect(0, sy, w, 1.2f, new Color(255, 255, 255, 4), 0);
        }

        RenderHelper.drawGradientCircle(-40f, -40f, 420f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
        RenderHelper.drawGradientCircle(w + 40f, -40f, 420f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
        RenderHelper.drawGradientCircle(-40f, h + 40f, 520f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
        RenderHelper.drawGradientCircle(w + 40f, h + 40f, 520f, new Color(0, 0, 0, 160), new Color(0, 0, 0, 0));
    }

    private void drawStatusChip(float x, float y, float w, float h, String text, Color color) {
        RenderHelper.drawRoundedRect(x, y, w, h, new Color(255, 255, 255, 10), 10);
        RenderHelper.drawRoundedRect(x + 1, y + 1, w - 2, h - 2, new Color(255, 255, 255, 4), 9);

        FontManager.semibold.drawString(11, text, x + 10, y + 10, color, false);
    }

    private void drawDetailBox(float x, float y, float w, float h, String detail, float alphaMul) {
        RenderHelper.drawRoundedRect(x, y, w, h, new Color(255, 100, 100, (int) (24 * alphaMul)), 10);
        RenderHelper.drawRoundedRect(x + 1, y + 1, w - 2, h - 2, new Color(0, 0, 0, (int) (90 * alphaMul)), 9);

        FontManager.semibold.drawString(10, "Detail", x + 10, y + 6, new Color(255, 140, 140, (int) (220 * alphaMul)), false);
        FontManager.semibold.drawString(10, ellipsis(detail, 120), x + 10, y + 18, new Color(255, 255, 255, (int) (180 * alphaMul)), false);
    }

    private void drawTab(float x, float y, float w, float h, String text, AuthMode target, int mouseX, int mouseY) {
        boolean selected = mode == target;
        boolean hovered = RenderUtils.isHovering(x, y, w, h, mouseX, mouseY);
        Color accent = Theme.getCurrentTheme().getColors().first;

        Color bg = selected
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 130)
                : new Color(255, 255, 255, hovered ? 18 : 9);

        RenderHelper.drawRoundedRect(x, y, w, h, bg, 8);

        FontManager.semibold.drawString(12, text, x + w / 2 - FontManager.semibold.getStringWidth(text,12) / 2, y + 10, selected ? Color.WHITE : new Color(255, 255, 255, 180), false);
    }

    private void drawMicrosoftPage(float x, float y, float w, float h) {
        FontManager.semibold.drawString(13, "Microsoft Login", x + 12, y + 12, Color.WHITE, false);
        FontManager.semibold.drawString(12, "No browser will be opened automatically.", x + 12, y + 32, new Color(255, 255, 255, 120), false);
        FontManager.semibold.drawString(12, "Use the code below to authorize the account.", x + 12, y + 48, new Color(255, 255, 255, 120), false);

        RenderHelper.drawRoundedRect(x + 12, y + 64, w - 24, 76, new Color(255, 255, 255, 6), 12);

        String codeText = microsoftUserCode == null || microsoftUserCode.isBlank() ? "Waiting for device code..." : "Code: " + microsoftUserCode;
        String uriText;

        if (microsoftVerificationUriComplete != null && !microsoftVerificationUriComplete.isBlank()) {
            uriText = "URL: " + ellipsis(microsoftVerificationUriComplete, 92);
        } else if (microsoftVerificationUri != null && !microsoftVerificationUri.isBlank()) {
            uriText = "URL: " + ellipsis(microsoftVerificationUri, 92);
        } else {
            uriText = "URL: -";
        }

        FontManager.semibold.drawString(12, codeText, x + 22, y + 84, waitingMicrosoft ? Theme.getCurrentTheme().getColors().first : Color.WHITE, false);
        FontManager.semibold.drawString(11, uriText, x + 22, y + 102, new Color(255, 255, 255, 150), false);

        String msg = waitingMicrosoft
                ? "Waiting for Microsoft authorization..."
                : "Click Start Login to request a device code.";

        FontManager.semibold.drawString(12, msg, x + 22, y + 122, waitingMicrosoft ? Theme.getCurrentTheme().getColors().first : new Color(255, 255, 255, 120), false);
    }

    private void drawTokenPage(float x, float y, float w, float h, int mouseX, int mouseY) {
        FontManager.semibold.drawString(13, "Session Token Login", x + 12, y + 12, Color.WHITE, false);
        FontManager.semibold.drawString(12, "Paste your Minecraft access token / session id to log in.", x + 12, y + 32, new Color(255, 255, 255, 120), false);

        drawInputBox(x + 12, y + 62, w - 24, 28, refreshToken, focus == InputFocus.TOKEN, "Paste refresh token here", mouseX, mouseY);
    }

    private void drawOfflinePage(float x, float y, float w, float h, int mouseX, int mouseY) {
        FontManager.semibold.drawString(13, "Offline Login", x + 12, y + 12, Color.WHITE, false);
        FontManager.semibold.drawString(12, "Enter any username to join in offline mode.", x + 12, y + 32, new Color(255, 255, 255, 120), false);

        drawInputBox(x + 12, y + 62, w - 24, 28, offlineName, focus == InputFocus.OFFLINE, "Username", mouseX, mouseY);
    }

    private void drawInputBox(float x, float y, float w, float h, String text, boolean focused, String placeholder, int mouseX, int mouseY) {
        boolean hovered = RenderUtils.isHovering(x, y, w, h, mouseX, mouseY);
        Color accent = Theme.getCurrentTheme().getColors().first;

        Color bg = focused
                ? new Color(25, 25, 28, 165)
                : new Color(25, 25, 28, hovered ? 115 : 90);

        RenderHelper.drawRoundedRect(x, y, w, h, bg, 9);
        RenderHelper.drawRoundedRect(x + 1, y + 1, w - 2, h - 2, new Color(255, 255, 255, 6), 8);

        String display = (text == null || text.isEmpty())
                ? placeholder
                : fitText(text, 12, w - 22) + (focused ? "_" : "");

        FontManager.semibold.drawString(
                12,
                display,
                x + 10,
                y + 9,
                text == null || text.isEmpty() ? new Color(255, 255, 255, 100) : Color.WHITE,
                false
        );

        if (focused || hovered) {
            RenderHelper.drawRoundedRect(x + 10, y + h - 3, 34, 1.4f, accent, 1);
        }
    }

    private void drawHistoryList(float x, float y, float w, float h, int mouseX, int mouseY, float alphaMul) {
        List<AuthHistoryManager.HistoryEntry> history = AuthHistoryManager.getHistory();
        hoveredDeleteIndex = -1;

        if (history.isEmpty()) {
            FontManager.semibold.drawString(10, "No history accounts", x + 6, y + 6,
                    new Color(255, 255, 255, (int) (120 * alphaMul)), false);
            return;
        }

        float rowH = 36f;
        float gap = 5f;
        float contentH = history.size() * (rowH + gap) - gap;
        float maxScroll = Math.max(0f, contentH - h);
        historyScroll = clamp(historyScroll, 0f, maxScroll);

        RenderHelper.scissorStart(x, y, w, h);
        try {
            for (int i = 0; i < history.size(); i++) {
                float ry = y - historyScroll + i * (rowH + gap);
                if (ry + rowH < y || ry > y + h) continue;

                AuthHistoryManager.HistoryEntry entry = history.get(i);
                boolean hovered = RenderUtils.isHovering(x, ry, w, rowH, mouseX, mouseY);

                RenderHelper.drawRoundedRect(x, ry, w, rowH, new Color(255, 255, 255, hovered ? 16 : 9), 8);
                RenderHelper.drawRoundedRect(x + 4, ry + 7, 20, 20, getTypeColor(entry.type()), 6);

                float deleteW = 18f;
                float deleteH = 18f;
                float deleteX = x + w - deleteW - 6f;
                float deleteY = ry + (rowH - deleteH) / 2f;

                boolean deleteHovered = RenderUtils.isHovering(deleteX, deleteY, deleteW, deleteH, mouseX, mouseY);
                if (deleteHovered) {
                    hoveredDeleteIndex = i;
                }

                String line1 = "[" + entry.type() + "] " + entry.username();
                String line2 = "UUID: " + entry.uuid();
                String line3 = "Last: " + entry.formatTime();

                float textMaxWidth = w - 34 - 28;

                FontManager.semibold.drawString(10, fitText(line1, 10, textMaxWidth), x + 30, ry + 6, Color.WHITE, false);
                FontManager.semibold.drawString(9, fitText(line2, 9, textMaxWidth), x + 30, ry + 17, new Color(255, 255, 255, (int) (145 * alphaMul)), false);
                FontManager.semibold.drawString(9, fitText(line3, 9, textMaxWidth), x + 30, ry + 27, new Color(255, 255, 255, (int) (110 * alphaMul)), false);

                RenderHelper.drawRoundedRect(
                        deleteX,
                        deleteY,
                        deleteW,
                        deleteH,
                        new Color(255, 80, 80, deleteHovered ? 150 : 95),
                        6
                );

                FontManager.semibold.drawString(
                        14,
                        "×",
                        deleteX + deleteW / 2f - FontManager.semibold.getStringWidth("×", 12) / 2f,
                        deleteY + deleteH / 2f - FontManager.semibold.getHeight(12) / 2f - 2,
                        Color.WHITE,
                        false
                );
            }
        } finally {
            RenderHelper.scissorEnd();
        }

        if (maxScroll > 0.1f) {
            float trackW = 3f;
            float trackX = x + w - 4f;
            float ratio = h / contentH;
            float thumbH = Math.max(18f, h * ratio);
            float thumbY = y + (historyScroll / maxScroll) * (h - thumbH);

            RenderHelper.drawRoundedRect(trackX, y, trackW, h, new Color(255, 255, 255, 18), 2);
            RenderHelper.drawRoundedRect(trackX, thumbY, trackW, thumbH, Theme.getCurrentTheme().getColors().first, 2);
        }
    }

    private boolean handleHistoryDeleteClick(double mouseX, double mouseY) {
        List<AuthHistoryManager.HistoryEntry> history = AuthHistoryManager.getHistory();
        if (history.isEmpty()) return false;

        float pw = 760;
        float ph = 420;
        float px = (width - pw) / 2f;
        float py = (height - ph) / 2f;

        float leftW = 230;
        float historyBoxX = px + 18;
        float historyBoxY = py + 152;
        float historyBoxW = leftW - 36;
        float historyBoxH = ph - 172;

        float rowH = 36f;
        float gap = 5f;
        float contentH = history.size() * (rowH + gap) - gap;
        float maxScroll = Math.max(0f, contentH - historyBoxH);
        historyScroll = clamp(historyScroll, 0f, maxScroll);

        for (int i = 0; i < history.size(); i++) {
            float ry = historyBoxY + 6 - historyScroll + i * (rowH + gap);
            float visibleTop = historyBoxY + 6;
            float visibleBottom = historyBoxY + 6 + (historyBoxH - 12);

            if (ry + rowH < visibleTop || ry > visibleBottom) continue;

            float rowX = historyBoxX + 6;
            float rowW = historyBoxW - 12;

            float deleteW = 18f;
            float deleteH = 18f;
            float deleteX = rowX + rowW - deleteW - 6f;
            float deleteY = ry + (rowH - deleteH) / 2f;

            if (RenderUtils.isHovering(deleteX, deleteY, deleteW, deleteH, mouseX, mouseY)) {
                AuthHistoryManager.HistoryEntry entry = history.get(i);
                boolean removed = AuthHistoryManager.removeHistory(entry);

                if (removed) {
                    try {
                        if (Client.INSTANCE != null && Client.INSTANCE.getConfigManager() != null) {
                            Client.INSTANCE.getConfigManager().saveAccountHistoryConfig();
                        }
                    } catch (Throwable ignored) {
                    }

                    setStatus("History entry deleted", entry.username(), new Color(255, 140, 140));
                }

                lastHistoryClickIndex = -1;
                lastHistoryClickTime = 0L;
                clampHistoryScroll();
                return true;
            }
        }

        return false;
    }

    private void renderButton(float x, float y, float w, float h, String text, int mouseX, int mouseY, boolean action) {
        boolean hovered = RenderUtils.isHovering(x, y, w, h, mouseX, mouseY);
        Color accent = Theme.getCurrentTheme().getColors().first;

        RenderHelper.drawRoundRectBloom(x, y, w, h, new Color(28, 28, 32, hovered ? 145 : 110), 8);
        RenderHelper.drawRoundedRect(x, y, w, h, new Color(28, 28, 32, hovered ? 145 : 110), 8);

        FontManager.semibold.drawString(
                12,
                text,
                x + w / 2f - FontManager.semibold.getStringWidth(text, 12) / 2f,
                y + 10,
                action ? accent : Color.WHITE,
                false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float pw = 760;
        float ph = 420;
        float px = (width - pw) / 2f;
        float py = (height - ph) / 2f;

        float leftW = 230;
        float contentX = px + leftW + 18;
        float contentY = py + 18;
        float contentW = pw - leftW - 36;
        float contentH = ph - 36;

        float tabY = contentY + 58;
        float tabW = 112;
        float tabH = 26;
        float tabGap = 8;
        float tabX = contentX + 14;

        float sectionX = contentX + 14;
        float sectionY = contentY + 96;
        float sectionW = contentW - 28;

        if (button == 0) {
            if (RenderUtils.isHovering(tabX, tabY, tabW, tabH, mouseX, mouseY)) {
                mode = AuthMode.MICROSOFT;
                focus = InputFocus.NONE;
                return true;
            }
            if (RenderUtils.isHovering(tabX + tabW + tabGap, tabY, tabW, tabH, mouseX, mouseY)) {
                mode = AuthMode.TOKEN;
                focus = InputFocus.TOKEN;
                return true;
            }
            if (RenderUtils.isHovering(tabX + (tabW + tabGap) * 2, tabY, tabW, tabH, mouseX, mouseY)) {
                mode = AuthMode.OFFLINE;
                focus = InputFocus.OFFLINE;
                return true;
            }

            float boxX = sectionX + 12;
            float boxY = sectionY + 62;
            float boxW = sectionW - 24;
            float boxH = 28;

            if (mode == AuthMode.TOKEN && RenderUtils.isHovering(boxX, boxY, boxW, boxH, mouseX, mouseY)) {
                focus = InputFocus.TOKEN;
                return true;
            }
            if (mode == AuthMode.OFFLINE && RenderUtils.isHovering(boxX, boxY, boxW, boxH, mouseX, mouseY)) {
                focus = InputFocus.OFFLINE;
                return true;
            }

            float historyBoxX = px + 18;
            float historyBoxY = py + 152;
            float historyBoxW = leftW - 36;
            float historyBoxH = ph - 172;

            if (RenderUtils.isHovering(historyBoxX, historyBoxY, historyBoxW, historyBoxH, mouseX, mouseY)) {
                if (handleHistoryDeleteClick(mouseX, mouseY)) {
                    return true;
                }
                if (handleHistoryClick(mouseX, mouseY)) {
                    return true;
                }
            }

            float btnY = contentY + contentH - 34;
            float backW = 80;
            float actionW = 136;

            float backX = contentX + contentW - 14 - backW;
            float actionX = contentX + contentW - 14 - backW - 10 - actionW;

            if (RenderUtils.isHovering(backX, btnY, backW, 24, mouseX, mouseY)) {
                if (this.client != null) {
                    MinecraftClient client = this.client;
                    client.execute(() -> client.setScreen(parent));
                }
                return true;
            }

            if (RenderUtils.isHovering(actionX, btnY, actionW, 24, mouseX, mouseY)) {
                if (mode == AuthMode.MICROSOFT) {
                    startMicrosoftLogin();
                } else if (mode == AuthMode.TOKEN) {
                    startTokenLogin();
                } else {
                    startOfflineLogin(null);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float pw = 760;
        float ph = 420;
        float px = (width - pw) / 2f;
        float py = (height - ph) / 2f;

        float leftW = 230;
        float historyBoxX = px + 18;
        float historyBoxY = py + 152;
        float historyBoxW = leftW - 36;
        float historyBoxH = ph - 172;

        if (RenderUtils.isHovering(historyBoxX, historyBoxY, historyBoxW, historyBoxH, mouseX, mouseY)) {
            historyScroll -= (float) verticalAmount * 22f;
            clampHistoryScroll();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void clampHistoryScroll() {
        List<AuthHistoryManager.HistoryEntry> history = AuthHistoryManager.getHistory();

        float rowH = 36f;
        float gap = 5f;

        float pw = 760;
        float ph = 420;
        float py = (height - ph) / 2f;
        float historyBoxH = ph - 172;

        float contentH = history.size() * (rowH + gap) - gap;
        float maxScroll = Math.max(0f, contentH - historyBoxH);

        historyScroll = clamp(historyScroll, 0f, maxScroll);
    }

    private boolean handleHistoryClick(double mouseX, double mouseY) {
        List<AuthHistoryManager.HistoryEntry> history = AuthHistoryManager.getHistory();
        if (history.isEmpty()) return false;

        float pw = 760;
        float ph = 420;
        float px = (width - pw) / 2f;
        float py = (height - ph) / 2f;

        float leftW = 230;
        float historyBoxX = px + 18;
        float historyBoxY = py + 152;
        float historyBoxW = leftW - 36;
        float historyBoxH = ph - 172;

        float rowH = 36f;
        float gap = 5f;
        float contentH = history.size() * (rowH + gap) - gap;
        float maxScroll = Math.max(0f, contentH - historyBoxH);
        historyScroll = clamp(historyScroll, 0f, maxScroll);

        float localY = (float) mouseY - historyBoxY + historyScroll;
        if (localY < 0) return false;

        int index = (int) (localY / (rowH + gap));
        if (index < 0 || index >= history.size()) return false;

        float rowTop = historyBoxY - historyScroll + index * (rowH + gap);
        if (!RenderUtils.isHovering(historyBoxX, rowTop, historyBoxW, rowH, mouseX, mouseY)) {
            return false;
        }

        AuthHistoryManager.HistoryEntry entry = history.get(index);
        long now = System.currentTimeMillis();
        boolean doubleClick = lastHistoryClickIndex == index && (now - lastHistoryClickTime) <= DOUBLE_CLICK_MS;

        if (doubleClick) {
            handleHistoryDoubleClick(entry);
            lastHistoryClickIndex = -1;
            lastHistoryClickTime = 0L;
            return true;
        }

        lastHistoryClickIndex = index;
        lastHistoryClickTime = now;

        if ("Offline".equalsIgnoreCase(entry.type()) || "LocalAuth".equalsIgnoreCase(entry.type())) {
            mode = AuthMode.OFFLINE;
            offlineName = entry.username();
            focus = InputFocus.OFFLINE;
            setStatus("Offline account selected", "Double-click the item to log in directly", new Color(120, 190, 255));
        } else if ("Microsoft".equalsIgnoreCase(entry.type())) {
            mode = AuthMode.MICROSOFT;
            focus = InputFocus.NONE;
            setStatus("Microsoft account selected", "Double-click to use saved refresh token", new Color(120, 190, 255));
        } else if ("Token".equalsIgnoreCase(entry.type())) {
            mode = AuthMode.TOKEN;
            focus = InputFocus.TOKEN;
            setStatus("Token history selected", "Tokens are not stored in history", new Color(120, 190, 255));
        }

        return true;
    }

    private void handleHistoryDoubleClick(AuthHistoryManager.HistoryEntry entry) {
        if (entry == null) return;

        String type = entry.type() == null ? "" : entry.type();

        if ("Offline".equalsIgnoreCase(type) || "LocalAuth".equalsIgnoreCase(type)) {
            mode = AuthMode.OFFLINE;
            offlineName = entry.username();
            focus = InputFocus.OFFLINE;
            startOfflineLogin(entry.username());
            return;
        }

        if ("Microsoft".equalsIgnoreCase(type)) {
            mode = AuthMode.MICROSOFT;
            focus = InputFocus.NONE;

            String token = AuthCredentialStore.getByUuid(entry.uuid());
            if (token == null || token.isBlank()) {
                token = AuthCredentialStore.getByUsername(entry.username());
            }

            if (token != null && !token.isBlank()) {
                startTokenLoginWithSavedToken(token);
            } else {
                startMicrosoftLogin();
            }
            return;
        }

        setStatus("Cannot auto-login", "This history item does not contain reusable credentials", new Color(255, 180, 120));
    }

    private void setStatus(String shortText, String detailText, Color color) {
        this.status = shortText == null ? "" : shortText;
        this.statusDetail = detailText == null ? "" : detailText;
        this.statusColor = color == null ? Color.WHITE : color;
    }

    private void startLoading(String title, String detail) {
        this.loading = true;
        this.loadingTitle = title == null ? "" : title;
        this.loadingDetail = detail == null ? "" : detail;
        this.loadingSpin = 0f;
    }

    private void stopLoading() {
        this.loading = false;
        this.loadingTitle = "";
        this.loadingDetail = "";
    }

    private void drawLoadingOverlay(float delta) {
        loadingSpin += delta * 7.5f;

        RenderHelper.drawRoundedRect(0, 0, width, height, new Color(0, 0, 0, 120), 0);

        float boxW = 190;
        float boxH = 112;
        float x = (width - boxW) / 2f;
        float y = (height - boxH) / 2f;

        RenderHelper.drawRoundedRect(x, y, boxW, boxH, new Color(15, 16, 22, 240), 16);
        RenderHelper.drawRoundedRect(x + 1, y + 1, boxW - 2, boxH - 2, new Color(255, 255, 255, 8), 15);

        float cx = x + boxW / 2f;
        float cy = y + 36f;

        drawSpinner(cx, cy, 16f);

        FontManager.semibold.drawString(13, loadingTitle, x + 22, y + 66, Color.WHITE, false);
        FontManager.semibold.drawString(11, loadingDetail, x + 22, y + 84, new Color(255, 255, 255, 150), false);
    }

    private void drawSpinner(float cx, float cy, float radius) {
        Color accent = Theme.getCurrentTheme().getColors().first;

        RenderHelper.drawCircleOutline(cx, cy, radius, new Color(255, 255, 255, 35), 2.8f);

        int dots = 12;
        for (int i = 0; i < dots; i++) {
            float a = (float) (Math.toRadians((360.0 / dots) * i) + loadingSpin);
            float px = cx + (float) Math.cos(a) * radius;
            float py = cy + (float) Math.sin(a) * radius;

            int alpha = (int) (255f * (i + 1) / dots);
            alpha = Math.min(255, Math.max(25, alpha));

            RenderHelper.drawCircle(px, py, 1.8f, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
        }
    }

    private void startMicrosoftLogin() {
        if (waitingMicrosoft || loading) return;

        waitingMicrosoft = true;
        microsoftUserCode = "";
        microsoftVerificationUri = "";
        microsoftVerificationUriComplete = "";
        startLoading("Microsoft Login", "Fetching device code...");
        setStatus("Fetching device code", "Requesting Microsoft device code...", Color.WHITE);

        CompletableFuture
                .supplyAsync(AuthManager::loginMicrosoftStart)
                .thenAccept(start -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    Runnable ui = () -> {
                        if (start == null) {
                            setStatus("Login failed", "Microsoft login could not be started", new Color(255, 120, 120));
                            waitingMicrosoft = false;
                            stopLoading();
                            return;
                        }

                        this.microsoftUserCode = start.userCode() == null ? "" : start.userCode();
                        this.microsoftVerificationUri = start.verificationUri() == null ? "" : start.verificationUri();
                        this.microsoftVerificationUriComplete = start.verificationUriComplete() == null ? "" : start.verificationUriComplete();

                        if (client != null && microsoftUserCode != null && !microsoftUserCode.isBlank()) {
                            client.keyboard.setClipboard(microsoftUserCode);
                        }

                        String detail;
                        if (microsoftUserCode != null && !microsoftUserCode.isBlank()) {
                            detail = "Enter the code in Microsoft login: " + microsoftUserCode + " (copied to clipboard)";
                        } else {
                            detail = "Please complete Microsoft authorization manually.";
                        }

                        setStatus("Please complete Microsoft login", detail, new Color(120, 190, 255));

                        start.future().thenAccept(result -> {
                            MinecraftClient c = MinecraftClient.getInstance();

                            Runnable uis = () -> {
                                if (result.success()) {
                                    AuthManager.applySessionIfNeeded(result, "Microsoft");
                                    setStatus("Login successful", result.message(), new Color(120, 255, 140));

                                    if (this.client != null) {
                                        this.client.setScreen(parent);
                                    }
                                } else {
                                    setStatus("Login failed", result.message(), new Color(255, 120, 120));
                                }
                                waitingMicrosoft = false;
                                stopLoading();
                            };

                            if (c != null) {
                                c.execute(uis);
                            } else {
                                uis.run();
                            }
                        }).exceptionally(e -> {
                            setStatus("Login exception", rootMessage(e), new Color(255, 120, 120));
                            waitingMicrosoft = false;
                            stopLoading();
                            return null;
                        });
                    };

                    if (client != null) {
                        client.execute(ui);
                    } else {
                        ui.run();
                    }
                })
                .exceptionally(e -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    Runnable ui = () -> {
                        setStatus("Login exception", rootMessage(e), new Color(255, 120, 120));
                        waitingMicrosoft = false;
                        stopLoading();
                    };

                    if (client != null) {
                        client.execute(ui);
                    } else {
                        ui.run();
                    }
                    return null;
                });
    }

    private void startTokenLoginWithSavedToken(String token) {
        if (token == null || token.isBlank()) {
            setStatus("Token is empty", "", new Color(255, 120, 120));
            return;
        }
        if (waitingToken || loading) return;

        waitingToken = true;
        startLoading("Microsoft Login", "Signing in with saved refresh token...");
        setStatus("Signing in", "Using saved refresh token...", Color.WHITE);

        AuthManager.loginWithSessionToken(token).thenAccept(result -> {
            if (result.success()) {
                setStatus("Login successful", result.message(), new Color(120, 255, 140));
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        if (this.client != null) this.client.setScreen(parent);
                    });
                }
            } else {
                setStatus("Login failed", result.message(), new Color(255, 120, 120));
            }
            waitingToken = false;
            stopLoading();
        }).exceptionally(e -> {
            setStatus("Login exception", rootMessage(e), new Color(255, 120, 120));
            waitingToken = false;
            stopLoading();
            return null;
        });
    }

    private void startTokenLogin() {
        String token = refreshToken == null ? "" : refreshToken.trim();
        if (token.isEmpty()) {
            setStatus("Token is empty", "", new Color(255, 120, 120));
            return;
        }

        if (waitingToken || loading) return;
        waitingToken = true;

        startLoading("Token Login", "Signing in...");
        setStatus("Signing in with token", "", Color.WHITE);

        AuthManager.loginWithSessionToken(token).thenAccept(result -> {
            if (result.success()) {
                setStatus("Login successful", result.message(), new Color(120, 255, 140));

                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        if (this.client != null) this.client.setScreen(parent);
                    });
                }
            } else {
                setStatus("Login failed", result.message(), new Color(255, 120, 120));
            }
            waitingToken = false;
            stopLoading();
        }).exceptionally(e -> {
            setStatus("Token exception", rootMessage(e), new Color(255, 120, 120));
            waitingToken = false;
            stopLoading();
            return null;
        });
    }

    private void startOfflineLogin(String forcedName) {
        String name = forcedName != null ? forcedName.trim() : (offlineName == null ? "" : offlineName.trim());
        if (name.isEmpty()) {
            setStatus("Username is empty", "", new Color(255, 120, 120));
            return;
        }

        if (waitingOffline || loading) return;
        waitingOffline = true;

        startLoading("Offline Login", "Creating local session...");
        setStatus("Signing in offline", "", Color.WHITE);

        CompletableFuture.supplyAsync(() -> AuthManager.loginOffline(name)).thenAccept(result -> {
            MinecraftClient client = MinecraftClient.getInstance();
            Runnable ui = () -> {
                if (result.success()) {
                    setStatus("Offline login successful", result.message(), new Color(120, 255, 140));

                    if (this.client != null) {
                        this.client.setScreen(parent);
                    }
                } else {
                    setStatus("Offline login failed", result.message(), new Color(255, 120, 120));
                }

                waitingOffline = false;
                stopLoading();
            };

            if (client != null) {
                client.execute(ui);
            } else {
                ui.run();
            }
        }).exceptionally(e -> {
            MinecraftClient client = MinecraftClient.getInstance();
            Runnable ui = () -> {
                setStatus("Offline exception", rootMessage(e), new Color(255, 120, 120));
                waitingOffline = false;
                stopLoading();
            };

            if (client != null) {
                client.execute(ui);
            } else {
                ui.run();
            }
            return null;
        });
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (focus == InputFocus.TOKEN) {
            if (isAllowed(chr)) {
                refreshToken += chr;
                return true;
            }
        } else if (focus == InputFocus.OFFLINE) {
            if (isAllowed(chr)) {
                offlineName += chr;
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.client != null) {
                MinecraftClient client = this.client;
                client.execute(() -> client.setScreen(parent));
            }
            return true;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            pasteIntoFocusedField();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (focus == InputFocus.TOKEN && !refreshToken.isEmpty()) {
                refreshToken = refreshToken.substring(0, refreshToken.length() - 1);
                return true;
            }
            if (focus == InputFocus.OFFLINE && !offlineName.isEmpty()) {
                offlineName = offlineName.substring(0, offlineName.length() - 1);
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (mode == AuthMode.MICROSOFT) startMicrosoftLogin();
            else if (mode == AuthMode.TOKEN) startTokenLogin();
            else startOfflineLogin(null);
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

    private static String rootMessage(Throwable t) {
        if (t == null) return "unknown";
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        String msg = c.getMessage();
        return (msg == null || msg.isBlank()) ? c.toString() : msg;
    }

    private void pasteIntoFocusedField() {
        if (this.client == null) return;

        String clip = this.client.keyboard.getClipboard();
        if (clip == null || clip.isEmpty()) return;

        clip = clip.replace("\r", "").replace("\n", "").trim();
        if (clip.isEmpty()) return;

        if (focus == InputFocus.TOKEN) {
            refreshToken += clip;
        } else if (focus == InputFocus.OFFLINE) {
            offlineName += clip;
        }
    }

    private static String ellipsis(String text, int max) {
        if (text == null) return "";
        String s = text.trim();
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private boolean isAllowed(char chr) {
        return !Character.isISOControl(chr);
    }

    private Color getTypeColor(String type) {
        if (type == null) return new Color(140, 140, 140);
        return switch (type.toLowerCase()) {
            case "microsoft" -> new Color(115, 170, 255);
            case "token" -> new Color(255, 180, 100);
            case "offline" -> new Color(120, 255, 140);
            default -> new Color(180, 180, 180);
        };
    }

    private String fitText(String text, float size, float maxWidth) {
        if (text == null) return "";
        if (FontManager.semibold.getStringWidth(text, size) <= maxWidth) return text;

        String suffix = "...";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = builder + String.valueOf(text.charAt(i)) + suffix;
            if (FontManager.semibold.getStringWidth(next, size) > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
        }
        return builder + suffix;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}