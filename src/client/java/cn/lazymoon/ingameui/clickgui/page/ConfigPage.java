package cn.lazymoon.ingameui.clickgui.page;

import cn.lazymoon.Client;
import cn.lazymoon.ingameui.clickgui.PanelClickGui;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ConfigPage implements IGuiPage {
    private final PanelClickGui parent;

    private String configName = "";
    private String selectedConfig = "";
    private boolean typingName;

    private float rawScroll, scroll, maxScroll;

    public ConfigPage(PanelClickGui parent) {
        this.parent = parent;
    }

    public List<String> getConfigs() {
        File[] files = Client.INSTANCE.getConfigManager().getConfigDir().listFiles(
                f -> f.isFile() && f.getName().toLowerCase().endsWith(".json")
        );

        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .map(File::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private String normalize(String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String n = name.trim();
        return n.toLowerCase().endsWith(".json") ? n : n + ".json";
    }

    private void saveCurrent() {
        String target = !configName.trim().isEmpty() ? configName.trim() : selectedConfig;
        target = normalize(target);
        if (!target.isEmpty()) {
            Client.INSTANCE.getConfigManager().saveConfig(target);
            selectedConfig = target;
        }
    }

    private void loadCurrent() {
        String target = !configName.trim().isEmpty() ? configName.trim() : selectedConfig;
        target = normalize(target);
        if (!target.isEmpty()) {
            Client.INSTANCE.getConfigManager().loadConfig(target, true);
            selectedConfig = target;
        }
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float contentX = parent.getX() + 162;
        float contentY = parent.getY() + 48;
        float contentW = parent.getW() - 174;
        float contentH = parent.getH() - 60;

        RenderHelper.drawRoundRectBloom(contentX, contentY, contentW, contentH, new Color(255,255,255, 10), 12);
        RenderHelper.drawRoundedRect(contentX, contentY, contentW, contentH, new Color(255,255,255, 40), 12);

        FontManager.semibold.drawGlowString(18, "Configs", contentX + 12, contentY + 10,
                Theme.getCurrentTheme().getColors().first,
                Theme.getCurrentTheme().getColors().first, false, 1);

        FontManager.semibold.drawString(13, "Manage your local configs", contentX + 12, contentY + 27,
                new Color(255, 255, 255, 120), false);

        float inputX = contentX + 12;
        float inputY = contentY + 42;
        float inputW = contentW - 24;
        float inputH = 20;

        RenderHelper.drawRoundedRect(inputX, inputY, inputW, inputH,
                typingName ? new Color(255,255,255, 40) : new Color(255,255,255, 10), 8);

        String display = configName.isEmpty() ? "Config name..." : configName + (typingName ? "_" : "");
        FontManager.semibold.drawString(14, display, inputX + 8, inputY + 6,
                configName.isEmpty() ? new Color(255,255,255,120) : Color.WHITE, false);

        float buttonY = inputY + 28;
        float buttonW = (inputW - 8) / 2f;

        RenderHelper.drawRoundRectBloom(inputX, buttonY, buttonW, 18, new Color(255,255,255, 10), 8);
        RenderHelper.drawRoundedRect(inputX, buttonY, buttonW, 18, new Color(255,255,255, 40), 8);
        FontManager.semibold.drawGlowString(14, "Save Config", inputX + buttonW / 2f - 28, buttonY + 5,
                Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().first, false, 1);

        float loadX = inputX + buttonW + 8;
        RenderHelper.drawRoundRectBloom(loadX, buttonY, buttonW, 18, new Color(255,255,255, 10), 8);
        RenderHelper.drawRoundedRect(loadX, buttonY, buttonW, 18, new Color(255,255,255, 40), 8);
        FontManager.semibold.drawGlowString(14, "Load Config", loadX + buttonW / 2f - 28, buttonY + 5,
                Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().first, false, 1);

        float listX = contentX + 12;
        float listY = buttonY + 28;
        float listW = contentW - 24;
        float listH = contentH - (listY - contentY) - 12;

        RenderHelper.drawRoundedRect(listX, listY, listW, listH, new Color(255,255,255, 40), 10);

        List<String> configs = getConfigs();
        RenderHelper.scissorStart(listX, listY, listW, listH);

        float offsetY = scroll;
        maxScroll = Math.max(0, configs.size() * 22 - (listH - 8));

        for (String config : configs) {
            float rowX = listX + 6;
            float rowY = listY + 6 + offsetY;
            float rowW = listW - 12;
            float rowH = 18;

            boolean selected = config.equalsIgnoreCase(selectedConfig);

            RenderHelper.drawRoundedRect(rowX, rowY, rowW, rowH,
                    selected ? new Color(255,255,255, 40) : new Color(255,255,255, 10), 7);

            FontManager.semibold.drawString(14, config, rowX + 8, rowY + 5,
                    selected ? Theme.getCurrentTheme().getColors().first : Color.WHITE, false);

            offsetY += 22;
        }

        if (configs.isEmpty()) {
            FontManager.semibold.drawString(14, "No configs found", listX + 10, listY + 10, new Color(255,255,255,120), false);
        }

        RenderHelper.scissorEnd();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float contentX = parent.getX() + 162;
        float contentY = parent.getY() + 48;
        float contentW = parent.getW() - 174;
        float contentH = parent.getH() - 60;

        float inputX = contentX + 12;
        float inputY = contentY + 42;
        float inputW = contentW - 24;
        float inputH = 20;

        typingName = RenderUtils.isHovering(inputX, inputY, inputW, inputH, mouseX, mouseY);

        float buttonY = inputY + 28;
        float buttonW = (inputW - 8) / 2f;

        if (mouseButton == 0) {
            if (RenderUtils.isHovering(inputX, buttonY, buttonW, 18, mouseX, mouseY)) {
                saveCurrent();
                return true;
            }

            float loadX = inputX + buttonW + 8;
            if (RenderUtils.isHovering(loadX, buttonY, buttonW, 18, mouseX, mouseY)) {
                loadCurrent();
                return true;
            }

            float listX = contentX + 12;
            float listY = buttonY + 28;
            float listW = contentW - 24;
            float listH = contentH - (listY - contentY) - 12;

            if (RenderUtils.isHovering(listX, listY, listW, listH, mouseX, mouseY)) {
                float offsetY = scroll;
                for (String config : getConfigs()) {
                    float rowX = listX + 6;
                    float rowY = listY + 6 + offsetY;
                    float rowW = listW - 12;
                    float rowH = 18;

                    if (RenderUtils.isHovering(rowX, rowY, rowW, rowH, mouseX, mouseY)) {
                        selectedConfig = config;
                        configName = config.replace(".json", "");
                        return true;
                    }

                    offsetY += 22;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float contentX = parent.getX() + 162;
        float contentY = parent.getY() + 48;
        float contentW = parent.getW() - 174;
        float contentH = parent.getH() - 60;

        float listX = contentX + 12;
        float listY = contentY + 42 + 28 + 28;
        float listW = contentW - 24;
        float listH = contentH - (listY - contentY) - 12;

        if (RenderUtils.isHovering(listX, listY, listW, listH, mouseX, mouseY)) {
            rawScroll += (float) (verticalAmount * 20);
            rawScroll = Math.max(-maxScroll, Math.min(0, rawScroll));
            scroll = rawScroll;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!typingName) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!configName.isEmpty()) {
                configName = configName.substring(0, configName.length() - 1);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveCurrent();
            typingName = false;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            typingName = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!typingName) return false;

        if (isAllowed(chr)) {
            configName += chr;
            return true;
        }
        return false;
    }

    private boolean isAllowed(char chr) {
        return Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '.' || chr == ' ';
    }
}