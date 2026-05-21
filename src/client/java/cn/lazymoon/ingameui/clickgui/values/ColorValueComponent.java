package cn.lazymoon.ingameui.clickgui.values;

import cn.lazymoon.features.value.impl.ColorValue;
import cn.lazymoon.ingameui.clickgui.utils.Component;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.EaseOutSine;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * @Author:Guyuemang
 * @Time:02-23
 */
public class ColorValueComponent extends Component {
    private final ColorValue setting;
    private final Animation open = new EaseOutSine(250, 1);
    private boolean opened, pickingHue, pickingOthers, pickingAlpha;

    public ColorValueComponent(ColorValue setting) {
        this.setting = setting;
        open.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        open.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);
        this.setHeight(15 + 65 * open.getOutput().floatValue());
        final float[] hsb = new float[]{setting.getHue(), setting.getSaturation(), setting.getBrightness()};
        final float alpha = setting.getAlpha();

        FontManager.semibold.drawString(14, setting.getName(), getX() + 6, getY() + 23, Theme.getCurrentTheme().getColors().first, false);

        RenderHelper.drawCircle(getX() + 160 - 5 - 5, getY() + 26, 5, setting.get());
        if (opened) {
            RenderHelper.drawGradientRRect3(getX() + 10, getY() + 35, 140, (float) (45 * open.getOutput()), 0,
                    Color.BLACK, Color.WHITE, Color.BLACK, Color.getHSBColor(setting.getHue(), 1, 1));

            for (int max = (int) (137), i = 0; i < max; i++) {
                RenderHelper.drawRect(getX() + i + 10,
                        (float) (getY() + (35 * open.getOutput()) + 48),
                        4, 4, Color.getHSBColor(i / (float) max, 1, 1));
            }

            float alphaSliderY = (float) (getY() + 4 + (35 * open.getOutput()) + 50);
            drawCheckerboard(getX() + 10, alphaSliderY, 137, 4);

            for (int max = (int) (137), i = 0; i < max; i++) {
                float alphaValue = i / (float) max;
                Color alphaColor = new Color(
                        setting.get().getRed(),
                        setting.get().getGreen(),
                        setting.get().getBlue(),
                        (int) (alphaValue * 255)
                );
                RenderHelper.drawRect(getX() + 10 + i, alphaSliderY, 4, 4,
                        (alphaColor));
            }

            float sliderX = getX() + 10;
            float sliderWidth = 140;
            float alphaHandleX = sliderX + (sliderWidth * alpha);
            alphaHandleX = Math.max(sliderX + 4, Math.min(sliderX + sliderWidth - 4, alphaHandleX));
            RenderHelper.drawRect((int) alphaHandleX, (int) alphaSliderY, 2, 4,
                    new Color(209, 211, 215));

            float gradientX = getX() + 10;
            float gradientY = getY() + 35;
            float gradientWidth = 140;
            float gradientHeight = (float) (45 * open.getOutput());

            float pickerY = (gradientY) + (gradientHeight * (1 - hsb[2]));
            float pickerX = (gradientX) + (gradientWidth * hsb[1] - 1);
            pickerY = Math.max(Math.min(gradientY + gradientHeight - 4, pickerY), gradientY - 4);
            pickerX = Math.max(Math.min(gradientX + gradientWidth - 4, pickerX), gradientX - 4);

            if (pickingHue) {
                setting.setHue((float) Math.min(1, Math.max(0, (mouseX - gradientX) / gradientWidth)));
            }

            if (pickingOthers) {
                setting.setBrightness((float) Math.min(1, Math.max(0, 1 - ((mouseY - gradientY) / gradientHeight))));
                setting.setSaturation((float) Math.min(1, Math.max(0, (mouseX - gradientX) / gradientWidth)));
            }

            if (pickingAlpha) {
                float newAlpha = (float) ((mouseX - sliderX) / sliderWidth);
                newAlpha = Math.max(0.0f, Math.min(1.0f, newAlpha));
                setting.setAlpha(newAlpha);
            }

            RenderHelper.drawRect((int) pickerX, (int) pickerY, 2, 2,
                    new Color(255, 255, 255));
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawCheckerboard(float x, float y, float width, float height) {
        RenderHelper.drawRect(x, y, width, height,
                new Color(200, 200, 200));

        int squareSize = 2;
        boolean white = true;
        for (int i = 0; i < width; i += squareSize) {
            for (int j = 0; j < height; j += squareSize) {
                if (!white) {
                    Color color = new Color(150, 150, 150);
                    float drawWidth = Math.min(squareSize, width - i);
                    float drawHeight = Math.min(squareSize, height - j);

                    if (i > 2 && i < width - 2 || j > 0 && j < height - 0) {
                        RenderHelper.drawRect(x + i, y + j, drawWidth, drawHeight,
                                color);
                    }
                }
                white = !white;
            }
            if (height / squareSize % 2 == 0) {
                white = !white;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (RenderUtils.isHovering(getX() + 160 - 5 - 10, getY() + 20, 10, 10, mouseX, mouseY)) {
            opened = !opened;
        }
        if (opened) {
            if (RenderUtils.isHovering(getX() + 10, getY() + 35, 140, (float) (45 * open.getOutput()), mouseX, mouseY)) {
                pickingOthers = true;
            }
            if (RenderUtils.isHovering(getX() + 10,
                    (float) (getY() + (35 * open.getOutput()) + 48), 137, 4, mouseX, mouseY)) {
                pickingHue = true;
            }

            float alphaSliderY = (float) (getY() + 4 + (35 * open.getOutput()) + 50);
            if (RenderUtils.isHovering(getX() + 10, alphaSliderY, 137, 4, mouseX, mouseY)) {
                pickingAlpha = true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (state == 0) {
            pickingHue = false;
            pickingOthers = false;
            pickingAlpha = false;
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean isVisible() {
        return setting.isAvailable();
    }
}
