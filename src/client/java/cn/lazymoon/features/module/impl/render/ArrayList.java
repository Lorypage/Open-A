package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ColorValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.ingameui.ui.ModuleWidget;
import cn.lazymoon.sxmurxy.builders.Builder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.BlurTaskInstance;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import cn.lazymoon.utils.render.font.FontRenderer;
import cn.lazymoon.features.module.Module;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Comparator;

import static cn.lazymoon.features.module.impl.render.InterFace.colorIndex;
import static cn.lazymoon.features.module.impl.render.InterFace.colorspeed;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "ArrayList",description = "Shou you Module List on you game screen",key = 0,category = Category.Render,hidden = false)
public class ArrayList extends ModuleWidget {
    public static BoolValue importantModules = new BoolValue("Important", false);
    public ModeValue textShadow = new ModeValue("Text Shadow", "None", new String[]{"Black", "Colored", "None"});
    public NumberValue glow = new NumberValue("Glow Size",() -> textShadow.is("Colored"),3,1,10,1);
    public ModeValue fontmode = new ModeValue("Font", "semibold", new String[]{"semibold","medium","regular","light","thin","ultralight"});

    public ModeValue misc = new ModeValue("Rectangle", "None", new String[]{"None", "Top", "Side"});
    public static BoolValue background = new BoolValue("Back Ground", false);
    public static ColorValue main = new ColorValue("Suffix First Color",new Color(75, 186, 255));
    public static ColorValue sec = new ColorValue("Suffix Seconde Color",new Color(169, 198, 255));
    public final ModeValue tags = new ModeValue("Suffix", "None", new String[]{"None", "Simple", "Bracket", "Dash"});

    public NumberValue fontsize = new NumberValue("Font Size",16,1,60,1);
    public NumberValue arrayCount = new NumberValue("Array Count", 2, -50, 50, 0.1);
    public NumberValue fontCount = new NumberValue("FontCount", 0.00, -60, 60, 0.1);
    public NumberValue arrayHeight = new NumberValue("ArrayHeight",()-> background.get(), 16, 1.0, 60, 0.1);
    public NumberValue rectangleHeight = new NumberValue("RectangleHeight",21, 1.0, 60, 0.1);
    public NumberValue radius = new NumberValue("Radius", 50, 0, 60, 0.5);

    FontRenderer fontManager = FontManager.semibold;
    @Override
    public void render(RenderNvgEvent event) {
        switch (fontmode.get()) {
            case "semibold":
                fontManager = FontManager.semibold;
                break;
            case "medium":
                fontManager = FontManager.medium;
                break;
            case "regular":
                fontManager = FontManager.regular;
                break;
            case "thin":
                fontManager = FontManager.thin;
                break;
            case "ultralight":
                fontManager = FontManager.ultralight;
                break;
            case "light":
                fontManager = FontManager.light;
        }
        int posX = (int) renderX;
        int posY = (int) renderY;
        draw(event.matrix4f(),posX,posY);
    }

