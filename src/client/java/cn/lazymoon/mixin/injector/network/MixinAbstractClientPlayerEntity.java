package cn.lazymoon.mixin.injector.network;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.visual.Camera;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayerEntity.class)
public class MixinAbstractClientPlayerEntity {

    @ModifyReturnValue(method = "getFovMultiplier", at = @At("RETURN"))
    private float injectFovMultiplier(float original) {
        if (Client.INSTANCE.getModuleManager().getModule(Camera.class).isState() && Camera.modifyFov.get()) {
            return Camera.fov.get().floatValue();
        }
        return original;
    }

}
