package cn.lazymoon.mixin.injector.item;

import cn.lazymoon.features.module.impl.visual.OldHitting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinItem {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void handleSwordUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (OldHitting.blocking.get() && itemStack.getItem() instanceof SwordItem) {
            user.setCurrentHand(hand);
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    private void handleSwordUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (OldHitting.blocking.get() && stack.getItem() instanceof SwordItem) {
            cir.setReturnValue(UseAction.BLOCK);
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void handleSwordMaxUseTime(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (OldHitting.blocking.get() && stack.getItem() instanceof SwordItem) {
            cir.setReturnValue(72000);
        }
    }

}
