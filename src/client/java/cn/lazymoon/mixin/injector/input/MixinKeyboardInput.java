package cn.lazymoon.mixin.injector.input;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends Input {

    @Shadow private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        } else {
            return positive ? 1.0F : -1.0F;
        }
    }

    @Inject(method = "tick",at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        MoveInputEvent moveInputEvent = new MoveInputEvent(movementForward,movementSideways, playerInput.jump(), playerInput.sneak());
        Client.INSTANCE.getEventManager().call(moveInputEvent);
        if (moveInputEvent.isCancelled()) {
            this.playerInput = new PlayerInput(false, false, false, false, false, false, false);
            movementForward = getMovementMultiplier(this.playerInput.forward(), this.playerInput.backward());
            movementSideways = getMovementMultiplier(this.playerInput.left(), this.playerInput.right());
        } else {
            movementForward = moveInputEvent.forward;
            movementSideways = moveInputEvent.strafe;
            this.playerInput = new PlayerInput(getKeyStateFromValue(movementForward), getKeyStateFromValue(-movementForward), getKeyStateFromValue(movementSideways), getKeyStateFromValue(-movementSideways), moveInputEvent.jump, moveInputEvent.sneak, getSprint(playerInput.sprint(), movementForward));
        }
    }

    @Unique
    private static boolean getKeyStateFromValue(float value) {
        if (value == 0) {
            return false;
        } else {
            return value > 0;
        }
    }

    @Unique
    private static boolean getSprint(boolean sprint,float forward) {
        if (forward <= 0) {
            return false;
        }
        return sprint;
    }

}
