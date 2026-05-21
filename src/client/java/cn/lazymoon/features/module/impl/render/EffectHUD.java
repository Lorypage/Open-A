package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.value.impl.ModeValue;
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
import cn.lazymoon.utils.animations.ContinualAnimation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.EaseInOutQuad;
import cn.lazymoon.utils.color.ColorPanel;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.awt.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "EffectHUD", description = "Shou you Potion on you game screen",key = 0, category = Category.Render,hidden = false)
public class EffectHUD extends ModuleWidget {
    private final List<StatusEffectInstance> activeEffects = new java.util.ArrayList<>();
    private final Map<String, EffectData> effectDataMap = new HashMap<>();
    private final List<StatusEffectInstance> pendingRemoval = new java.util.ArrayList<>();
    private final ContinualAnimation headerAnimation = new ContinualAnimation();
    private static final float PADDING = 2f;
    private static final float SMOOTHNESS = 5f;

    @SuppressWarnings("unused")
    @EventTarget
    public void render(RenderNvgEvent event) {
        if (mc.player == null) return;
        float x = renderX, y = renderY;
        setWidth(50);
        setHeight(50);
        boolean isOnRightSide = x > (float) mc.getWindow().getScaledWidth() / 2;
        if (isOnRightSide) {
            renderRightSideUI(event, x, y);
        } else {
            renderLeftSideUI(event, x, y);
        }
    }

    @Override
    public boolean shouldRender() {
        return isState();
    }

    private void renderRightSideUI(RenderNvgEvent event, float x, float y) {
        float maxWidth = calculateMaxWidth();
        if (activeEffects.isEmpty()) {
            renderHeader(event, x + maxWidth - PADDING, y - PADDING, maxWidth, true);
        }
        float offset = 0;
        for (StatusEffectInstance effect : activeEffects) {
            EffectData data = getEffectData(effect);
            if (shouldSkipEffect(data)) continue;
            float effectX = calculateEffectX(data, x, true);
            renderEffect(event, effect, data, effectX, y + offset, true);
            offset += (float) (27 * data.animation.getOutput());
        }
    }

    private void renderLeftSideUI(RenderNvgEvent event, float x, float y) {
        float maxWidth = calculateMaxWidth();
        if (activeEffects.isEmpty()) {
            renderHeader(event, x - PADDING, y - PADDING, maxWidth, false);
        }
        float offset = 0;
        for (StatusEffectInstance effect : activeEffects) {
            EffectData data = getEffectData(effect);
            if (shouldSkipEffect(data)) continue;
            float effectX = calculateEffectX(data, x, false);
            renderEffect(event, effect, data, effectX, y + offset, false);
            offset += (float) (27 * data.animation.getOutput());
        }
    }

