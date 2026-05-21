package cn.lazymoon.mixin.injector.network.packet.c2s.play;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface PlayerMoveC2SPacketAccessor {

    @Accessor("yaw")
    void setYaw(float yaw);

    @Accessor("pitch")
    void setPitch(float pitch);


    @Accessor("changeLook")
    void setRotating(boolean rotating);

    @Accessor("changeLook")
    boolean getRotating();
}
