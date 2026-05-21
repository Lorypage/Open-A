package cn.lazymoon.utils.render.font;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class FontManager {
    public static FontRenderer semibold;
    public static FontRenderer medium;
    public static FontRenderer regular;
    public static FontRenderer thin;
    public static FontRenderer ultralight;
    public static FontRenderer light;
    public static FontRenderer icon;
    public static FontRenderer icon1;
    public static FontRenderer FilledMaterial;


    public static void registerFonts() {
        semibold = new FontRenderer("PingFangSC-Semibold", "PingFangSC-Semibold");
        medium = new FontRenderer("PingFangSC-Medium", "PingFangSC-Medium");
        FilledMaterial = new FontRenderer("FilledMaterial", "FilledMaterial");
        regular = new FontRenderer("PingFangSC-Regular", "PingFangSC-Regular");
        thin = new FontRenderer("PingFangSC-Thin", "PingFangSC-Thin");
        ultralight = new FontRenderer("PingFangSC-Ultralight", "PingFangSC-Ultralight");
        light = new FontRenderer("PingFangSC-Light", "PingFangSC-Light");
        icon = new FontRenderer("Icon", "Icon");
        icon1 = new FontRenderer("iconfont", "iconfont");
    }
}
