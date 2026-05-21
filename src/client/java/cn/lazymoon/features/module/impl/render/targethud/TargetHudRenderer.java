package cn.lazymoon.features.module.impl.render.targethud;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.features.module.impl.render.PostProcessing;
import cn.lazymoon.ingameui.ui.ModuleWidget;
import cn.lazymoon.sxmurxy.builders.Builder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.BlurTaskInstance;
import cn.lazymoon.sxmurxy.instance.TextureTaskInstance;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltTexture;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.ContinualAnimation;
import cn.lazymoon.utils.color.ColorPanel;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;

import java.awt.*;

import static cn.lazymoon.utils.InstanceAccess.mc;

public class TargetHudRenderer {
    public static ContinualAnimation animation1 = new ContinualAnimation();
    public static ContinualAnimation animation2 = new ContinualAnimation();

    public static void render(float scaleValue, float x, float y, Matrix4f matrix4f, LivingEntity renderTarget, ModuleWidget dragging) {
        String name = renderTarget.getName().getString();
        float space = 9f * scaleValue, avatar = 32f * scaleValue, nameTextSize = 18f * scaleValue, healthTextSize = 15f * scaleValue;
        float maxWidth = space + avatar + FontManager.semibold.getStringWidth(name, nameTextSize) + 30;

        dragging.setWidth(maxWidth);
        dragging.setHeight(38);

        float height = dragging.getHeight();

        if (scaleValue > 0f) {
            float centerX = x + maxWidth / 2f, centerY = y + height / 2f;
            float scaledWidth = maxWidth * scaleValue, scaledHeight = height * scaleValue;
            float scaledX = centerX - scaledWidth / 2f, scaledY = centerY - scaledHeight / 2f;

            renderBackground(matrix4f, scaledX, scaledY, scaledWidth, scaledHeight, scaleValue);
            LivingEntity target = renderTarget instanceof AbstractClientPlayerEntity ? renderTarget : mc.player;
            renderTextAndHealthBarOthers(renderTarget, scaledX, scaledY, scaledWidth, scaleValue,scaledHeight);
            renderPlayerAvatar(target, scaledX, scaledY, scaleValue, matrix4f);
        }
    }

