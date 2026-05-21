package cn.lazymoon.event.impl.input;

import cn.lazymoon.event.api.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Setter
@Getter
@AllArgsConstructor
public class KeyEvent implements Event {
    private int key;
}