    private void renderHeader(RenderNvgEvent event, float blurX, float blurY, float maxWidth, boolean isRightSide) {
        headerAnimation.animate(maxWidth, 60);
        float animatedWidth = headerAnimation.getOutput();
        float textBlurX = isRightSide ? (blurX - animatedWidth - EffectHUD.PADDING) + 4f + 17 : (blurX + 21f - EffectHUD.PADDING) - 17;
        renderBlur(event, textBlurX, blurY, animatedWidth);
        float textBgX = isRightSide ? (blurX - animatedWidth) + 1f + 17 : (blurX + 19f) - 17;
        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloomApple(textBgX, blurY, animatedWidth, 15f, 15, 2, new Color(255, 255, 255, 10));
        }
        RenderHelper.drawGradientAppleRoundedRectLR(textBgX, blurY, animatedWidth, 15f, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),15);
        float textX = isRightSide ? (blurX - FontManager.semibold.getStringWidth("Potion Effects", 16)) - 2 + 17 - 5 : (blurX + 22f) - 17 + 5;
        FontManager.semibold.drawString(16f, "Potion Effects", textX, blurY + 4f, new Color(255,255,255,255), false);
    }

    private void renderEffect(RenderNvgEvent event, StatusEffectInstance effect, EffectData data, float x, float y, boolean isRightSide) {
        String effectName = getEffectDisplayName(effect);
        String text = effectName;
        float effectWidth = FontManager.semibold.getStringWidth(text, 16f) + 6f + 20;
        float progress = calculateProgress(effect, data);
        data.progressAnimation.animate(effectWidth * progress, 30);
        if (isRightSide) {
            renderLeftSideEffect(event, effect, data, x, y, effectName, effectWidth);
        } else {
            renderLeftSideEffect(event, effect, data, x, y, effectName, effectWidth);
        }
    }

    private void renderRightSideEffect(RenderNvgEvent event, StatusEffectInstance effect, EffectData data, float x, float y, String effectName, float effectWidth) {
        float maxWidth = calculateMaxWidth();
        renderBlur(event, x + maxWidth - effectWidth, y, effectWidth - 20);
        RenderHelper.drawRoundRectBloom(x + maxWidth - effectWidth, y, effectWidth - 20, 25,10,1, new Color(255,255,255,10));
        RenderHelper.drawGradientRoundedRectLR(x + maxWidth - effectWidth, y, effectWidth - 20, 25, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10), 10);
        RenderHelper.drawRoundedRect(x + maxWidth, y, Math.max(20, data.progressAnimation.getOutput()), 25, getEffectColor(effect), 10);
        FontManager.FilledMaterial.drawGlowString(24, getEffectIcon(effect), x + maxWidth + 20 / 2 - FontManager.FilledMaterial.getStringWidth(getEffectIcon(effect),24) / 2 + .5f, y + 21 / 2 - FontManager.FilledMaterial.getHeight(24) / 2 - .5f, getEffectColor(effect), ColorPanel.createColorPanel(0f, 0f, 0f, 0.3f), false, 5f);
        String text;
            text = effectName;
        FontManager.semibold.drawGlowString(15, text, x + maxWidth - FontManager.semibold.getStringWidth(text, 15) - 3f, y + 9, ColorPanel.createColorPanel(1f, 1f, 1f, 0.9f), ColorPanel.createColorPanel(0f, 0f, 0f, 0.1f), false, 5f);
    }

    private void renderLeftSideEffect(RenderNvgEvent event, StatusEffectInstance effect, EffectData data, float x, float y, String effectName, float effectWidth) {
        renderBlur(event, x, y, effectWidth);
        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloom(x, y, effectWidth, 25, 10, 2, new Color(255, 255, 255, 10));
        }
        RenderHelper.drawGradientRoundedRectLR(x, y, effectWidth, 25, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),10);
        String text;
            text = effectName;
        RenderHelper.drawRoundedRect(x, y, Math.max(20, data.progressAnimation.getOutput()), 25, getEffectColor(effect), 10);
        FontManager.FilledMaterial.drawGlowString(24, getEffectIcon(effect), x + 20 / 2 - FontManager.FilledMaterial.getStringWidth(getEffectIcon(effect),24) / 2 + .5f, y + 21 / 2 - FontManager.FilledMaterial.getHeight(24) / 2 - .5f, getEffectColor(effect), ColorPanel.createColorPanel(0f, 0f, 0f, 0.1f), false, 5f);
        FontManager.semibold.drawGlowString(15, text, x + 20f, y + 9, ColorPanel.createColorPanel(1f, 1f, 1f, 0.9f), ColorPanel.createColorPanel(0f, 0f, 0f, 0.1f), false, 5f);
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player == null) return;

        Collection<StatusEffectInstance> currentEffects = mc.player.getStatusEffects();
        updateEffects(currentEffects);
        cleanupEffects();
        sortEffects();
    }

    private static String formatDuration(int ticks) {
        int seconds = ticks / 20;
        if (seconds < 60) {
            return seconds + "s";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + "m " + (remainingSeconds < 10 ? "0" : "") + remainingSeconds + "s";
        }
    }

    private void updateEffects(Collection<StatusEffectInstance> currentEffects) {
        Set<String> currentEffectKeys = new HashSet<>();
        for (StatusEffectInstance currentEffect : currentEffects) {
            if (currentEffect.getDuration() <= 0) continue;
            if (currentEffect.getEffectType() == StatusEffects.NIGHT_VISION) {
                continue;
            }
            String effectKey = getEffectKey(currentEffect);
            currentEffectKeys.add(effectKey);
            StatusEffectInstance existingEffect = findExistingEffect(effectKey);
            if (existingEffect != null) {
                updateExistingEffect(existingEffect, currentEffect);
            } else if (!isPendingRemoval(effectKey)) {
                addNewEffect(currentEffect);
            }
        }
        for (StatusEffectInstance effect : new ArrayList<>(activeEffects)) {
            String effectKey = getEffectKey(effect);
            boolean shouldRemove = !currentEffectKeys.contains(effectKey) || effect.getDuration() <= 0;
            if (shouldRemove && !pendingRemoval.contains(effect)) {
                markEffectForRemoval(effect);
            }
        }
    }

    private StatusEffectInstance findExistingEffect(String effectKey) {
        for (StatusEffectInstance effect : activeEffects) {
            if (getEffectKey(effect).equals(effectKey)) {
                return effect;
            }
        }
        return null;
    }

    private void updateExistingEffect(StatusEffectInstance existingEffect, StatusEffectInstance currentEffect) {
        String effectKey = getEffectKey(existingEffect);
        EffectData data = effectDataMap.get(effectKey);
        if (data != null) {
            if (currentEffect.getDuration() > data.originalDuration) {
                data.originalDuration = currentEffect.getDuration();
            }
            activeEffects.remove(existingEffect);
            activeEffects.add(currentEffect);
            effectDataMap.remove(effectKey);
            effectDataMap.put(getEffectKey(currentEffect), data);
            pendingRemoval.removeIf(e -> getEffectKey(e).equals(effectKey));
        }
    }

    private void cleanupEffects() {
        Iterator<StatusEffectInstance> removalIterator = pendingRemoval.iterator();
        while (removalIterator.hasNext()) {
            StatusEffectInstance effect = removalIterator.next();
            EffectData data = effectDataMap.get(getEffectKey(effect));
            if (data != null && data.animation.isDone()) {
                activeEffects.remove(effect);
                effectDataMap.remove(getEffectKey(effect));
                removalIterator.remove();
            }
        }
    }

    private void sortEffects() {
            activeEffects.sort((e1, e2) -> Float.compare(FontManager.semibold.getStringWidth(getEffectDisplayName(e2), 16f), FontManager.semibold.getStringWidth(getEffectDisplayName(e1), 16f)));
    }

    private float calculateMaxWidth() {
        return FontManager.semibold.getStringWidth("Potion Effects", 16f) + 16;
    }

    private float calculateEffectX(EffectData data, float baseX, boolean isRightSide) {
        if (data.animation.getDirection() == Direction.FORWARDS) {
            float startX = isRightSide ? mc.getWindow().getScaledWidth() : -calculateMaxWidth() - 50;
            return (float) (startX + (baseX - startX) * data.animation.getOutput());
        } else {
            float currentX = data.targetX;
            float endX = isRightSide ? mc.getWindow().getScaledWidth() : -calculateMaxWidth() - 50;
            return (float) (currentX + (endX - currentX) * (1 - data.animation.getOutput()));
        }
    }

    private float calculateProgress(StatusEffectInstance effect, EffectData data) {
        StatusEffectInstance currentEffect = getCurrentEffectInstance(effect);
        if (currentEffect != null) {
            return Math.max(0f, Math.min(1f, (float) currentEffect.getDuration() / data.originalDuration));
        }
        return Math.max(0f, Math.min(1f, (float) effect.getDuration() / data.originalDuration));
    }

    private StatusEffectInstance getCurrentEffectInstance(StatusEffectInstance effect) {
        if (mc.player == null) {
            return effect;
        }
        String targetKey = getEffectKey(effect);
        for (StatusEffectInstance currentEffect : mc.player.getStatusEffects()) {
            if (getEffectKey(currentEffect).equals(targetKey)) {
                return currentEffect;
            }
        }
        return effect;
    }

    private boolean shouldSkipEffect(EffectData data) {
        return data.animation.getDirection() == Direction.BACKWARDS && data.animation.isDone();
    }

    private void addNewEffect(StatusEffectInstance effect) {
        Animation animation = new EaseInOutQuad(200, 1);
        animation.setDirection(Direction.FORWARDS);
        EffectData data = new EffectData();
        data.animation = animation;
        data.progressAnimation = new ContinualAnimation();
        data.originalDuration = effect.getDuration();
        data.targetX = renderX;
        effectDataMap.put(getEffectKey(effect), data);
        activeEffects.add(effect);
    }

    private void markEffectForRemoval(StatusEffectInstance effect) {
        EffectData data = effectDataMap.get(getEffectKey(effect));
        if (data != null && data.animation.getDirection() != Direction.BACKWARDS) {
            data.animation.setDirection(Direction.BACKWARDS);
            pendingRemoval.add(effect);
        }
    }

    private EffectData getEffectData(StatusEffectInstance effect) {
        return effectDataMap.get(getEffectKey(effect));
    }

    private void renderBlur(RenderNvgEvent event, float x, float y, float width) {
        if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
            BuiltBlur blur = Builder.blur().size(new SizeState(width + SMOOTHNESS, activeEffects.isEmpty() ? 15 + SMOOTHNESS : 25 + SMOOTHNESS)).
                    radius(new QuadRadiusState(15)).blurRadius(20).smoothness(SMOOTHNESS).
                    color(QuadColorState.TRANSPARENT).position(new PositionState(x - SMOOTHNESS / 2f, y - SMOOTHNESS / 2f)).matrix4f(event.matrix4f()).build();
            BlurTaskInstance.addTask(blur);
        }
    }

    private String getEffectKey(StatusEffectInstance effect) {
        StatusEffect type = effect.getEffectType().value();
        return type.getTranslationKey() + ":" + effect.getAmplifier();
    }

    private boolean isPendingRemoval(String effectKey) {
        return pendingRemoval.stream().anyMatch(effect -> getEffectKey(effect).equals(effectKey));
    }

    private String getEffectDisplayName(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString();
        int amplifier = effect.getAmplifier();
        if (amplifier > 0) {
            name += " " + getRomanNumber(amplifier + 1);
        }
        return name;
    }

    private String getEffectIcon(StatusEffectInstance effect) {
        String type = effect.getEffectType().value().getTranslationKey().toLowerCase().replace(" ", "_");
        if (type.contains("speed") || type.contains("slowness")) {
            return "E";
        } else if (type.contains("haste")) {
            return "F";
        } else if (type.contains("strength")) {
            return "G";
        } else if (type.contains("mining_fatigue") || type.contains("weakness")) {
            return "H";
        } else if (type.contains("muscle")) {
            return "I";
        } else if (type.contains("instant_health") || type.contains("regeneration")) {
            return "J";
        } else if (type.contains("instant_damage")) {
            return "K";
        } else if (type.contains("jump_boost")) {
            return "L";
        } else if (type.contains("nausea") || type.contains("hunger") || type.contains("saturation")) {
            return "M";
        } else if (type.contains("fire_resistance")) {
            return "N";
        } else if (type.contains("resistance")) {
            return "O";
        } else if (type.contains("water_breathing") || type.contains("oozing") || type.contains("conduit_power") || type.contains("dolphins_grace")) {
            return "P";
        } else if (type.contains("invisibility") || type.contains("darkness")) {
            return "Q";
        } else if (type.contains("blindness")) {
            return "R";
        } else if (type.contains("night_vision")) {
            return "S";
        } else if (type.contains("poison") || type.contains("wither")) {
            return "T";
        } else if (type.contains("health_boost")) {
            return "U";
        } else if (type.contains("absorption")) {
            return "V";
        } else if (type.contains("glowing")) {
            return "W";
        } else if (type.contains("trial_omen") || type.contains("raid_omen")) {
            return "X";
        }
        return "A";
    }

    private ColorPanel getEffectColor(StatusEffectInstance effect) {
        StatusEffect type = effect.getEffectType().value();
        int color = type.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return ColorPanel.createColorPanel(r, g, b, 0.35f);
    }

    private static String getRomanNumber(int number) {
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

    private static class EffectData {
        public Animation animation;
        public ContinualAnimation progressAnimation;
        public int originalDuration;
        public float targetX;
    }
}
