package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.Render2DEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;

@ModuleInfo(name = "OldHitting",description = "Let you use the old Minecraft effects",key = 0,category = Category.Visual,hidden = false)
public class OldHitting extends Module {
    public static ModeValue cameraVersion = new ModeValue("Camera position version",new String[]{"Pre 1.8","Pre 1.13","Pre 1.14","Latest"},"Pre 1.8");
    public static BoolValue sneakTiming = new BoolValue("Sneak timing (Pre 1.21.3)",false);
    public static BoolValue cancelSwimming = new BoolValue("Cancel swimming (Pre 1.13)",false);
    public static BoolValue blocking = new BoolValue("Always sword blocking (Pre 1.9)",false);
    public static BoolValue noCooldown = new BoolValue("Remove item cooldown (Pre 1.9)",false);
    public static BoolValue swingWhileUsing = new BoolValue("Swing while using item (1.7)",false);

    public static BoolValue smoothSneaking = new BoolValue("Sneak interpolation (Since 1.13)",false);
    public static BoolValue alternativeSmoothSneaking = new BoolValue("Old sneak interpolation (1.7)",false);
    public static BoolValue fakeEyeHeight = new BoolValue("Old eye height",false);
    public static BoolValue bodyOffset = new BoolValue("Offset sneaking body position (1.7)",false);
    public static BoolValue syncPlayerModelWithEyeHeight = new BoolValue("Sync player model size (1.7)",false);
    public static BoolValue sneakAnimationWhileFlying = new BoolValue("Show sneak animation while flying (Pre 1.12)",false);

    @EventTarget
    public void onUpdate(Render2DEvent event) {
        if (mc.player == null) return;

        if (swingWhileUsing.get()) {
            if (mc.player.handSwinging) {
                return;
            }

            boolean using = mc.player.isUsingItem() && mc.options.attackKey.isPressed();

            if (!using) return;

            for (ItemStack stack : mc.player.getHandItems()){
                if (stack.getUseAction() != UseAction.NONE && mc.player.getActiveItem() == stack) {
                    mc.player.handSwinging = true;
                    mc.player.handSwingTicks = 0;
                    mc.player.preferredHand = Hand.MAIN_HAND;
                    return;
                }

                if (stack.getUseAction() != UseAction.NONE && mc.player.getActiveItem() == stack) {
                    mc.player.handSwinging = true;
                    mc.player.handSwingTicks = 0;
                    mc.player.preferredHand = Hand.OFF_HAND;
                }
            }
        }
    }


}