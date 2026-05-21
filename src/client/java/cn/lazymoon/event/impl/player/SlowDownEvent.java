package cn.lazymoon.event.impl.player;

import cn.lazymoon.event.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SlowDownEvent extends CancellableEvent {
    public float sideways;
    public float forward;
}
