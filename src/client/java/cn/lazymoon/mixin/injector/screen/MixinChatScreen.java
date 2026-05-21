package cn.lazymoon.mixin.injector.screen;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.misc.MessageEvent;
import cn.lazymoon.event.impl.render.ChatGuiEvent;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.ingameui.ui.ModuleWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ChatScreen.class)
public class MixinChatScreen extends Screen {

    protected MixinChatScreen(Text title) {
        super(title);
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        MessageEvent messageEvent = new MessageEvent(chatText);
        Client.INSTANCE.getEventManager().call(messageEvent);
        // Client.commandManager.receive(messageEvent);
        if (messageEvent.isCancelled()) {
            Objects.requireNonNull(this.client).inGameHud.getChatHud().addToMessageHistory(chatText);
            Objects.requireNonNull(this.client).setScreen(null);
            ci.cancel();
        }
    }

    @Inject(method = "render",at = @At("HEAD"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci){
        ChatGuiEvent event = new ChatGuiEvent(context,mouseX,mouseY);
        Client.INSTANCE.getEventManager().call(event);

        ModuleWidget draggingWidget = null;
        for (cn.lazymoon.features.module.Module module : Client.INSTANCE.getModuleManager().getAllModules()) {
            if (module instanceof ModuleWidget && module.isState()) {
                ModuleWidget widget = (ModuleWidget) module;
                if (widget.shouldRender() && widget.dragging) {
                    draggingWidget = widget;
                    break;
                }
            }
        }

        for (Module module : Client.INSTANCE.getModuleManager().getAllModules()) {
            if (module instanceof ModuleWidget && module.isState()) {
                ModuleWidget widget = (ModuleWidget) module;
                if (widget.shouldRender()) {
                    widget.onChatGUI(event.getMouseX(), event.getMouseY(),
                            (draggingWidget == null || draggingWidget == widget));
                    if (widget.dragging) draggingWidget = widget;
                }
            }
        }
    }

}
