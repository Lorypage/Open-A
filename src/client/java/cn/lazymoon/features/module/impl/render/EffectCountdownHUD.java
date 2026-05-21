package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.world.Scaffold;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.misc.ItemUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import com.ibm.icu.impl.Pair;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;

import java.awt.Color;
import java.util.Locale;

import static cn.lazymoon.utils.InstanceAccess.mc;

@ModuleInfo(
        name = "EffectCountdownHUD",
        description = "Show food countdown, strength countdown and scaffold block count",
        key = 0,
        category = Category.Render,
        hidden = false
)
public class EffectCountdownHUD extends Module {

    public final BoolValue top = new BoolValue("Top", true);
    public final BoolValue foodCountdown = new BoolValue("Food Countdown", true);
    public final BoolValue strengthCountdown = new BoolValue("Strength Countdown", true);
    public final BoolValue scaffoldCountdown = new BoolValue("Scaffold Countdown", true);

    private static final float BAR_WIDTH = 140f;
    private static final float BAR_HEIGHT = 18f;
    private static final float BAR_RADIUS = 8f;

    // 物品栏上方
    private static final float FOOD_Y_OFFSET = 58f;
    private static final float SCAFFOLD_Y_OFFSET = 78f;

    // 准星下方
    private static final float STRENGTH_Y_OFFSET = 16f;

    private int strengthMaxDuration = 1;
    private int strengthLastAmplifier = -1;

    private float foodAnim = 0f;
    private float strengthAnim = 0f;
    private float scaffoldAnim = 0f;

    @Override
    public void onEnable() {
        strengthMaxDuration = 1;
        strengthLastAmplifier = -1;
        foodAnim = 0f;
        strengthAnim = 0f;
        scaffoldAnim = 0f;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player == null || mc.world == null) return;

