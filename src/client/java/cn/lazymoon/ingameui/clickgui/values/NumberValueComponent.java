package cn.lazymoon.ingameui.clickgui.values;

import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.ingameui.clickgui.utils.Component;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.math.MathUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-28
 */
public class NumberValueComponent extends Component {
    private final NumberValue setting;
    private boolean dragging;
    private final Animation drag = new DecelerateAnimation(250, 1);

    public NumberValueComponent(NumberValue setting) {
        this.setting = setting;
        drag.setDirection(Direction.BACKWARDS);
    }

    private float anim;

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        setHeight(28);
        int w = 122;
        anim = RenderUtils.animate(anim, (float) (w * (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin())), 50);
        float sliderWidth = anim;
        drag.setDirection(dragging ? Direction.FORWARDS : Direction.BACKWARDS);
        FontManager.semibold.drawString(14, setting.getName(), getX() + 6, getY() + 23,new Color(255, 255, 255), false);
        RenderHelper.drawRoundedRect(getX() + 160 - FontManager.semibold.getStringWidth(setting.get().toString(),14) - 6 - 3, getY() + 21 + 12, FontManager.semibold.getStringWidth(setting.get().toString(),14) + 5, 7.5f + 2,new Color(255, 255, 255,20),4);
        FontManager.semibold.drawGlowString(14,setting.get().toString(), getX() + 160 - FontManager.semibold.getStringWidth(setting.get().toString(),14) - 6, getY() + 23 + 12, Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().first ,false,3);
        RenderHelper.drawRoundedRect(getX() + 160 - w - 30, getY() + 25 + 12, w, 2, new Color(255, 255, 255,20),1);
        RenderHelper.drawRoundedRect(getX() + 160 - w - 30, getY() + 25 + 12, sliderWidth, 2, Theme.getCurrentTheme().getColors().first,1);
        RenderHelper.drawCircle(getX() + 160 - w - 30 + sliderWidth, getY() + 25 + 1 + 12, 3, new Color(255, 255, 255,255));
        if (dragging) {
            final double difference = this.setting.getMax() - this.setting
                    .getMin(), //
                    value = this.setting.getMin() + RenderUtils.clamp_double((mouseX - (getX() + 160 - w - 30)) / w, 0, 1) * difference;
            setting.setValue((double) MathUtils.incValue(value, setting.getStep()));
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int w = 122;
        if (RenderUtils.isHovering(getX() + 160 - w - 30, getY() + 22 + 12, w, 8,mouseX, mouseY) && mouseButton == 0) {
            dragging = true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (state == 0){
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isVisible() {
        return setting.isAvailable();
    }
}
