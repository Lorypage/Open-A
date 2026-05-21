package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.utils.client.ClientUtil;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class MixinDynamic {
    @Shadow
    private float lastFrameDuration;

    @Shadow
    private float tickDelta;

    @Shadow
    private long prevTimeMillis;
    
    @Final
    @Shadow
    private float tickTime;

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
    private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        this.lastFrameDuration = ((timeMillis - this.prevTimeMillis) / this.tickTime) * ClientUtil.timer;
        this.prevTimeMillis = timeMillis;
        this.tickDelta += this.lastFrameDuration;
        int i = (int) this.tickDelta;
        this.tickDelta -= i;
        cir.setReturnValue(i);
    }
}