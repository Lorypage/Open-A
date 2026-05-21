package cn.lazymoon.mixin.injector.world;

import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements InstanceAccess {

    @Inject(method = "tickEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;resetPosition()V", shift = At.Shift.AFTER), cancellable = true)
    public void tickEntity(Entity entity, CallbackInfo ci) {
        if (ClientUtil.skipTicks > 0 && entity == mc.player) {
            ClientUtil.skipTicks--;
            ci.cancel();
        }
    }

    @Inject(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(PlayerEntity player, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed, CallbackInfo ci) {
        if (InterFace.noHitSound.getValue()) {
            if (isHitSound(sound)) {
                ci.cancel();
            }
        }
    }

    private boolean isHitSound(RegistryEntry<SoundEvent> soundEntry) {
        SoundEvent sound = soundEntry.value();
        return sound == SoundEvents.ENTITY_PLAYER_ATTACK_STRONG
                || sound == SoundEvents.ENTITY_PLAYER_ATTACK_WEAK
                || sound == SoundEvents.ENTITY_PLAYER_ATTACK_CRIT
                || sound == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK
                || sound == SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP
                || sound == SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE;
    }
}
