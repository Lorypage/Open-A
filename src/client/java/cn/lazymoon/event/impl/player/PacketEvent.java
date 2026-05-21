package cn.lazymoon.event.impl.player;

import cn.lazymoon.event.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.packet.Packet;

@Setter
@Getter
@AllArgsConstructor
public class PacketEvent extends CancellableEvent {
    @Setter
    public Packet<?> packet;
    private final PacketType state;

    public enum PacketType {
        Send, Receive, SyncReceive
    }

    public boolean isSend() {
        return state.equals(PacketType.Send);
    }

    public boolean isReceive() {
        return state.equals(PacketType.Receive);
    }

    public boolean isSyncReceive() {
        return state.equals(PacketType.SyncReceive);
    }
}
