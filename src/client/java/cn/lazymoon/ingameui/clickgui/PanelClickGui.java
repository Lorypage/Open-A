package cn.lazymoon.ingameui.clickgui;

import cn.lazymoon.Client;
import cn.lazymoon.ingameui.clickgui.page.ConfigPage;
import cn.lazymoon.ingameui.clickgui.page.GuiPageType;
import cn.lazymoon.ingameui.clickgui.page.SearchPage;
import cn.lazymoon.ingameui.clickgui.page.SettingsPage;
import org.lwjgl.glfw.GLFW;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.features.module.impl.render.PostProcessing;
import cn.lazymoon.ingameui.clickgui.panel.CategoryPanel;
import cn.lazymoon.nanovg.gl.States;
import cn.lazymoon.sxmurxy.builders.Builder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.BlurTaskInstance;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.EaseBackIn;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@Getter
@Setter
public class PanelClickGui extends Screen implements InstanceAccess {
    public float x,y,w,h;
    private final Animation openAnimation = new EaseBackIn(400, 1.0, 1.5f);
    private boolean isClosing = false;
    private final List<CategoryPanel> categoryPanels = new ArrayList<>();
    private GuiPageType currentPage = GuiPageType.MODULES;

    private SearchPage searchPage;
    private SettingsPage settingsPage;
    private ConfigPage configPage;

    private String searchText = "";
    private boolean typingSearch = false;

    public PanelClickGui() {
        super(Text.empty());
        Arrays.stream(Category.values()).forEach(moduleCategory -> {
            CategoryPanel panel = new CategoryPanel(moduleCategory);
            if (moduleCategory == Category.Combat) {
                panel.setSelected(true);
            }
            categoryPanels.add(panel);
        });
        searchPage = new SearchPage(this);
        settingsPage = new SettingsPage(this);
        configPage = new ConfigPage(this);
    }

    @Override
    protected void init() {
        if (!isClosing && openAnimation.getDirection() != Direction.FORWARDS) {
            openAnimation.setDirection(Direction.FORWARDS);
            openAnimation.reset();
        }

        Window sr = MinecraftClient.getInstance().getWindow();
        w = 505;
        h = 265;
        x = (float) sr.getScaledWidth() / 2 - w / 2;
        y = (float) sr.getScaledHeight() / 2 - h / 2;
        super.init();
    }

    @Override
    public void close() {
        if (!isClosing && openAnimation.getDirection() != Direction.BACKWARDS) {
            isClosing = true;
            openAnimation.setDirection(Direction.BACKWARDS);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (isClosing && openAnimation.isDone() && openAnimation.getDirection() == Direction.BACKWARDS) {
            isClosing = false;
            mc.setScreen(null);
            return;
        }

        if (ClientUtils.isNull()) {
            return;
        }
        if (mc.currentScreen instanceof MessageScreen) {
            return;
        }
        if (mc.currentScreen instanceof DownloadingTerrainScreen) {
            return;
        }
        float animProgress = openAnimation.getOutput().floatValue();

        RenderSystem.enableCull();
        States.push();
        RenderHelper.beginRender();
        DrawContext drawContext = new DrawContext(mc, mc.gameRenderer.buffers.getEntityVertexConsumers());
        Matrix4f matrix4f = drawContext.getMatrices().peek().getPositionMatrix();

        float settingsScale = settingsPage != null ? settingsPage.getUiScale() : 1.0f;
        float currentScale = (settingsPage != null && settingsPage.isReduceMotion())
                ? settingsScale
                : (0.8f + (0.2f * animProgress)) * settingsScale;

        float currentAlpha = animProgress;

        float centerX = x + w / 2f;
        float centerY = y + h / 2f;

        long vg = RenderHelper.context;

        RenderHelper.translate(vg, centerX, centerY);
        RenderHelper.scale(vg, currentScale, currentScale);
        RenderHelper.translate(vg, -centerX, -centerY);
        RenderHelper.globalAlpha(vg, currentAlpha);

        Window sr = MinecraftClient.getInstance().getWindow();
        float panelRadius = settingsPage != null ? settingsPage.getRoundness() : 25f;

        if (animProgress >= 0.8f && (settingsPage == null || settingsPage.isEnableBlur())) {
            if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
                float smoothness = 5f;
                BuiltBlur blur = Builder.blur()
                        .size(new SizeState(w + smoothness, h + smoothness))
                        .radius(new QuadRadiusState(panelRadius))
                        .blurRadius(20)
                        .smoothness(smoothness)
                        .color(QuadColorState.TRANSPARENT)
                        .position(new PositionState(x - smoothness / 2f, y - smoothness / 2f))
                        .matrix4f(matrix4f)
                        .build();
                BlurTaskInstance.addTask(blur);
            }
        }
        int panelAlpha = settingsPage != null ? (int) settingsPage.getPanelOpacity() : 120;

        if (PostProcessing.bloom.get()) {
            if (settingsPage == null || settingsPage.isEnableBloom()) {
                RenderHelper.drawRoundRectBloomApple(x, y, w, h, 25, 1, new Color(255, 255, 255, 10));
            }
        }
        RenderHelper.drawGradientAppleRoundedRectLR(x, y, w, h, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),25);

