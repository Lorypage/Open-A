package cn.lazymoon.command;

import cn.lazymoon.utils.InstanceAccess;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Manager<T> implements InstanceAccess {

    protected List<T> elements;

    public Manager(List<T> elements) {
        this.elements = elements;
    }

    public Manager() {
        this.elements = new ArrayList<>();
    }
}
