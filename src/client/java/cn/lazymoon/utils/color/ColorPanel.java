package cn.lazymoon.utils.color;




public class ColorPanel {

    public float red, green, blue, alpha;


    public ColorPanel(float red, float green, float blue, float alpha) {
        red = Math.max(0f, Math.min(1f, red));
        this.red = red;

        green = Math.max(0f, Math.min(1f, green));
        this.green = green;

        blue = Math.max(0f, Math.min(1f, blue));
        this.blue = blue;

        alpha = Math.max(0f, Math.min(1f, alpha));
        this.alpha = alpha;
    }

    public ColorPanel darken() {
        return new ColorPanel(this.red * 0.35f, this.green * 0.35f, this.blue * 0.35f, this.alpha);
    }

    public static ColorPanel createColorPanel(float red, float green, float blue, float alpha) {
        return new ColorPanel(red, green, blue, alpha);
    }

    public ColorPanel updateAlpha(float alpha) {
        return new ColorPanel(this.red, this.green, this.blue, alpha);
    }

    public ColorPanel mulAlpha(float alpha) {
        return new ColorPanel(this.red, this.green, this.blue, this.alpha * alpha);
    }

    public ColorPanel updateRGB(float red, float green, float blue) {
        return new ColorPanel(red, green, blue, this.alpha);
    }

    public ColorPanel updateRGBA(float red, float green, float blue, float alpha) {
        return new ColorPanel(red, green, blue, alpha);
    }

}
