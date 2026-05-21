package cn.lazymoon.features.module.impl.render.targethud;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.ingameui.ui.ModuleWidget;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.SmoothStepAnimation;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;

import static cn.lazymoon.utils.render.RenderHelper.context;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVG.nvgTranslate;

@ModuleInfo(name = "TargetHUD", description = "Shou you Attack Target on you game screen",key = 0, category = Category.Render,hidden = false)
public class TargetHUD extends ModuleWidget {

    public static Animation anim = new SmoothStepAnimation(250, 1f);

    private boolean lastDrawState = false;
    private LivingEntity lastTarget = null;

    @SuppressWarnings("unused")
    @Override
    public void render(RenderNvgEvent event) {
        float x = renderX, y = renderY;
        LivingEntity target = null;

        if (KillAura.target != null) {
            target = (LivingEntity) KillAura.target;
        } else if (mc.currentScreen instanceof ChatScreen) {
            target = mc.player;
        }

        LivingEntity renderTarget = target != null ? target : lastTarget;

        if (renderTarget == null) return;

        boolean canDraw = KillAura.target != null || mc.currentScreen instanceof ChatScreen;
        float scaleValue = anim.getOutput().floatValue();

        doRender(scaleValue, x, y, event.matrix4f(), renderTarget);
    }

    public void doRender(float scaleValue, float x, float y, Matrix4f matrix4f, LivingEntity renderTarget) {
        TargetHudRenderer.render(scaleValue, x, y,matrix4f, renderTarget,this);
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.isPost()) return;
        boolean canDraw = KillAura.target != null || mc.currentScreen instanceof ChatScreen;
        LivingEntity currentTarget = null;

        if (KillAura.target != null) {
            currentTarget = (LivingEntity) KillAura.target;
        } else if (mc.currentScreen instanceof ChatScreen) {
            currentTarget = mc.player;
        }

        boolean stateChanged = canDraw != lastDrawState;

        if (currentTarget != null) {
            lastTarget = currentTarget;
        }

        if (stateChanged) {
            anim.setDirection(canDraw ? Direction.FORWARDS : Direction.BACKWARDS);
        }

        lastDrawState = canDraw;
    }

    @Override
    public boolean shouldRender() {
        return isState();
    }
}
