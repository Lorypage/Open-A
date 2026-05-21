package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.value.impl.BoolValue;
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
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.MinecraftClient;

import java.awt.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "WaterMark",description = "Shou you watermark on you game screen",key = 0,category = Category.Render,hidden = false)
public class WaterMark extends ModuleWidget {
    private ModeValue mode = new ModeValue("Mode","Type1",new String[]{"Type1"});

    @Override
    public void render(RenderNvgEvent event) {
        int x = (int) renderX;
        int y = (int) renderY;
        int w = 0;
        int h = 0;

        switch (mode.get()) {
            case "Type1":
                w = (int) (23.5f + FontManager.icon.getStringWidth("A", 25)
                        + FontManager.semibold.getStringWidth("Arcane", 23)
                        + FontManager.semibold.getStringWidth(" · " + mc.getCurrentFps() + "fps", 15)
                        + 3);
                h = 25;
                Type1(x,y,w,event);
                break;
        }
        setWidth(w);
        setHeight(h);
    }

    public void Type1(float x,float y,float w,RenderNvgEvent event){
       onblur(x,y,w,25,event);

        if (PostProcessing.bloom.get()) {
            RenderHelper.drawRoundRectBloomApple(x, y, w, 25, 10, 2, new Color(255, 255, 255, 10));
        }
        RenderHelper.drawGradientAppleRoundedRectLR(x, y, w, 25, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),10);

        RenderHelper.scissorStart(x, y, 57 / 2, 30);
        RenderHelper.drawAppleRoundedRect(x, y, w, 25, new Color(255, 255, 255, 40), 10);
        RenderHelper.scissorEnd();

        FontManager.icon.drawGlowString(25, "A", x + 8, y + 5, InterFace.color(12), InterFace.color(12), false, 1);
        FontManager.semibold.drawStringmiddleY(23, "Arcane", x + 57 / 2 + 16 - 12, y + 33 - 10 - 12, new Color(255, 255, 255), false);
        FontManager.semibold.drawStringmiddleY(15, " · " + mc.getCurrentFps() + "fps", x + 57 / 2 + 16 + FontManager.semibold.getStringWidth("Arcane", 23) - 12, y + 34 - 10 - 12, new Color(255, 255, 255,180), false);
    }

    private void onblur(float x,float y,float w,float h,RenderNvgEvent event){
        float smoothness = 5f;
        if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
            BuiltBlur blur = Builder.blur()
                    .size(new SizeState(w + smoothness, h + smoothness))
                    .radius(new QuadRadiusState(10))
                    .blurRadius(20)
                    .smoothness(smoothness)
                    .color(QuadColorState.TRANSPARENT)
                    .position(new PositionState((x) - smoothness / 2f
                            , (y) - smoothness / 2f))
                    .matrix4f(event.matrix4f())
                    .build();
            BlurTaskInstance.addTask(blur);
        }
    }

    @Override
    public boolean shouldRender() {
        return isState();
    }
}
