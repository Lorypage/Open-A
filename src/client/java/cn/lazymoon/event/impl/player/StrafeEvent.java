package cn.lazymoon.event.impl.player;

import cn.lazymoon.event.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Setter
public class StrafeEvent extends CancellableEvent {
    private float yaw;
}
