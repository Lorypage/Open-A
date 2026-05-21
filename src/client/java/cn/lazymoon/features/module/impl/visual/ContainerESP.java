package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.Render3DEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.client.ClientData;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.player.PlayerUtils;
import cn.lazymoon.utils.render.RenderUtils;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.Arrays;

/**
 * @Author:Guyuemang
 * @Time:02-28
 */
@ModuleInfo(name = "ContainerESP",description = "Help you see all the containers in the world",key = 0,category = Category.Visual,hidden = false)
public class ContainerESP extends Module {
    public static ModeValue colorMode = new ModeValue("Color Mode","HUD", new String[]{"HUD", "Theme1", "Theme2"});
    private final NumberValue range = new NumberValue("Range", 120, 20, 360, 10);
    public static MultiBoolValue container = new MultiBoolValue("Container Addons", Arrays.asList(
            new BoolValue("Chest",true),
            new BoolValue("Furnace",false),
            new BoolValue("BlastFurnace",false),
            new BoolValue("SmokerFurnace", false),
            new BoolValue("BrewingStand", false)
    ));

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        Color sb = InterFace.color(1);
        sb = switch (colorMode.get()) {
            case "HUD" -> InterFace.color(1);
            case "Theme2" -> Theme.getCurrentTheme().getColors().second;
            case "Theme1" -> Theme.getCurrentTheme().getColors().first;
            default -> sb;
        };
        for (BlockEntity blockEntity : PlayerUtils.getBlockEntities(this.range.getValue())) {
            if ((blockEntity instanceof ChestBlockEntity && container.isEnabled("Chest")
                    || blockEntity instanceof FurnaceBlockEntity && container.isEnabled("Furnace")
                    || blockEntity instanceof BlastFurnaceBlockEntity && container.isEnabled("BlastFurnace")
                    || blockEntity instanceof SmokerBlockEntity && container.isEnabled("SmokerFurnace")
                    || blockEntity instanceof BrewingStandBlockEntity && container.isEnabled("BrewingStand")) && (!ClientData.clickedContainers.contains(blockEntity))) {
                RenderUtils.drawBox(event.getMatrix(), new Box(blockEntity.getPos()), ColorUtils.reAlpha(
                        sb
                        , 120), false, null, true);
            }
        }
    }
}
