package cn.lazymoon.mixin.injector.interaction;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.input.EventClick;
import cn.lazymoon.event.impl.player.AttackEvent;
import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager implements InstanceAccess {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void attackEntityPre(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(target, AttackEvent.State.Pre);
        Client.INSTANCE.getEventManager().call(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "attackEntity", at = @At("RETURN"))
    private void attackEntityPost(PlayerEntity player, Entity target, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(target, AttackEvent.State.Post);
        Client.INSTANCE.getEventManager().call(event);
    }

    @SuppressWarnings("DiscouragedShift")
    @Inject(method = "updateBlockBreakingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V", shift = At.Shift.BEFORE))
    private void onClickBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (mc == null || mc.world == null || mc.player == null) return;
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isAir()) {
            Client.INSTANCE.getEventManager().call(new EventClick(pos,direction));
        }
    }
}