    private static void renderBackground(Matrix4f matrix4f, float scaledX, float scaledY, float scaledWidth, float scaledHeight, float scaleValue) {
        float smoothness = 5f;
        if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
            BuiltBlur blur = Builder.blur().size(new SizeState(scaledWidth + smoothness, scaledHeight + smoothness)).radius(new QuadRadiusState(15)).blurRadius(20).smoothness(smoothness).color(QuadColorState.TRANSPARENT).position(new PositionState(scaledX - smoothness / 2f, scaledY - smoothness / 2f)).matrix4f(matrix4f).build();
            BlurTaskInstance.addTask(blur);
        }
        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloomApple(scaledX, scaledY, scaledWidth, scaledHeight, 10 * scaleValue, 2, new Color(255, 255, 255, 10));
        }
        RenderHelper.drawGradientAppleRoundedRectLR(scaledX, scaledY, scaledWidth, scaledHeight, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),10 * scaleValue);
    }

    private static void renderPlayerAvatar(LivingEntity renderTarget, float scaledX, float scaledY, float scaleValue, Matrix4f matrix4f) {
        float avatarSize = 32f * scaleValue;
        float avatarX = scaledX + 3f * scaleValue;
        float avatarY = scaledY + 3f * scaleValue;
        AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(((AbstractClientPlayerEntity) renderTarget).getSkinTextures().texture());
        if (abstractTexture != null) {
            BuiltTexture texture = Builder.texture().matrix4f(matrix4f).position(new PositionState(avatarX, avatarY)).size(new SizeState(avatarSize, avatarSize)).texture(0.125f, 0.125f, 0.125f, 0.125f, abstractTexture).smoothness(1f).radius(new QuadRadiusState(5 * scaleValue, 5 * scaleValue, 5 * scaleValue, 5 * scaleValue)).color(new QuadColorState(new Color(255, 255, 255,255),new Color(255, 255, 255,255),new Color(255, 255, 255,255),new Color(255, 255, 255,255))).build();
            TextureTaskInstance.addTask(texture);
        }
    }

    private static void renderTextAndHealthBarOthers(LivingEntity renderTarget, float scaledX, float scaledY, float scaledWidth, float scaleValue,float scaledHeight) {
        float textX = scaledX + 38f * scaleValue, textY = scaledY + 5f * scaleValue;
        float healthBarX = scaledX + 38 * scaleValue, healthBarY = scaledY + (17) * scaleValue;
        float healthBarWidth = (scaledWidth - 42 * scaleValue), healthBarHeight = 16 * scaleValue;
        float textAlpha = 0.9f * scaleValue, shadowAlpha = 0.1f * scaleValue;
        float nameTextSize = 18f * scaleValue;
        float healthTextSize = 18 * scaleValue;
        float winsize = 14 * scaleValue;
        String name = renderTarget.getName().getString();

        String sb = "win";
        boolean win = renderTarget.getHealth() >= mc.player.getHealth();

        RenderHelper.scissorStart(scaledX, scaledY, scaledWidth, scaledHeight);
        FontManager.semibold.drawString(nameTextSize, name, textX, textY, new Color(255,255,255), false);
        FontManager.semibold.drawString(winsize,  (win ? "lose" : "win"),  textX + scaledWidth - 42 - FontManager.semibold.getStringWidth((win ? "lose" : "win"),healthTextSize), textY,!win ? new Color(65, 252, 65, 150) : new Color(226, 87, 76, 150), false);
        RenderHelper.scissorEnd();
        float healthBarAlpha = 0.1f * scaleValue;
        RenderHelper.drawGradientAppleRoundedRectLR(healthBarX, healthBarY, healthBarWidth, healthBarHeight,ColorUtils.applyOpacity(Theme.getCurrentTheme().getColors().first,healthBarAlpha),ColorUtils.applyOpacity(Theme.getCurrentTheme().getColors().second,healthBarAlpha), 10 * scaleValue);

        float healthPercentage = renderTarget.getHealth() / renderTarget.getMaxHealth();
        float healthWidth = healthBarWidth * healthPercentage;

        animation1.animate(Math.max(healthBarHeight, healthWidth), 15);
        animation2.animate(Math.max(healthBarHeight, healthWidth), 30);

        float animatedHealth1 = Math.min(animation1.getOutput(), healthBarWidth);
        float animatedHealth2 = Math.min(animation2.getOutput(), healthBarWidth);

        float healthBarColorAlpha1 = 0.5f * scaleValue;
        float healthBarColorAlpha2 = 0.25f * scaleValue;

        RenderHelper.drawGradientAppleRoundedRectLR(healthBarX, healthBarY, animatedHealth1, healthBarHeight,ColorUtils.applyOpacity(Theme.getCurrentTheme().getColors().first,healthBarColorAlpha1),ColorUtils.applyOpacity(Theme.getCurrentTheme().getColors().second,healthBarColorAlpha1), 10 * scaleValue);
        RenderHelper.drawGradientAppleRoundedRectLR(healthBarX, healthBarY, animatedHealth2, healthBarHeight, ColorUtils.applyOpacity(Theme.getCurrentTheme().getColors().first,healthBarColorAlpha2), ColorUtils.applyOpacity(Theme.getCurrentTheme().getColors().second,healthBarColorAlpha2), 10 * scaleValue);
        FontManager.semibold.drawString(healthTextSize, (int) renderTarget.getHealth() + "hp",  healthBarX + healthBarWidth / 2 - FontManager.semibold.getStringWidth((int) renderTarget.getHealth() + "hp",healthTextSize) / 2, textY + 16 * scaleValue, new Color(255,255,255,180), false);
    }
}
