package cn.lazymoon.utils.render.font;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.exploit.NameProtect;
import cn.lazymoon.nanovg.NanoVG;
import cn.lazymoon.utils.color.ColorPanel;
import net.minecraft.util.Formatting;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.lazymoon.utils.InstanceAccess.mc;
import static org.lwjgl.nanovg.NanoVG.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class FontRenderer extends NanoVG {

    private static final String COLOR_CODES = "0123456789abcdefklmnor";
    private static final int MAX_WIDTH_CACHE = 4096;
    private static final int MAX_PARSED_CACHE = 512;

    private final int fontInt;
    private final long context = NanoVG.INSTANCE.getContext();

    /**
     * 文本宽度缓存：
     * key = size + "|" + text
     */
    private final Map<String, Float> widthCache = new LinkedHashMap<String, Float>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Float> eldest) {
            return size() > MAX_WIDTH_CACHE;
        }
    };

    /**
     * 颜色分段解析缓存：
     * key = 原始文本（已经做过 NameProtect 之后）
     */
    private final Map<String, ParsedText> parsedCache = new LinkedHashMap<String, ParsedText>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ParsedText> eldest) {
            return size() > MAX_PARSED_CACHE;
        }
    };

    private final float[] bounds = new float[4];
    private final float[] ascender = new float[1];
    private final float[] descender = new float[1];
    private final float[] lineh = new float[1];

    private static final int[] colorCode = new int[32];

    static {
        for (int i = 0; i < 32; ++i) {
            final int base = (i >> 3 & 0x1) * 85;
            int r = (i >> 2 & 0x1) * 170 + base;
            int g = (i >> 1 & 0x1) * 170 + base;
            int b = (i & 0x1) * 170 + base;
            if (i == 6) {
                r += 85;
            }
            if (i >= 16) {
                r /= 4;
                g /= 4;
                b /= 4;
            }
            colorCode[i] = ((r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF));
        }
    }

    public FontRenderer(String name, String fileName) {
        try (InputStream inputStream = getResourceAsStream(fileName)) {
            byte[] data = toByteArray(Objects.requireNonNull(inputStream));
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();

            fontInt = nvgCreateFontMem(context, name, buffer, false);

            // 如果你确认 nvgCreateFontMem 已经拷贝了字体数据，可以释放
            // 这里保守一点，不释放；如果你测试没问题，可以打开下一行
            // MemoryUtil.memFree(buffer);

        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;

        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public static InputStream getResourceAsStream(String fileName) {
        try {
            String location = "/assets/lazymoon/arcane/fonts/" + fileName + ".ttf";
            return FontRenderer.class.getResourceAsStream(location);
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean isNameProtectEnabled() {
        return Client.INSTANCE.getModuleManager() != null
                && Client.INSTANCE.getModuleManager().getModule(NameProtect.class).isState();
    }

    private static String applyNameProtect(String text) {
        if (text == null) return "";

        if (isNameProtectEnabled() && mc.player != null) {
            String name = mc.player.getName().getString();
            if (text.contains(name)) {
                text = text.replace(name, Formatting.LIGHT_PURPLE + NameProtect.nick + Formatting.RESET);
            }
        }
        return text;
    }

    private static String stripFormattingCodes(String text) {
        if (text == null || text.indexOf('§') < 0) return text == null ? "" : text;

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void setFont(float size) {
        nvgFontFaceId(context, fontInt);
        nvgFontSize(context, size / 2f);
    }

    private float measureWidthInternal(String text, float size) {
        if (text == null || text.isEmpty()) return 0f;

        String key = size + "|" + text;
        Float cached = widthCache.get(key);
        if (cached != null) return cached;

        setFont(size);
        nvgTextBounds(context, 0, 0, text, bounds);

        float width = bounds[2] - bounds[0];
        widthCache.put(key, width);
        return width;
    }

    public float getStringWidth(String text, float size) {
        if (text == null || Client.INSTANCE.getModuleManager() == null) return 0f;
        text = stripFormattingCodes(applyNameProtect(text));
        return measureWidthInternal(text, size);
    }

    public float getHeight(float size) {
        setFont(size);
        nvgTextMetrics(context, ascender, descender, lineh);
        return lineh[0] / 2f;
    }

    public void drawString(float size, String text, float x, float y, Color color, boolean shadow) {
        if (Client.INSTANCE.getModuleManager() == null) return;
        text = applyNameProtect(text);

        if (shadow) {
            renderString(size, text, x + 0.5f, y + 0.5f, darken(color));
        }
        renderString(size, text, x, y, color);
    }

    public void b(float size, String text, float x, float y, Color color, boolean shadow) {
        drawString(size, text, x, y, color, shadow);
    }

    public void drawStringmiddleY(float size, String text, float x, float y, Color color, boolean shadow) {
        if (Client.INSTANCE.getModuleManager() == null) return;
        text = applyNameProtect(text);

        y -= getHeight(size) / 2f;

        if (shadow) {
            renderString(size, text, x + 0.5f, y + 0.5f, darken(color));
        }
        renderString(size, text, x, y, color);
    }

    public void drawStringOpposite(float size, String text, float x, float y, Color color, boolean shadow) {
        if (Client.INSTANCE.getModuleManager() == null) return;
        text = applyNameProtect(text);

        if (shadow) {
            renderStringOpposite(size, text, x + 0.5f, y + 0.5f, darken(color));
        }
        renderStringOpposite(size, text, x, y, color);
    }

    private void renderStringOpposite(float size, String text, float x, float y, Color color) {
        String plain = stripFormattingCodes(text);

        nvgSave(context);
        nvgTextAlign(context, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        setFont(size);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor colorBuf = NVGColor.malloc(stack);
            writeColor(colorBuf, color);
            nvgFillColor(context, colorBuf);

            nvgTranslate(context, x + measureWidthInternal(plain, size), y);
            nvgScale(context, -1f, 1f);
            nvgText(context, 0f, 0f, plain);
        }

        nvgRestore(context);
    }

    private void renderString(float size, String text, float x, float y, Color color) {
        if (text == null || text.isEmpty()) return;

        nvgTextAlign(context, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        setFont(size);

        // 没有颜色码，直接画，最快
        if (text.indexOf('§') < 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                NVGColor colorBuf = NVGColor.malloc(stack);
                writeColor(colorBuf, color);
                nvgFillColor(context, colorBuf);
                nvgText(context, x, y, text);
            }
            return;
        }

        ParsedText parsed = getParsedText(text);
        int baseArgb = argb(color);
        int baseAlpha = (baseArgb >>> 24) & 0xFF;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor colorBuf = NVGColor.malloc(stack);

            float currentX = x;
            for (TextSegment segment : parsed.segments) {
                int segArgb;
                if (segment.colorCode == -1) {
                    segArgb = baseArgb;
                } else {
                    int rgb = colorCode[segment.colorCode];
                    if (rgb == 0xAAAAAA) rgb = 0xFFFFFF;
                    segArgb = (baseAlpha << 24) | (rgb & 0x00FFFFFF);
                }

                writeColor(colorBuf, segArgb);
                nvgFillColor(context, colorBuf);

                segment.width = measureWidthInternal(segment.text, size);
                nvgText(context, currentX, y, segment.text);
                currentX += segment.width + 0.2f;
            }
        }
    }

    private void renderString(float size, String text, float x, float y, ColorPanel color) {
        if (text == null || text.isEmpty()) return;

        nvgTextAlign(context, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        setFont(size);

        if (text.indexOf('§') < 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                NVGColor colorBuf = NVGColor.malloc(stack);
                writeColor(colorBuf, color);
                nvgFillColor(context, colorBuf);
                nvgText(context, x, y, text);
            }
            return;
        }

        ParsedText parsed = getParsedText(text);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor colorBuf = NVGColor.malloc(stack);

            float currentX = x;
            for (TextSegment segment : parsed.segments) {
                if (segment.colorCode == -1) {
                    writeColor(colorBuf, color);
                } else {
                    int rgb = colorCode[segment.colorCode];
                    if (rgb == 0xAAAAAA) rgb = 0xFFFFFF;
                    writeColor(colorBuf, rgb, color.alpha);
                }

                nvgFillColor(context, colorBuf);

                segment.width = measureWidthInternal(segment.text, size);
                nvgText(context, currentX, y, segment.text);
                currentX += segment.width + 0.2f;
            }
        }
    }

    public void drawGlowString(float size, String text, float x, float y, ColorPanel color, ColorPanel glowColor, boolean shadow, float radius) {
        if (Client.INSTANCE.getModuleManager() == null) return;
        text = applyNameProtect(text);

        if (shadow) {
            renderGlowString(size, text, x + 0.5f, y + 0.5f, color.darken(), glowColor, radius);
        }
        renderGlowString(size, text, x, y, color, glowColor, radius);
    }

    public void drawGlowString(float size, String text, float x, float y, Color color, Color glowColor, boolean shadow, float radius) {
        if (Client.INSTANCE.getModuleManager() == null) return;
        text = applyNameProtect(text);

        if (shadow) {
            renderGlowString(size, text, x + 0.5f, y + 0.5f, darken(color), glowColor, radius);
        }
        renderGlowString(size, text, x, y, color, glowColor, radius);
    }

    private void renderGlowString(float size, String text, float x, float y, ColorPanel color, ColorPanel glowColor, float radius) {
        nvgFontBlur(context, radius);
        renderString(size, text, x, y, glowColor);
        nvgFontBlur(context, 0f);
        renderString(size, text, x, y, color);
    }

    private void renderGlowString(float size, String text, float x, float y, Color color, Color glowColor, float radius) {
        nvgFontBlur(context, radius);
        renderString(size, text, x, y, glowColor);
        nvgFontBlur(context, 0f);
        renderString(size, text, x, y, color);
    }

    private ParsedText getParsedText(String text) {
        ParsedText cached = parsedCache.get(text);
        if (cached != null) return cached;

        List<TextSegment> segments = new ArrayList<>(Math.max(4, text.length() / 8));
        StringBuilder currentSegment = new StringBuilder();

        int currentColorCode = -1;

        for (int i = 0; i < text.length(); i++) {
            char chr = text.charAt(i);

            if (chr == '§' && i + 1 < text.length()) {
                if (currentSegment.length() > 0) {
                    segments.add(new TextSegment(currentSegment.toString(), currentColorCode));
                    currentSegment.setLength(0);
                }

                char next = Character.toLowerCase(text.charAt(++i));
                int codeIndex = COLOR_CODES.indexOf(next);

                if (codeIndex >= 0) {
                    if (codeIndex < 16) {
                        currentColorCode = codeIndex;
                    } else if (next == 'r') {
                        currentColorCode = -1;
                    }
                }
            } else {
                currentSegment.append(chr);
            }
        }

        if (currentSegment.length() > 0) {
            segments.add(new TextSegment(currentSegment.toString(), currentColorCode));
        }

        ParsedText parsed = new ParsedText(segments.toArray(new TextSegment[0]));
        parsedCache.put(text, parsed);
        return parsed;
    }

    private static void writeColor(NVGColor colorBuf, Color color) {
        colorBuf.r(color.getRed() / 255f)
                .g(color.getGreen() / 255f)
                .b(color.getBlue() / 255f)
                .a(color.getAlpha() / 255f);
    }

    private static void writeColor(NVGColor colorBuf, ColorPanel color) {
        colorBuf.r(color.red)
                .g(color.green)
                .b(color.blue)
                .a(color.alpha);
    }

    private static void writeColor(NVGColor colorBuf, int argb) {
        colorBuf.a(((argb >>> 24) & 0xFF) / 255f)
                .r(((argb >>> 16) & 0xFF) / 255f)
                .g(((argb >>> 8) & 0xFF) / 255f)
                .b((argb & 0xFF) / 255f);
    }

    private static void writeColor(NVGColor colorBuf, int rgb, float alpha) {
        colorBuf.a(alpha)
                .r(((rgb >>> 16) & 0xFF) / 255f)
                .g(((rgb >>> 8) & 0xFF) / 255f)
                .b((rgb & 0xFF) / 255f);
    }

    private static int clamp255(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private static int argb(Color color) {
        return ((color.getAlpha() & 0xFF) << 24)
                | ((color.getRed() & 0xFF) << 16)
                | ((color.getGreen() & 0xFF) << 8)
                | (color.getBlue() & 0xFF);
    }

    private static Color darken(Color color) {
        float factor = 0.8f;
        return new Color(
                color.getRed() / 255f * factor,
                color.getGreen() / 255f * factor,
                color.getBlue() / 255f * factor,
                color.getAlpha() / 255f
        );
    }

    private static final class ParsedText {
        final TextSegment[] segments;

        ParsedText(TextSegment[] segments) {
            this.segments = segments;
        }
    }

    private static final class TextSegment {
        final String text;
        final int colorCode; // -1 = base color
        float width;

        TextSegment(String text, int colorCode) {
            this.text = text;
            this.colorCode = colorCode;
        }
    }
}