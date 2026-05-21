package cn.lazymoon.event.impl.world;

import cn.lazymoon.event.api.event.Event;

public class SendMessageEvent implements Event {

    public String message;
    public SendMessageEvent(String message) {
        this.message = message;
    }

}
