package cn.lazymoon.ingameui.clickgui.values;

import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.ingameui.clickgui.utils.Component;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class BooleanValueComponent extends Component {
    private final BoolValue setting;
    private final Animation enabled = new DecelerateAnimation(250,1);

    public BooleanValueComponent(BoolValue setting) {
        this.setting = setting;
        enabled.setDirection(setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        setHeight(15);
        enabled.setDirection(setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);
        FontManager.semibold.drawString(14, setting.getName(), getX() + 6, getY() + 23, setting.get() ? Theme.getCurrentTheme().getColors().first : new Color(255, 255, 255, 160), false);
        RenderHelper.drawRoundedRect(getX() + 160 - 23 - 6, getY() + 18, 23, 13, setting.get() ? new Color(255, 255, 255,50) : new Color(255, 255, 255,20), 6);
        RenderHelper.drawCircle(getX() + 160 - 23 - 6 + 6.5f + enabled.getOutput().floatValue() * 10, getY() + 18 + 6.5f, 5, setting.get() ? new Color(255, 255, 255,50) : new Color(255, 255, 255,20));
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (RenderUtils.isHovering(getX() + 160 - 23 - 6, getY() + 18, 23, 13,mouseX,mouseY) && mouseButton == 0){
            setting.toggle();
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isVisible() {
        return this.setting.isAvailable();
    }
}
