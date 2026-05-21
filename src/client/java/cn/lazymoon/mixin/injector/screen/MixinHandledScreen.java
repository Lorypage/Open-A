package cn.lazymoon.mixin.injector.screen;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.world.ContainerStealer;
import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class MixinHandledScreen implements InstanceAccess {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void drawScreenHead(CallbackInfo callbackInfo) {
        ContainerStealer containerStealer = Client.INSTANCE.getModuleManager().getModule(ContainerStealer.class);
        Screen screen = mc.currentScreen;
        if (containerStealer.isState() && ContainerStealer.isVanillaChest(screen) && ContainerStealer.cancelContainerGui.get() && ContainerStealer.container.isEnabled("Chest") && screen instanceof GenericContainerScreen) {
            mc.currentScreen = screen;
            callbackInfo.cancel();
        }
        if (containerStealer.isState() && ContainerStealer.cancelContainerGui.get() && ContainerStealer.container.isEnabled("Furnace") && screen instanceof FurnaceScreen) {
            mc.currentScreen = screen;
            callbackInfo.cancel();
        }
        if (containerStealer.isState() && ContainerStealer.cancelContainerGui.get() && ContainerStealer.container.isEnabled("BlastFurnace") && screen instanceof BlastFurnaceScreen) {
            mc.currentScreen = screen;
            callbackInfo.cancel();
        }
        if (containerStealer.isState() && ContainerStealer.cancelContainerGui.get() && ContainerStealer.container.isEnabled("SmokerFurnace") && screen instanceof SmokerScreen) {
            mc.currentScreen = screen;
            callbackInfo.cancel();
        }
        if (containerStealer.isState() && ContainerStealer.cancelContainerGui.get() && ContainerStealer.container.isEnabled("BrewingStand") && screen instanceof BrewingStandScreen) {
            mc.currentScreen = screen;
            callbackInfo.cancel();
        }
    }

}
