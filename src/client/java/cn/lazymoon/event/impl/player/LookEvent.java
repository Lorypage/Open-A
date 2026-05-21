package cn.lazymoon.event.impl.player;

import cn.lazymoon.event.api.event.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;

@Setter
@Getter
public class LookEvent implements Event {
    public Entity entity;
    public float yaw;
    public float pitch;

    public LookEvent(Entity entity, float yaw, float pitch) {
        this.entity = entity;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
