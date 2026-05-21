package cn.lazymoon.ingameui.clickgui.values;

import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.ingameui.clickgui.utils.Component;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.animations.impl.SmoothStepAnimation;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-28
 */
public class ModeValueComponent extends Component {
    private final ModeValue setting;
    private float maxScroll = Float.MAX_VALUE, rawScroll, scroll;
    private Animation scrollAnimation = new SmoothStepAnimation(0, 0, Direction.BACKWARDS);
    private final Animation open = new DecelerateAnimation(175, 1);
    private boolean opened;

    public ModeValueComponent(ModeValue setting) {
        this.setting = setting;
        open.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        setHeight(15);
        open.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);

        FontManager.semibold.drawString(14, setting.getName(), getX() + 6, getY() + 23, new Color(255, 255, 255), false);

        float offset = 0;
        float heightoff = 0;

        for (String text : setting.getModes()) {
            float off = FontManager.semibold.getStringWidth(text,14) + 5;
            if (offset + off >= (154 - 6)) {
                offset = 0;
                heightoff += 13;
            }
            float finalOffset = offset;
            float finalHeightoff = heightoff;
            if (text.equals(setting.get())) {
                RenderHelper.drawRoundedRect(getX() + 12 + finalOffset - 3, getY() + 37 + finalHeightoff - 3.5f, FontManager.semibold.getStringWidth(text, 14) + 5, 13,new Color(255, 255, 255,20), 6);
                FontManager.semibold.drawGlowString(14, text, getX() + 12 + finalOffset, getY() + 37 + finalHeightoff, Theme.getCurrentTheme().getColors().first , Theme.getCurrentTheme().getColors().first, false,3);
            }else FontManager.semibold.drawString(14, text, getX() + 12 + finalOffset, getY() + 37 + finalHeightoff, new Color(255, 255, 255), false);
            offset += off;
        }

        setHeight(32 + heightoff);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float offset = 0;
        float heightoff = 0;
        for (String text : setting.getModes()) {
            float off = FontManager.semibold.getStringWidth(text,14) + 4;
            if (offset + off >= (154 - 6)) {
                offset = 0;
                heightoff += 13;
            }
            float finalOffset = offset;
            float finalHeightoff = heightoff;
            if (RenderUtils.isHovering(getX() + 12 + finalOffset - 3, getY() + 37 + finalHeightoff - 3.5f, FontManager.semibold.getStringWidth(text, 14) + 5, 13, mouseX, mouseY) && mouseButton == 0) {
                setting.set(text);
            }
            offset += off;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return setting.isAvailable();
    }
}