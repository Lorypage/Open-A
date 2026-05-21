package cn.lazymoon.mixin.injector.network;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.world.SendCommandEvent;
import cn.lazymoon.event.impl.world.SendMessageEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonNetworkHandler implements ClientPlayPacketListener {
    @Shadow
    private ClientWorld world;

    protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @ModifyVariable(method = "sendCommand", at = @At("HEAD"), argsOnly = true)
    private String modifyCommand(String command) {
        SendCommandEvent event = new SendCommandEvent(command);
        Client.INSTANCE.getEventManager().call(event);
        return event.command;
    }

    @ModifyVariable(method = "sendChatCommand", at = @At("HEAD"), argsOnly = true)
    private String modifyChatCommand(String command) {
        SendCommandEvent event = new SendCommandEvent(command);
        Client.INSTANCE.getEventManager().call(event);
        return event.command;
    }

    @ModifyArg(method = "sendChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;<init>(Ljava/lang/String;Ljava/time/Instant;JLnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/network/message/LastSeenMessageList$Acknowledgment;)V"), index = 0)
    private String modifyChatMessage(String original) {
        SendMessageEvent sendMessageEvent = new SendMessageEvent(original);
        Client.INSTANCE.getEventManager().call(sendMessageEvent);
        return sendMessageEvent.message;
    }
}