        if (strengthCountdown.get()) {
            StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
            if (strength == null || strength.getDuration() <= 0) {
                strengthMaxDuration = 1;
                strengthLastAmplifier = -1;
            } else {
                if (strengthLastAmplifier != strength.getAmplifier()) {
                    strengthLastAmplifier = strength.getAmplifier();
                    strengthMaxDuration = Math.max(1, strength.getDuration());
                } else {
                    strengthMaxDuration = Math.max(strengthMaxDuration, strength.getDuration());
                }
            }
        }
    }

    @EventTarget
    public void onRender(RenderNvgEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (Client.INSTANCE.getModuleManager() == null) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        float centerX = screenW / 2f;
        float barX = centerX - BAR_WIDTH / 2f;

        Pair<Color, Color> theme = Theme.getCurrentTheme().getColors();
        Color leftColor = theme.first;
        Color rightColor = theme.second;

        if (scaffoldCountdown.get() && isScaffoldEnabled()) {
            renderScaffold(barX, top.get()? 80 : screenH / 2f + 110, leftColor, rightColor);
        }

        if (foodCountdown.get()) {
            renderFood(barX, top.get()? 50 : screenH / 2f + 80, leftColor, rightColor);
        }

        if (strengthCountdown.get()) {
            renderStrength(barX, top.get()? 20 : screenH / 2f + 50, leftColor, rightColor);
        }
    }

    private void renderFood(float x, float y, Color leftColor, Color rightColor) {
        if (!mc.player.isUsingItem()) {
            foodAnim = approach(foodAnim, 0f, 0.2f);
            return;
        }

        ItemStack active = mc.player.getActiveItem();
        if (active.isEmpty() || !ItemUtils.isFood(active)) {
            foodAnim = approach(foodAnim, 0f, 0.2f);
            return;
        }

        int leftTicks = mc.player.getItemUseTimeLeft();
        int totalTicks = Math.max(1, active.getMaxUseTime(mc.player));
        float progress = clamp01(leftTicks / (float) totalTicks);

        foodAnim = approach(foodAnim, progress, 0.25f);

        String title = "Food";
        String detail = active.getName().getString() + "  " + formatSeconds(leftTicks);

        drawBar(x, y, title, detail, foodAnim, leftColor, rightColor);
    }

    private void renderStrength(float x, float y, Color leftColor, Color rightColor) {
        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        if (strength == null || strength.getDuration() <= 0) {
            strengthAnim = approach(strengthAnim, 0f, 0.2f);
            return;
        }

        float progress = clamp01(strength.getDuration() / (float) Math.max(1, strengthMaxDuration));
        strengthAnim = approach(strengthAnim, progress, 0.25f);

        String title = "Strength";
        if (strength.getAmplifier() > 0) {
            title += " " + roman(strength.getAmplifier() + 1);
        }

        String detail = formatDuration(strength.getDuration());

        drawBar(x, y, title, detail, strengthAnim, leftColor, rightColor);
    }

    private void renderScaffold(float x, float y, Color leftColor, Color rightColor) {
        int blockCount = Scaffold.getBlockCount();
        if (blockCount < 0) blockCount = 0;

        // 这里用 64 作为视觉基准，方便看剩余块数
        float progress = clamp01(blockCount / 64f);
        scaffoldAnim = approach(scaffoldAnim, progress, 0.25f);

        String title = "Scaffold";
        String detail = blockCount + " Blocks";

        drawBar(x, y, title, detail, scaffoldAnim, leftColor, rightColor);
    }

    private void drawBar(float x, float y, String title, String detail, float progress, Color leftColor, Color rightColor) {
        progress = clamp01(progress);

        // 偏白底色
        Color bg = new Color(255, 255, 255, 62);
        Color bgInner = new Color(255, 255, 255, 28);
        Color titleColor = new Color(30, 30, 30, 235);
        Color detailColor = new Color(60, 60, 60, 220);

        // 背景光晕 + 底板
        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloomApple(x, y, BAR_WIDTH, BAR_HEIGHT, BAR_RADIUS, 2, new Color(255, 255, 255, 16));
        }
        RenderHelper.drawRoundedRect(x, y, BAR_WIDTH, BAR_HEIGHT, bg, BAR_RADIUS);
        RenderHelper.drawRoundedRect(x + 0.7f, y + 0.7f, BAR_WIDTH - 1.4f, BAR_HEIGHT - 1.4f, bgInner, BAR_RADIUS - 0.7f);

        // 进度条
        float fillWidth = BAR_WIDTH * progress;
        if (fillWidth > 1.5f) {
            RenderHelper.drawGradientRoundedRectLR(
                    x + 1f,
                    y + 1f,
                    fillWidth - 2f,
                    BAR_HEIGHT - 2f,
                    withAlpha(leftColor, 210),
                    withAlpha(rightColor, 210),
                    BAR_RADIUS - 1f
            );
        }

        // 左侧标题
        float titleSize = 13.5f;
        FontManager.semibold.drawString(titleSize, title, x + 8f, y + 6.5f, titleColor, false);

        // 右侧详细信息
        float detailSize = 13f;
        float detailWidth = FontManager.semibold.getStringWidth(detail, detailSize);
        FontManager.semibold.drawString(
                detailSize,
                detail,
                x + BAR_WIDTH - 8f - detailWidth,
                y + 6.5f,
                detailColor,
                false
        );
    }

    private boolean isScaffoldEnabled() {
        if (Client.INSTANCE.getModuleManager() == null) return false;
        Scaffold scaffold = Client.INSTANCE.getModuleManager().getModule(Scaffold.class);
        return scaffold != null && scaffold.isState();
    }

    private static float approach(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp255(alpha));
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static String formatSeconds(int ticks) {
        float seconds = ticks / 20f;
        return String.format(Locale.ROOT, "%.1fs", seconds);
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        if (minutes <= 0) {
            return seconds + "s";
        }
        return minutes + "m " + (seconds < 10 ? "0" : "") + seconds + "s";
    }

    private static String roman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
}