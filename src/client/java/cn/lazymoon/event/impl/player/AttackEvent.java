package cn.lazymoon.event.impl.player;

import cn.lazymoon.event.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public class AttackEvent extends CancellableEvent {
    private final Entity entity;
    private AttackEvent.State state;

    public enum State {
        Pre, Post
    }

    public boolean isPre() {
        return state.equals(AttackEvent.State.Pre);
    }

    public boolean isPost() {
        return state.equals(AttackEvent.State.Post);
    }
}