        RenderHelper.scissorStart(x, y, 150, h);
        RenderHelper.drawAppleRoundedRect(x, y, w, h, new Color(255, 255, 255, 10), 25);
        RenderHelper.scissorEnd();

        RenderHelper.drawRoundRectBloom(x + 12, y + 12, 72, 20, new Color(255, 255, 255, 10), 10);
        RenderHelper.drawRoundedRect(x + 12, y + 12, 72, 20, new Color(255, 255, 255, 40), 10);
        FontManager.icon.drawGlowString(26, "A", x + 21.5f, y + 14.5f, InterFace.color(12), InterFace.color(12), false, 1);
        FontManager.medium.drawGlowString(22, "A", x + 36, y + 18, InterFace.color(12), InterFace.color(12), false, 1);
        FontManager.medium.drawGlowString(22, "rcane", x + 44, y + 18, new Color(255, 255, 255), new Color(255, 255, 255), false, 1);

        RenderHelper.drawRoundRectBloom(x + 90, y + 12, 50, 20, new Color(255, 255, 255, 10), 10);
        RenderHelper.drawRoundedRect(x + 90, y + 12, 50, 20, new Color(255, 255, 255, 40), 10);
        FontManager.medium.drawGlowString(16, "NextGen", x + 99, y + 19, Theme.getCurrentTheme().getColors().first,Theme.getCurrentTheme().getColors().first, false, 1);

        Color searchColor = currentPage == GuiPageType.SEARCH
                ? new Color(255, 255, 255, 40)
                : new Color(255, 255, 255, 10);

        RenderHelper.drawRoundedRect(x + 162, y + 12, 128, 20, searchColor, 10);

        FontManager.icon.drawGlowString(16, "G",
                x + 174,
                y + 20 - FontManager.icon.getHeight(16) / 2,
                Theme.getCurrentTheme().getColors().first,
                Theme.getCurrentTheme().getColors().first, false, 1);

        String searchDisplay = searchText.isEmpty() && !typingSearch
                ? "Search Modules..."
                : searchText + (typingSearch ? "_" : "");

        FontManager.semibold.drawGlowString(14, searchDisplay,
                x + 174 + 16,
                y + 20 - FontManager.semibold.getHeight(14) / 2,
                searchText.isEmpty() && !typingSearch ? new Color(255,255,255,150) : Color.WHITE,
                searchText.isEmpty() && !typingSearch ? new Color(255,255,255,150) : Color.WHITE,
                false, 1);

