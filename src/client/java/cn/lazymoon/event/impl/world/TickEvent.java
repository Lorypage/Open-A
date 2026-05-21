package cn.lazymoon.event.impl.world;

import cn.lazymoon.event.api.event.CancellableEvent;
import cn.lazymoon.event.impl.player.MotionEvent;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class TickEvent extends CancellableEvent {
    private TickEvent.State state;

    public enum State {
        Pre, Post
    }

    public TickEvent(TickEvent.State state) {
        this.state = state;
    }

    public boolean isPre() {
        return state.equals(TickEvent.State.Pre);
    }

    public boolean isPost() {
        return state.equals(TickEvent.State.Post);
    }
}
