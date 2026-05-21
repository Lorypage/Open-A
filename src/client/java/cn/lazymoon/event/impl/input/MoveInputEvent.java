package cn.lazymoon.event.impl.input;

import cn.lazymoon.event.api.event.CancellableEvent;
import cn.lazymoon.event.api.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MoveInputEvent extends CancellableEvent {
    public float forward;
    public float strafe;
    public boolean jump;
    public boolean sneak;

    public MoveInputEvent(float forward, float strafe, boolean jump, boolean sneak) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }
}