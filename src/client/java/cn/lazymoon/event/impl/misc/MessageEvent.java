package cn.lazymoon.event.impl.misc;

import cn.lazymoon.event.api.event.CancellableEvent;
import lombok.Getter;

@Getter
public class MessageEvent extends CancellableEvent {

    private final String message;

    public MessageEvent(String message) {
        this.message = message;
    }

}
