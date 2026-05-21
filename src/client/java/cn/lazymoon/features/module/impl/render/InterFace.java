package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.Client;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.*;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.render.RenderUtils;

import java.awt.*;

import static cn.lazymoon.utils.color.ColorUtils.astolfoRainbow;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "InterFace",description = "Adjust the colors and other visuals of your client",key = 0, category = Category.Render, hidden = false)
public class InterFace extends Module {
    public static BoolValue noHitSound = new BoolValue("NoHitSound",false);
    public static ModeValue colorMode = new ModeValue("Color Mode","Tenacity", new String[]{"Fade", "Rainbow", "Astolfo", "Dynamic","Tenacity", "Double"});
    public static final NumberValue colorspeed = new NumberValue("Color Speed",() -> colorMode.is("Tenacity") || colorMode.is("Dynamic"), 4, 1, 10, 1);
    public static final NumberValue colorIndex = new NumberValue("Color Separation", () -> colorMode.is("Tenacity"), 1, 1, 50, 1);
    public static ColorValue mainColor = new ColorValue("First Color",() -> !colorMode.is("Rainbow") && !colorMode.is("Astolfo"),new Color(26, 26, 26));
    public static ColorValue secondeColor = new ColorValue("Second Color",() -> colorMode.is("Tenacity") || colorMode.is("Double") || colorMode.is("Fade"),new Color(192, 108, 132));
    public static final ModeValue theme = Theme.getModeSetting("Theme Selection", "CUSTOM_THEME");
    public static BoolValue hidePotion = new BoolValue("Hide Potion HUD", true);

    public static int colors(int tick) {
        return color(tick).getRGB();
    }

    public static Color colorA(int tick) {
        Color textColor = new Color(-1);
        switch (colorMode.get()) {
            case "Fade":
                textColor = ColorUtils.fade(5, tick * 20, new Color(ArrayList.main.get().getRGB()), 1);
                break;
            case "Static":
                textColor = ArrayList.main.get();
                break;
            case "Astolfo" :
                textColor = new Color(ColorUtils.swapAlpha(astolfoRainbow(tick, ArrayList.main.get().getRed(), ArrayList.main.get().getRGB()), 255));
                break;
            case "Rainbow":
                textColor = new Color(RenderUtils.getRainbow(System.currentTimeMillis(), 2000, tick));;
                break;
            case "Tenacity":
                textColor = ColorUtils.interpolateColorsBackAndForth(colorspeed.getValue().intValue(), Client.INSTANCE.getModuleManager().getAllModules().size() * colorIndex.getValue().intValue(), ArrayList.main.get(),ArrayList.sec.get(), false);
                break;
            case "Dynamic":
                textColor = new Color(ColorUtils.swapAlpha(ColorUtils.colorSwitch(ArrayList.main.get(), new Color(ColorUtils.darker(ArrayList.main.get().getRGB(), 0.25F)), 2000.0F, 0, 1 * 10, colorspeed.get()).getRGB(), 255));
                break;
            case "Double":
                tick *= 200;
                textColor = new Color(RenderUtils.colorSwitch(ArrayList.main.get(), ArrayList.sec.get(), 2000, -tick / 40, 75, 2));
                break;
        }
        return textColor;
    }

    public static Color color(int tick) {
        Color textColor = new Color(-1);
        switch (colorMode.get()) {
            case "Fade":
                textColor = ColorUtils.fade(5, tick * 20, new Color(Theme.getCurrentTheme().getColors().first.getRGB()), 1);
                break;
            case "Static":
                textColor = Theme.getCurrentTheme().getColors().first;
                break;
            case "Astolfo" :
                textColor = new Color(ColorUtils.swapAlpha(astolfoRainbow(tick, Theme.getCurrentTheme().getColors().first.getRed(), Theme.getCurrentTheme().getColors().first.getRGB()), 255));
                break;
            case "Rainbow":
                textColor = new Color(RenderUtils.getRainbow(System.currentTimeMillis(), 2000, tick));;
                break;
            case "Tenacity":
                textColor = ColorUtils.interpolateColorsBackAndForth(colorspeed.getValue().intValue(), Client.INSTANCE.getModuleManager().getAllModules().size() * colorIndex.getValue().intValue(), Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().second, false);
                break;
            case "Dynamic":
                textColor = new Color(ColorUtils.swapAlpha(ColorUtils.colorSwitch(Theme.getCurrentTheme().getColors().first, new Color(ColorUtils.darker(Theme.getCurrentTheme().getColors().first.getRGB(), 0.25F)), 2000.0F, 0, 1 * 10, colorspeed.get()).getRGB(), 255));
                break;
            case "Double":
                tick *= 200;
                textColor = new Color(RenderUtils.colorSwitch(Theme.getCurrentTheme().getColors().first, Theme.getCurrentTheme().getColors().second, 2000, -tick / 40, 75, 2));
                break;
        }
        return textColor;
    }
}
