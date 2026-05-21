package cn.lazymoon.mixin.injector.client;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.input.ClickEvent;
import cn.lazymoon.event.impl.player.RotationAppliedEvent;
import cn.lazymoon.event.impl.player.RotationEvent;
import cn.lazymoon.event.impl.render.PreClientUpdateEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.event.impl.world.WorldEvent;
import cn.lazymoon.features.module.impl.movement.GuiMove;
import cn.lazymoon.features.module.impl.movement.NoSlow;
import cn.lazymoon.features.module.impl.visual.ESP;
import cn.lazymoon.features.module.impl.world.ContainerStealer;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientData;
import cn.lazymoon.utils.client.ClientUtil;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class JMixinMinecraftClient implements InstanceAccess {
    @Shadow
    @Final
    private Window window;

    @Shadow
    protected abstract String getWindowTitle();

    /** Inject Minecraft Loading */
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;onResolutionChanged()V"))
    private void injectClient(CallbackInfo callback) {
        Client.INSTANCE.load();
        mc.getWindow().setScaleFactor(2);
    }

    /** Inject Minecraft Stop */
    @Inject(method = "stop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;close()V", shift = At.Shift.AFTER))
    public void ShotDown(CallbackInfo ci) {
        Client.INSTANCE.shutdown();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 2))
    public void onPreClientUpdate(CallbackInfo callbackInfo) {
        Client.INSTANCE.getEventManager().call(new PreClientUpdateEvent());
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void updateWindowTitle() {
        this.window.setTitle(Client.Name + " Client " + Client.Versions + " for " + getWindowTitle() + " | 国内最好的Hypixel动态加速ip: fisproxy.org");
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void setWorld(ClientWorld world, CallbackInfo ci) {
        ClientUtil.reset();
        Client.INSTANCE.getEventManager().call(new WorldEvent());
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V"))
    private void redirectStopUsing(ClientPlayerInteractionManager manager, PlayerEntity player) {
        if (NoSlow.allowStopUsingItem()) {
            manager.stopUsingItem(player);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUseHead(CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;

        NoSlow.interactingBlockThisTick = false;

        if (!NoSlow.isHypixelLikeMode()) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem)) return;

        if (mc.crosshairTarget instanceof BlockHitResult bhr) {
            BlockState state = mc.world.getBlockState(bhr.getBlockPos());
            if (isInteractable(state)) {
                NoSlow.interactingBlockThisTick = true;
            }
        }
    }

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult injected$itemUse(ClientPlayerInteractionManager manager, PlayerEntity player, Hand hand) {
        if (Client.INSTANCE.getModuleManager().getModule(NoSlow.class).isState()
                && this.player != null
                && NoSlow.isHypixelLikeMode()
                && this.player.getMainHandStack().getItem() instanceof SwordItem) {
            return ActionResult.PASS;
        }
        return manager.interactItem(player, hand);
    }

    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;" +
            "interactBlock(" +
            "Lnet/minecraft/client/network/ClientPlayerEntity;" +
            "Lnet/minecraft/util/Hand;" +
            "Lnet/minecraft/util/hit/BlockHitResult;" +
            ")Lnet/minecraft/util/ActionResult;"))
    private ActionResult doItemUseRedirect(ClientPlayerInteractionManager manager, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (Client.INSTANCE.getModuleManager().getModule(NoSlow.class).isState()
                && NoSlow.isHypixelLikeMode()
                && player.getMainHandStack().getItem() instanceof SwordItem
                && !NoSlow.interactingBlockThisTick) {
            return ActionResult.PASS;
        }
        return manager.interactBlock(player, hand, hitResult);
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    public void onClick(CallbackInfo ci) {
        Client.INSTANCE.getEventManager().call(new ClickEvent());
    }

    @SuppressWarnings("DiscouragedShift")
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V", shift = At.Shift.BEFORE))
    public void hookRotation(CallbackInfo ci) {
        if (mc.player != null) {
            Client.INSTANCE.getEventManager().call(new RotationEvent());
            if (mc.currentScreen != null) {
                Client.INSTANCE.getEventManager().call(new RotationAppliedEvent());
            }
        }
    }

    @Redirect(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;unlockCursor()V")
    )
    private void onUnlockCursor(Mouse instance, Screen screen) {

        if (screen instanceof GenericContainerScreen && GuiMove.noUnlockMouse.get() && ContainerStealer.isVanillaChest(screen)) {
            return;
        }

        instance.unlockCursor();
    }

    @Inject(at = @At(value = "RETURN"), method = "hasOutline", cancellable = true)
    private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (ESP.shouldGlow(pEntity) && ESP.glow.get()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean isInteractable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock
                || block instanceof DoorBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof AnvilBlock
                || block instanceof BedBlock
                || block instanceof CraftingTableBlock
                || block instanceof EnderChestBlock
                || block instanceof FurnaceBlock;
    }

    @Inject(at = @At(value = "HEAD"), method = "tick")
    public void tickPre(CallbackInfo info) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround()) {
            ClientUtil.offGroundTicks = 0;
            ClientUtil.onGroundTicks++;
        } else {
            ClientUtil.onGroundTicks = 0;
            ClientUtil.offGroundTicks++;
        }

        if (mc.player != null) {
            if (mc.player.isOnGround()) {
                ClientData.setOnGroundTicks(ClientData.getOnGroundTicks() + 1);
                ClientData.setOffGroundTicks(0);
            } else {
                ClientData.setOnGroundTicks(0);
                ClientData.setOffGroundTicks(ClientData.getOffGroundTicks() + 1);
            }
        } else {
            ClientData.setOnGroundTicks(0);
            ClientData.setOffGroundTicks(0);
        }

        ClientUtil.updateFallDistance();
        Client.INSTANCE.getEventManager().call(new TickEvent(TickEvent.State.Pre));
    }

    @Inject(at = @At(value = "TAIL"), method = "tick")
    public void tickPost(CallbackInfo info) {
        if (mc.player == null || mc.world == null) return;
        Client.INSTANCE.getEventManager().call(new TickEvent(TickEvent.State.Post));
    }
}