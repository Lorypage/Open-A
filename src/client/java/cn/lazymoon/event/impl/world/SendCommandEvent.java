package cn.lazymoon.event.impl.world;

import cn.lazymoon.event.api.event.Event;

public class SendCommandEvent implements Event {

    public String command;
    public SendCommandEvent(String command) {
        this.command = command;
    }

}
