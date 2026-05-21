package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.world.Scaffold;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.mixin.injector.accessor.MinecraftClientAccessor;
import net.minecraft.item.BedItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;

@ModuleInfo(name = "FastPlace",description = "Faster placement",key = 0,category = Category.Player, hidden = true)
public class FastPlace extends Module {
    private final NumberValue delay = new NumberValue("Delay", 0D, 0, 3, 1);
    private int originalRightClickDelay;

    @Override
    public void onEnable() {
        originalRightClickDelay = ((MinecraftClientAccessor) mc).getItemUseCooldown();
    }

    @Override
    public void onDisable() {
        ((MinecraftClientAccessor) mc).setItemUseCooldown(originalRightClickDelay);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (mc.player != null && mc.world != null) {
            if ((mc.player.getMainHandStack().getItem() == Items.COBWEB || mc.player.getOffHandStack().getItem() == Items.COBWEB)) {
                return;
            }

            if (Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState()) {
                return;
            }

            if ((mc.player.getMainHandStack().getItem() instanceof BlockItem && (!(mc.player.getMainHandStack().getItem() instanceof BedItem)) || mc.player.getOffHandStack().getItem() instanceof BlockItem && (!(mc.player.getMainHandStack().getItem() instanceof BedItem)) ) && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                if (((MinecraftClientAccessor) mc).getItemUseCooldown() != 0) {
                    ((MinecraftClientAccessor) mc).setItemUseCooldown(delay.get().intValue());
                }
            }
        }
    }
}
