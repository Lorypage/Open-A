package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.render.Render2DEvent;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.features.module.impl.render.Scoreboard;
import cn.lazymoon.features.module.impl.visual.AntiBlind;
import cn.lazymoon.utils.InstanceAccess;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinGuiIngame implements InstanceAccess {

    @Inject(method="renderHeldItemTooltip", at=@At(value="HEAD"))
    private void renderHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        Render2DEvent event = new Render2DEvent(context,1.0f);
        event.getContext().getMatrices().push();
        Client.INSTANCE.getEventManager().call(event);
        event.getContext().getMatrices().pop();
    }

    @Final
    @Unique
    private static final Identifier liquid_bounce$PUMPKIN_BLUR = Identifier.ofVanilla("misc/pumpkinblur");

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"),cancellable = true)
    private void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (InterFace.hidePotion.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(DrawContext context, Identifier texture, float opacity, CallbackInfo callback) {
        if (!Client.INSTANCE.getModuleManager().getModule(AntiBlind.class).isState()) {
            return;
        }

        if (!AntiBlind.pumpkin.get() && liquid_bounce$PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void hookNauseaOverlay(DrawContext context, float distortionStrength, CallbackInfo ci) {
        if (!(AntiBlind.nausea.get() && Client.INSTANCE.getModuleManager().getModule(AntiBlind.class).isState())) {
            ci.cancel();
        }
    }


    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"), cancellable = true)
    private void renderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 1) ScoreboardObjective scoreboardObjective) {
        if (scoreboardObjective != null) {
            Scoreboard scoreboard = Client.INSTANCE.getModuleManager().getModule(Scoreboard.class);
            if (scoreboard != null && scoreboard.isState()) {
                scoreboard.setObjective(scoreboardObjective);
                ci.cancel();
            }
        }
    }
}