        RenderHelper.drawCircle(x + w - 45, y + 22, 10,
                currentPage == GuiPageType.SETTINGS ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 10));
        FontManager.icon.drawGlowString(16, "M", x + w - 49, y + 20 - FontManager.icon.getHeight(16) / 2,
                Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().first, false, 1);

        RenderHelper.drawCircle(x + w - 20, y + 22, 10,
                currentPage == GuiPageType.CONFIGS ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 10));
        FontManager.icon.drawGlowString(16, "I", x + w - 24, y + 20 - FontManager.icon.getHeight(16) / 2,
                Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().first, false, 1);

        for (CategoryPanel categoryPanel : categoryPanels) {
            if (categoryPanel.isSelected() && currentPage == GuiPageType.MODULES) {
                categoryPanel.render(context, mouseX, mouseY, delta);
            }

            if (categoryPanel.isSelected()) {
                RenderHelper.drawRoundRectBloom(x + 12, y + 90 + categoryPanel.getCategory().ordinal() * 25 - 50, 128, 25, new Color(255, 255, 255, 10), 10);
                RenderHelper.drawRoundedRect(x + 12, y + 90 + categoryPanel.getCategory().ordinal() * 25 - 50, 128, 25, new Color(255, 255, 255, 40), 10);

                FontManager.icon.drawGlowString(18, categoryPanel.getCategory().icon
                        , x + 25, y + 88 + 25 / 2 + categoryPanel.getCategory().ordinal() * 25 - FontManager.icon.getHeight(18) / 2 - 50,Theme.getCurrentTheme().getColors().first,Theme.getCurrentTheme().getColors().first,false,1);

                FontManager.medium.drawGlowString(15, categoryPanel.getCategory().name()
                        , x + 42, y + 89 + 25 / 2 + categoryPanel.getCategory().ordinal() * 25 - 50 - FontManager.medium.getHeight(15) / 2, new Color(255, 255, 255, 255), new Color(255, 255, 255, 255),false,1);

                FontManager.medium.drawGlowString(15, Client.INSTANCE.getModuleManager().getModulesInCategory(categoryPanel.getCategory()).size() + ""
                        , x + 125, y + 89 + 25 / 2 + categoryPanel.getCategory().ordinal() * 25 - 50 - FontManager.medium.getHeight(15) / 2, Theme.getCurrentTheme().getColors().first,Theme.getCurrentTheme().getColors().first,false,1);

            } else {
                FontManager.icon.drawStringmiddleY(18, categoryPanel.getCategory().icon
                        , x + 25, y + 88 + 25 / 2 + categoryPanel.getCategory().ordinal() * 25 - 50, new Color(255, 255, 255), false);

                FontManager.medium.drawStringmiddleY(15, categoryPanel.getCategory().name()
                        , x + 42, y + 89 + 25 / 2 + categoryPanel.getCategory().ordinal() * 25 - 50, new Color(255, 255, 255), false);

                FontManager.medium.drawStringmiddleY(15, Client.INSTANCE.getModuleManager().getModulesInCategory(categoryPanel.getCategory()).size() + ""
                        , x + 125, y + 89 + 25 / 2 + categoryPanel.getCategory().ordinal() * 25 - 50, new Color(255, 255, 255), false);
            }
        }

        switch (currentPage) {
            case MODULES -> {
                CategoryPanel selected = getSelected();
                if (selected != null) {
                    selected.render(context, mouseX, mouseY, delta);
                }
            }
            case SEARCH -> searchPage.render(context, mouseX, mouseY, delta);
            case SETTINGS -> settingsPage.render(context, mouseX, mouseY, delta);
            case CONFIGS -> configPage.render(context, mouseX, mouseY, delta);
        }

        RenderHelper.endRender();
        States.pop();
        RenderSystem.disableCull();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (RenderUtils.isHovering(x + 162, y + 12, 128, 20, mouseX, mouseY)) {
                currentPage = GuiPageType.SEARCH;
                typingSearch = true;
                searchPage.resetScroll();
                return true;
            } else {
                typingSearch = false;
            }

            if (RenderUtils.isHovering(x + w - 55, y + 12, 20, 20, mouseX, mouseY)) {
                currentPage = GuiPageType.SETTINGS;
                typingSearch = false;
                return true;
            }

            if (RenderUtils.isHovering(x + w - 30, y + 12, 20, 20, mouseX, mouseY)) {
                currentPage = GuiPageType.CONFIGS;
                typingSearch = false;
                return true;
            }

            boolean categoryClicked = false;
            for (CategoryPanel panel : categoryPanels) {
                if (handleCategoryPanel(panel, mouseX, mouseY)) {
                    categoryClicked = true;
                    currentPage = GuiPageType.MODULES;
                    typingSearch = false;
                    break;
                }
            }
        }

        switch (currentPage) {
            case MODULES -> {
                CategoryPanel selected = getSelected();
                if (selected != null) {
                    selected.mouseClicked(mouseX, mouseY, button);
                }
            }
            case SEARCH -> searchPage.mouseClicked(mouseX, mouseY, button);
            case SETTINGS -> settingsPage.mouseClicked(mouseX, mouseY, button);
            case CONFIGS -> configPage.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        switch (currentPage) {
            case MODULES -> {
                CategoryPanel selected = getSelected();
                if (selected != null) {
                    selected.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                }
            }
            case SEARCH -> searchPage.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            case SETTINGS -> settingsPage.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            case CONFIGS -> configPage.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        switch (currentPage) {
            case MODULES -> {
                CategoryPanel selected = getSelected();
                if (selected != null) {
                    selected.mouseReleased(mouseX, mouseY, button);
                }
            }
            case SEARCH -> searchPage.mouseReleased(mouseX, mouseY, button);
            case SETTINGS -> settingsPage.mouseReleased(mouseX, mouseY, button);
            case CONFIGS -> configPage.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typingSearch) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                typingSearch = false;
                if (searchText.isEmpty()) {
                    currentPage = GuiPageType.MODULES;
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                typingSearch = false;
                return true;
            }
        }

        switch (currentPage) {
            case MODULES -> {
                CategoryPanel selected = getSelected();
                if (selected != null) {
                    selected.keyPressed(keyCode, scanCode, modifiers);
                }
            }
            case SEARCH -> searchPage.keyPressed(keyCode, scanCode, modifiers);
            case SETTINGS -> settingsPage.keyPressed(keyCode, scanCode, modifiers);
            case CONFIGS -> configPage.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typingSearch) {
            if (isAllowedSearchChar(chr)) {
                searchText += chr;
                return true;
            }
        }

        switch (currentPage) {
            case SEARCH -> {
                return searchPage.charTyped(chr, modifiers);
            }
            case SETTINGS -> {
                return settingsPage.charTyped(chr, modifiers);
            }
            case CONFIGS -> {
                return configPage.charTyped(chr, modifiers);
            }
        }

        return super.charTyped(chr, modifiers);
    }

    private boolean isAllowedSearchChar(char chr) {
        return !Character.isISOControl(chr);
    }

    private boolean handleCategoryPanel(CategoryPanel categoryPanel, double mouseX, double mouseY) {
        if (RenderUtils.isHovering(x + 12, y + 90 + categoryPanel.getCategory().ordinal() * 25 - 50, 128, 25,
                mouseX, mouseY)) {
            for (CategoryPanel p : categoryPanels) {
                p.setSelected(false);
            }
            categoryPanel.setSelected(true);
            currentPage = GuiPageType.MODULES;
            return true;
        }
        return false;
    }

    public CategoryPanel getSelected() {
        return categoryPanels.stream().filter(CategoryPanel::isSelected).findAny().orElse(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