    public void draw(Matrix4f matrix4f,int posX,int posY) {
        java.util.ArrayList<Module> enabledMods = getModuleArrayList(fontManager);

        int count = 0;
        int counts = 0;

        for (Module module : enabledMods) {
            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isState() ? Direction.FORWARDS : Direction.BACKWARDS);

            if (module.isHidden()) continue;

            if (!module.isState() && moduleAnimation.finished(Direction.BACKWARDS)) continue;

            if (importantModules.get()){
                if (module.getCategory() == Category.Visual) continue;
                if (module.getCategory() == Category.Render) continue;
            }

            boolean flip = renderX + width / 2 <= window.getScaledWidth() / 2f;
            String displayText = module.getName();
            String suffix = module.getSuffix();

            float x = flip ? (posX) : posX + width - fontManager.getStringWidth(displayText + suffix,fontsize.get().byteValue());
            int y = posY + count;

            int w = (int) (fontManager.getStringWidth(displayText + suffix,fontsize.get().byteValue())) + 12;

            if (flip) {
                x -= (int) Math.abs((moduleAnimation.getOutput() - 1.0) * (12.0 + fontManager.getStringWidth(displayText + suffix,fontsize.get().byteValue())));
            } else {
                x += (int) Math.abs((moduleAnimation.getOutput() - 1.0) * (12.0 + fontManager.getStringWidth(displayText + suffix,fontsize.get().byteValue())));
            }

            switch (misc.getValue()) {
                case "Top":
                    if (count == 0) {
                        RenderHelper.drawAppleRoundedRect(x - 6, y - 7, w, 2,InterFace.color(12),0);
                    }
                    break;
                case "Side":
                    if (flip) {
                        RenderHelper.drawAppleRoundedRect(x - 6, y - 5, 2, rectangleHeight.get().floatValue(),InterFace.color(12),0);
                    }else {
                        RenderHelper.drawAppleRoundedRect(x - 6 + w, y - 5, 2, rectangleHeight.get().floatValue(),InterFace.color(12),0);
                    }
                    break;
                default:
                    break;
            }

            if (background.get()) {
                float smoothness = 5f;
                if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
                    BuiltBlur blur = Builder.blur().size(new SizeState(w + smoothness, arrayHeight.get().floatValue() + smoothness)).
                            radius(new QuadRadiusState(radius.get().floatValue())).blurRadius(20).smoothness(smoothness).
                            color(QuadColorState.TRANSPARENT).position(new PositionState(x - 6 - smoothness / 2f, y - 5 - smoothness / 2f)).matrix4f(matrix4f).build();
                    BlurTaskInstance.addTask(blur);
                }

                if (PostProcessing.bloom.get()) {
                    RenderHelper.drawRoundRectBloomApple(x - 6, y - 5, w, arrayHeight.get().floatValue(), radius.get().floatValue(),2, new Color(255, 255, 255, 10));
                }
                RenderHelper.drawGradientAppleRoundedRectLR(x - 6, y - 5, w, arrayHeight.get().floatValue(), new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),radius.get().floatValue());
            }

            int index = (int) (counts * colorIndex.getValue());
            int textcolor = InterFace.colors(1);
            int textcolor2 = InterFace.colorA(1).getRGB();
            if (InterFace.colorMode.is("Tenacity")) {
                textcolor = ColorUtils.interpolateColorsBackAndForth(colorspeed.getValue().intValue(), index, Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().second, false).getRGB();
                textcolor2 = ColorUtils.interpolateColorsBackAndForth(colorspeed.getValue().intValue(), index, main.get(), sec.get(), false).getRGB();
            }

            switch (textShadow.get()) {
                case "None":
                    fontManager.drawString(fontsize.get().byteValue(), displayText, x, y + fontCount.get().intValue(), new Color(textcolor), false);
                    fontManager.drawString(fontsize.get().byteValue(), suffix, x + fontManager.getStringWidth(displayText, fontsize.get().byteValue()), y + fontCount.get().intValue(), new Color(textcolor2), false);
                break;
                case "Black":
                    fontManager.drawString(fontsize.get().byteValue(), displayText, x, y + fontCount.get().intValue(), new Color(textcolor), true);
                    fontManager.drawString(fontsize.get().byteValue(), suffix, x + fontManager.getStringWidth(displayText, fontsize.get().byteValue()), y + fontCount.get().intValue(), new Color(textcolor2), false);
                    break;
                case "Colored":
                    fontManager.drawGlowString(fontsize.get().byteValue(), displayText, x, y + fontCount.get().intValue(), new Color(textcolor), new Color(textcolor), false,glow.get().byteValue());
                    fontManager.drawGlowString(fontsize.get().byteValue(), suffix, x + fontManager.getStringWidth(displayText, fontsize.get().byteValue()), y + fontCount.get().intValue(), new Color(textcolor2), new Color(textcolor2), false,glow.get().byteValue());
                    break;
            }

            counts ++;
            count += (int) (moduleAnimation.getOutput() * (this.arrayCount.get().floatValue() * arrayHeight.get().floatValue()));
            this.height = count;
        }
        this.width = 152;
    }

    private java.util.ArrayList<Module> getModuleArrayList(FontRenderer string) {
        Comparator<Module> sort = (m1, m2) -> {
            double module1 = string.getStringWidth(m1.getName() + m1.getSuffix(),fontsize.get().byteValue());
            double module2 = string.getStringWidth(m2.getName() + m2.getSuffix(),fontsize.get().byteValue());
            return Double.compare(module2, module1);
        };
        java.util.ArrayList<Module> enabledMods = new java.util.ArrayList<>(Client.INSTANCE.getModuleManager().getAllModules());
        enabledMods.sort(sort);
        return enabledMods;
    }

    @Override
    public boolean shouldRender() {
        return isState();
    }
}
