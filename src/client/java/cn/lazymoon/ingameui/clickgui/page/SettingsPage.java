package cn.lazymoon.ingameui.clickgui.page;

import cn.lazymoon.ingameui.clickgui.PanelClickGui;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.animations.impl.EaseOutSine;
import cn.lazymoon.utils.animations.impl.SmoothStepAnimation;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsPage implements IGuiPage {
    private final PanelClickGui parent;

    private final List<SettingItem> items = new ArrayList<>();

    private final Animation pageAnimation = new EaseOutSine(250, 1);
    private Animation scrollAnimation = new SmoothStepAnimation(0, 0, Direction.BACKWARDS);

    private float rawScroll, scroll, maxScroll;

    // settings
    private boolean enableBlur = true;
    private boolean enableBloom = true;
    private boolean reduceMotion = false;

    private float uiScale = 1.00f;
    private float roundness = 25.0f;
    private float panelOpacity = 120.0f;

    public SettingsPage(PanelClickGui parent) {
        this.parent = parent;
        pageAnimation.setDirection(Direction.FORWARDS);

        items.add(new ToggleItem(
                "Enable Blur",
                "Background blur behind the click gui",
                () -> enableBlur,
                v -> enableBlur = v
        ));

        items.add(new ToggleItem(
                "Enable Bloom",
                "Soft glow around panels and cards",
                () -> enableBloom,
                v -> enableBloom = v
        ));

        items.add(new ToggleItem(
                "Reduce Motion",
                "Use gentler transitions and less scaling",
                () -> reduceMotion,
                v -> reduceMotion = v
        ));

        items.add(new SliderItem(
                "UI Scale",
                "Scale of the main click gui panel",
                0.85f, 1.15f,
                () -> uiScale,
                v -> uiScale = v,
                value -> String.format("%.2fx", value)
        ));

        items.add(new SliderItem(
                "Panel Roundness",
                "Corner radius used by the main panel",
                10f, 35f,
                () -> roundness,
                v -> roundness = v,
                value -> String.format("%.0f px", value)
        ));

        items.add(new SliderItem(
                "Panel Opacity",
                "Background transparency of the main panel",
                70f, 180f,
                () -> panelOpacity,
                v -> panelOpacity = v,
                value -> String.format("%.0f", value)
        ));
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        pageAnimation.setDirection(Direction.FORWARDS);

        float contentX = parent.getX() + 162;
        float contentY = parent.getY() + 48;
        float contentW = parent.getW() - 174;
        float contentH = parent.getH() - 60;

        RenderHelper.drawRoundRectBloom(contentX, contentY, contentW, contentH, new Color(255,255,255, 10), 12);
        RenderHelper.drawRoundedRect(contentX, contentY, contentW, contentH, new Color(255,255,255, 40), 12);

        FontManager.semibold.drawGlowString(18, "Settings", contentX + 12, contentY + 10,
                Theme.getCurrentTheme().getColors().first,
                Theme.getCurrentTheme().getColors().first, false, 1);

        FontManager.semibold.drawString(13, "Customize your click gui appearance",
                contentX + 12, contentY + 27, new Color(255, 255, 255, 120), false);

        float listX = contentX + 12;
        float listY = contentY + 42;
        float listW = contentW - 24;
        float listH = contentH - 54;

        RenderHelper.drawRoundedRect(listX, listY, listW, listH, new Color(255,255,255, 10), 10);

        RenderHelper.scissorStart(listX + 4, listY + 4, listW - 8, listH - 8);

        float animatedScroll = getScroll();
        float itemY = listY + 6 + animatedScroll;
        maxScroll = Math.max(0, items.size() * 46 - (listH - 12));

        for (SettingItem item : items) {
            item.x = listX + 6;
            item.y = itemY;
            item.width = listW - 12;
            item.height = 40;
            item.render(guiGraphics, mouseX, mouseY, partialTicks);
            itemY += 46;
        }

        RenderHelper.scissorEnd();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float contentX = parent.getX() + 162;
        float contentY = parent.getY() + 48;
        float contentW = parent.getW() - 174;
        float contentH = parent.getH() - 60;

        float listX = contentX + 12;
        float listY = contentY + 42;
        float listW = contentW - 24;
        float listH = contentH - 54;

        if (RenderUtils.isHovering(listX, listY, listW, listH, mouseX, mouseY)) {
            for (SettingItem item : items) {
                if (item.mouseClicked(mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        boolean handled = false;
        for (SettingItem item : items) {
            handled |= item.mouseReleased(mouseX, mouseY, state);
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float contentX = parent.getX() + 162;
        float contentY = parent.getY() + 48;
        float contentW = parent.getW() - 174;
        float contentH = parent.getH() - 60;

        float listX = contentX + 12;
        float listY = contentY + 42;
        float listW = contentW - 24;
        float listH = contentH - 54;

        if (RenderUtils.isHovering(listX, listY, listW, listH, mouseX, mouseY)) {
            rawScroll += (float) (verticalAmount * 24);
            rawScroll = Math.max(-maxScroll, Math.min(0, rawScroll));
            scrollAnimation = new SmoothStepAnimation(120, rawScroll - scroll, Direction.BACKWARDS);
            return true;
        }

        return false;
    }

    public float getScroll() {
        scroll = (float) (rawScroll - scrollAnimation.getOutput());
        return scroll;
    }

    public boolean isEnableBlur() {
        return enableBlur;
    }

    public boolean isEnableBloom() {
        return enableBloom;
    }

    public boolean isReduceMotion() {
        return reduceMotion;
    }

    public float getUiScale() {
        return uiScale;
    }

    public float getRoundness() {
        return roundness;
    }

    public float getPanelOpacity() {
        return panelOpacity;
    }

    private abstract static class SettingItem {
        protected float x, y, width, height;
        protected final String name;
        protected final String description;
        protected final Animation hoverAnimation = new DecelerateAnimation(120, 1);

        protected SettingItem(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public void render(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = RenderUtils.isHovering(x, y, width, height, mouseX, mouseY);
            hoverAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);

            float hoverValue = hoverAnimation.getOutput().floatValue();

            Color bg = ColorUtils.interpolateColorC(
                    new Color(255,255,255, 40),
                    new Color(255,255,255, 10),
                    hoverValue
            );

            RenderHelper.drawRoundedRect(x, y, width, height, bg, 9);

            FontManager.semibold.drawString(14, name, x + 10, y + 8, Color.WHITE, false);
            FontManager.semibold.drawString(12, description, x + 10, y + 23,
                    new Color(255, 255, 255, 120), false);

            renderControl(ctx, mouseX, mouseY, partialTicks, hoverValue);
        }

        protected abstract void renderControl(DrawContext ctx, int mouseX, int mouseY, float partialTicks, float hover);

        public abstract boolean mouseClicked(double mouseX, double mouseY, int button);

        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return false;
        }
    }

    private interface BoolGetter {
        boolean get();
    }

    private interface BoolSetter {
        void set(boolean value);
    }

    private interface FloatGetter {
        float get();
    }

    private interface FloatSetter {
        void set(float value);
    }

    private interface ValueFormatter {
        String format(float value);
    }

    private static class ToggleItem extends SettingItem {
        private final BoolGetter getter;
        private final BoolSetter setter;
        private final Animation toggleAnimation = new DecelerateAnimation(140, 1);

        public ToggleItem(String name, String description, BoolGetter getter, BoolSetter setter) {
            super(name, description);
            this.getter = getter;
            this.setter = setter;
        }
        private float getSwitchWidth() {
            return 30f;
        }
        private float getSwitchHeight() {
            return 16f;
        }
        private float getSwitchX() {
            return x + width - getSwitchWidth() - 10f;
        }
        private float getSwitchY() {
            return y + (height - getSwitchHeight()) / 2f;
        }
        @Override
        protected void renderControl(DrawContext ctx, int mouseX, int mouseY, float partialTicks, float hover) {
            boolean value = getter.get();
            toggleAnimation.setDirection(value ? Direction.FORWARDS : Direction.BACKWARDS);

            float switchW = getSwitchWidth();
            float switchH = getSwitchHeight();
            float switchX = getSwitchX();
            float switchY = getSwitchY();

            Color accent = Theme.getCurrentTheme().getColors().first;
            Color trackColor = value
                    ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110)
                    : new Color(255, 255, 255, 45);

            RenderHelper.drawRoundedRect(switchX, switchY, switchW, switchH, trackColor, 7);

            float knobRadius = 5f;
            float knobStart = switchX + 7f;
            float knobTravel = switchW - 14f;
            float knobOffset = toggleAnimation.getOutput().floatValue() * knobTravel;
            RenderHelper.drawCircle(knobStart + knobOffset, switchY + switchH / 2f, knobRadius,
                    value ? Color.WHITE : new Color(220, 220, 220, 180));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && RenderUtils.isHovering(getSwitchX(), getSwitchY(), getSwitchWidth(), getSwitchHeight(), mouseX, mouseY)) {
                setter.set(!getter.get());
                return true;
            }
            return false;
        }
    }

    private static class SliderItem extends SettingItem {
        private final float min;
        private final float max;
        private final FloatGetter getter;
        private final FloatSetter setter;
        private final ValueFormatter formatter;

        private boolean dragging;
        private float animatedValue;

        public SliderItem(String name, String description, float min, float max, FloatGetter getter, FloatSetter setter, ValueFormatter formatter) {
            super(name, description);
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.formatter = formatter;
            this.animatedValue = getter.get();
        }

        @Override
        protected void renderControl(DrawContext ctx, int mouseX, int mouseY, float partialTicks, float hover) {
            float sliderX = x + width - 104;
            float sliderY = y + 23;
            float sliderW = 88;
            float sliderH = 3;

            if (dragging) {
                float percent = (float) ((mouseX - sliderX) / sliderW);
                percent = Math.max(0f, Math.min(1f, percent));
                setter.set(min + (max - min) * percent);
            }

            // 更快：几乎贴手
            animatedValue = RenderUtils.animate(animatedValue, getter.get(), 0.55f);

            float percent = (animatedValue - min) / (max - min);
            percent = Math.max(0f, Math.min(1f, percent));

            Color accent = Theme.getCurrentTheme().getColors().first;

            RenderHelper.drawRoundedRect(sliderX, sliderY, sliderW, sliderH, new Color(255, 255, 255, 30), 2);
            RenderHelper.drawRoundedRect(sliderX, sliderY, sliderW * percent, sliderH,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220), 2);
            RenderHelper.drawCircle(sliderX + sliderW * percent, sliderY + 1.5f, 4.2f, Color.WHITE);

            String valueText = formatter.format(getter.get());
            float valueW = FontManager.semibold.getStringWidth(valueText, 12) + 8;
            RenderHelper.drawRoundedRect(sliderX + sliderW - valueW, y + 7, valueW, 12,
                    new Color(255, 255, 255, 16), 5);
            FontManager.semibold.drawString(12, valueText,
                    sliderX + sliderW - valueW + 4, y + 10, accent, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            float sliderX = x + width - 104;
            float sliderY = y + 19;
            float sliderW = 88;
            float sliderH = 10;

            if (button == 0 && RenderUtils.isHovering(sliderX, sliderY, sliderW, sliderH, mouseX, mouseY)) {
                dragging = true;

                float percent = (float) ((mouseX - sliderX) / sliderW);
                percent = Math.max(0f, Math.min(1f, percent));
                setter.set(min + (max - min) * percent);

                return true;
            }

            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0 && dragging) {
                dragging = false;
                return true;
            }
            return false;
        }
    }
}