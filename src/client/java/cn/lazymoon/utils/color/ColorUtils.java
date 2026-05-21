package cn.lazymoon.utils.color;

import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.math.MathUtils;

import java.awt.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class ColorUtils implements InstanceAccess {
    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static Color hsvToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255);
        } else {
            float h = hue * 6;
            int i = (int) Math.floor(h);
            float f = h - i;
            float p = brightness * (1 - saturation);
            float q = brightness * (1 - saturation * f);
            float t = brightness * (1 - saturation * (1 - f));

            switch (i) {
                case 0:
                    r = (int) (brightness * 255);
                    g = (int) (t * 255);
                    b = (int) (p * 255);
                    break;
                case 1:
                    r = (int) (q * 255);
                    g = (int) (brightness * 255);
                    b = (int) (p * 255);
                    break;
                case 2:
                    r = (int) (p * 255);
                    g = (int) (brightness * 255);
                    b = (int) (t * 255);
                    break;
                case 3:
                    r = (int) (p * 255);
                    g = (int) (q * 255);
                    b = (int) (brightness * 255);
                    break;
                case 4:
                    r = (int) (t * 255);
                    g = (int) (p * 255);
                    b = (int) (brightness * 255);
                    break;
                case 5:
                    r = (int) (brightness * 255);
                    g = (int) (p * 255);
                    b = (int) (q * 255);
                    break;
            }
        }
        return new Color(r, g, b);
    }

    public static ColorPanel colorToColorPanel(Color color) {
        return ColorPanel.createColorPanel((float) color.getRed() / 255, (float) color.getGreen() / 255, (float) color.getBlue() / 255, (float) color.getAlpha() / 255);
    }

    public static Color reAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color reAlpha2(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathUtils.clamp(alpha, 0, 255));
    }

    public static Color brighter(Color color, float FACTOR) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int alpha = color.getAlpha();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;

        return new Color(Math.min((int) (r / FACTOR), 255),
                Math.min((int) (g / FACTOR), 255),
                Math.min((int) (b / FACTOR), 255),
                alpha);
    }

    public static int astolfoRainbow(final int offset, final float saturation, final float brightness) {
        double currentColor = Math.ceil((double) (System.currentTimeMillis() + offset * 20L)) / 6.0;
        return Color.getHSBColor(((float) ((currentColor %= 360.0) / 360.0) < 0.5) ? (-(float) (currentColor / 360.0)) : ((float) (currentColor / 360.0)), saturation, brightness).getRGB();
    }

    public static int applyOpacity(int color, float opacity) {
        Color old = new Color(color);
        return applyOpacity(old, opacity).getRGB();
    }
    public static Color applyOpacity3(int color, float opacity) {
        Color old = new Color(color);
        return applyOpacity(old, opacity);
    }

    public static Color fade(int speed, int index, Color color, float alpha) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;

        Color colorHSB = new Color(Color.HSBtoRGB(hsb[0], hsb[1], angle / 360f));

        return new Color(colorHSB.getRed(), colorHSB.getGreen(), colorHSB.getBlue(), Math.max(0, Math.min(255, (int) (alpha * 255))));
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? ColorUtils.interpolateColorHue(start, end, angle / 360f) : ColorUtils.interpolateColorC(start, end, angle / 360f);
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue){
        return interpolate(oldValue, newValue, (float) interpolationValue).intValue();
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue){
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue){
        return interpolate(oldValue, newValue, (float) interpolationValue).floatValue();
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount),
                interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        return ColorUtils.applyOpacity(resultColor, interpolateInt(color1.getAlpha(), color2.getAlpha(), amount) / 255f);
    }

    public static Color darker(Color color, float FACTOR) {
        return new Color(Math.max((int) (color.getRed() * FACTOR), 0),
                Math.max((int) (color.getGreen() * FACTOR), 0),
                Math.max((int) (color.getBlue() * FACTOR), 0),
                color.getAlpha());
    }

    public static int darker(int color, float factor) {
        int r = (int) ((color >> 16 & 0xFF) * factor);
        int g = (int) ((color >> 8 & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        int a = color >> 24 & 0xFF;
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF | (a & 0xFF) << 24;
    }

    public static int colorSwitch2(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed) {
        return colorSwitch2(firstColor, secondColor, time, index, timePerIndex, speed, 255);
    }

    public static int colorSwitch2(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed, double alpha) {
        long now = (long) (speed * System.currentTimeMillis() + index * timePerIndex);

        float redDiff = (firstColor.getRed() - secondColor.getRed()) / time;
        float greenDiff = (firstColor.getGreen() - secondColor.getGreen()) / time;
        float blueDiff = (firstColor.getBlue() - secondColor.getBlue()) / time;
        int red = Math.round(secondColor.getRed() + redDiff * (now % (long) time));
        int green = Math.round(secondColor.getGreen() + greenDiff * (now % (long) time));
        int blue = Math.round(secondColor.getBlue() + blueDiff * (now % (long) time));

        float redInverseDiff = (secondColor.getRed() - firstColor.getRed()) / time;
        float greenInverseDiff = (secondColor.getGreen() - firstColor.getGreen()) / time;
        float blueInverseDiff = (secondColor.getBlue() - firstColor.getBlue()) / time;
        int inverseRed = Math.round(firstColor.getRed() + redInverseDiff * (now % (long) time));
        int inverseGreen = Math.round(firstColor.getGreen() + greenInverseDiff * (now % (long) time));
        int inverseBlue = Math.round(firstColor.getBlue() + blueInverseDiff * (now % (long) time));

        if (now % ((long) time * 2) < (long) time)
            return ColorUtils.getColor(inverseRed, inverseGreen, inverseBlue, (int) alpha);
        else return ColorUtils.getColor(red, green, blue, (int) alpha);
    }

    public static Color colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed) {
        return colorSwitch(firstColor, secondColor, time, index, timePerIndex, speed, 255.0D);
    }

    public static Color colorSwitch(Color firstColor, Color secondColor, float time, int index, long timePerIndex, double speed, double alpha) {
        long now = (long) (speed * (double) System.currentTimeMillis() + (double) ((long) index * timePerIndex));
        float redDiff = (float) (firstColor.getRed() - secondColor.getRed()) / time;
        float greenDiff = (float) (firstColor.getGreen() - secondColor.getGreen()) / time;
        float blueDiff = (float) (firstColor.getBlue() - secondColor.getBlue()) / time;
        int red = Math.round((float) secondColor.getRed() + redDiff * (float) (now % (long) time));
        int green = Math.round((float) secondColor.getGreen() + greenDiff * (float) (now % (long) time));
        int blue = Math.round((float) secondColor.getBlue() + blueDiff * (float) (now % (long) time));
        float redInverseDiff = (float) (secondColor.getRed() - firstColor.getRed()) / time;
        float greenInverseDiff = (float) (secondColor.getGreen() - firstColor.getGreen()) / time;
        float blueInverseDiff = (float) (secondColor.getBlue() - firstColor.getBlue()) / time;
        int inverseRed = Math.round((float) firstColor.getRed() + redInverseDiff * (float) (now % (long) time));
        int inverseGreen = Math.round((float) firstColor.getGreen() + greenInverseDiff * (float) (now % (long) time));
        int inverseBlue = Math.round((float) firstColor.getBlue() + blueInverseDiff * (float) (now % (long) time));

        return now % ((long) time * 2L) < (long) time ? (new Color(inverseRed, inverseGreen, inverseBlue, (int) alpha)) : (new Color(red, green, blue, (int) alpha));
    }

    public static int swapAlpha(int color, float alpha) {
        int f = color >> 16 & 0xFF;
        int f1 = color >> 8 & 0xFF;
        int f2 = color & 0xFF;
        return ColorUtils.getColor(f, f1, f2, (int) alpha);
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= alpha << 24;
        color |= red << 16;
        color |= green << 8;
        return color |= blue;
    }
}
