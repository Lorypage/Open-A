package cn.lazymoon.event.impl.player;

import cn.lazymoon.event.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class MotionEvent extends CancellableEvent {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean horizontalCollision;
    private State state;

    public enum State {
        Pre, Post
    }

    public MotionEvent(State state) {
        this.state = state;
    }

    public boolean isPre() {
        return state.equals(State.Pre);
    }

    public boolean isPost() {
        return state.equals(State.Post);
    }
}