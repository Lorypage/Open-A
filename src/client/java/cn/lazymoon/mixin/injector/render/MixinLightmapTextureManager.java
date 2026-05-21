package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.visual.AntiBlind;
import cn.lazymoon.utils.accessor.LightmapTextureManagerAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-04-02
 */
@Mixin({LightmapTextureManager.class})
public class MixinLightmapTextureManager implements LightmapTextureManagerAccess {
    @Unique private SimpleFramebuffer tintedFramebuffer;
    @Unique
    private boolean worldRendering = false;

    @Inject(method = "enable", at = @At("HEAD"), cancellable = true)
    private void enableTinted(CallbackInfo ci) {
        if (worldRendering) {
            RenderSystem.setShaderTexture(2, tintedFramebuffer.getColorAttachment());
            ci.cancel();
        }
    }

    @Redirect(method = "getDarknessFactor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/entity/effect/StatusEffectInstance;"))
    private StatusEffectInstance injectAntiDarkness(ClientPlayerEntity instance, RegistryEntry<StatusEffect> registryEntry) {
        if (!(Client.INSTANCE.getModuleManager().getModule(AntiBlind.class).isState() && AntiBlind.darkness.get())) {
            return null;
        }
        return instance.getStatusEffect(registryEntry);
    }

    @Override
    public void setWorldRendering(boolean value) {
        this.worldRendering = value;
    }
}
