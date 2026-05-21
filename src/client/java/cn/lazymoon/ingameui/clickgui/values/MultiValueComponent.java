package cn.lazymoon.ingameui.clickgui.values;

import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.ingameui.clickgui.utils.Component;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.EaseOutSine;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-28
 */
public class MultiValueComponent extends Component {
    private final MultiBoolValue setting;
    private final Map<BoolValue, EaseOutSine> select = new HashMap<>();

    public MultiValueComponent(MultiBoolValue setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float offset = 8;
        float heightoff = 0;

        FontManager.semibold.drawString(14, setting.getName(), getX() + 6, getY() + 23, new Color(255, 255, 255), false);

        for (BoolValue boolValue : setting.getValues()) {
            float off = FontManager.semibold.getStringWidth(boolValue.getName(),14) + 5;
            if (offset + off >= (154 - 6)) {
                offset = 4;
                heightoff += 12.5f;
            }
            select.putIfAbsent(boolValue, new EaseOutSine(250, 1));
            select.get(boolValue).setDirection(boolValue.get() ? Direction.FORWARDS : Direction.BACKWARDS);

            float finalOffset = offset;
            float finalHeightoff = heightoff;
            if (boolValue.get()) {
                RenderHelper.drawRoundedRect(getX() + 12 + finalOffset - 3, getY() + 37 + finalHeightoff - 3.5f, FontManager.semibold.getStringWidth(boolValue.getName(), 14) + 5, 13,new Color(255, 255, 255,20), 6);
                FontManager.semibold.drawGlowString(14, boolValue.getName(), getX() + 12 + finalOffset, getY() + 37 + finalHeightoff, Theme.getCurrentTheme().getColors().first , Theme.getCurrentTheme().getColors().first, false,3);
            }else FontManager.semibold.drawString(14, boolValue.getName(), getX() + 12 + finalOffset, getY() + 37 + finalHeightoff, new Color(255, 255, 255), false);
            offset += off;
        }

        setHeight(32 + heightoff);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float offset = 8;
        float heightoff = 0;
        for (BoolValue boolValue : setting.getValues()) {
            float off = FontManager.semibold.getStringWidth(boolValue.getName(),14) + 5;
            if (offset + off >= (154 - 6)) {
                offset = 4;
                heightoff += 12.5f;
            }
            float finalOffset = offset;
            float finalHeightoff = heightoff;
            if (RenderUtils.isHovering(getX() + 12 + finalOffset - 3, getY() + 37 + finalHeightoff - 3.5f, FontManager.semibold.getStringWidth(boolValue.getName(), 14) + 5, 13, mouseX, mouseY) && mouseButton == 0) {
                boolValue.set(!boolValue.get());
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
