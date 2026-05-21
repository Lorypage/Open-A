package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.Render3DEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.render.RenderUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.awt.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "BedESP",description = "Show the position of the bed",key = 0,category = Category.Visual,hidden = false)
public class BedPlates extends Module {
    private final NumberValue radius = new NumberValue("Radius", 10, 1, 100,1);
    public static ModeValue color = new ModeValue("Render Block Color Mode","HUD",new String[]{"HUD","Theme1","Theme2"});

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (ClientUtils.isNull()) return;

        World world = mc.world;
        BlockPos playerPos = mc.player.getBlockPos();

        int r = radius.get().intValue();

        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, r, 5, r)) {
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock bed)) continue;

            if (state.get(BedBlock.PART) != BedPart.HEAD)
                continue; // 只渲染床头

            Direction facing = state.get(BedBlock.FACING);
            BlockPos footPos = pos.offset(facing.getOpposite());

            // 合并 AABB
            double minX = Math.min(pos.getX(), footPos.getX());
            double minY = Math.min(pos.getY(), footPos.getY());
            double minZ = Math.min(pos.getZ(), footPos.getZ());
            double maxX = Math.max(pos.getX(), footPos.getX()) + 1.0;
            double maxY = Math.max(pos.getY(), footPos.getY()) + 0.5625;
            double maxZ = Math.max(pos.getZ(), footPos.getZ()) + 1.0;

            Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
            Color sb = InterFace.color(1);
            sb = switch (color.get()) {
                case "HUD" -> ColorUtils.reAlpha(InterFace.color(0), 100);
                case "Theme2" -> ColorUtils.reAlpha(Theme.getCurrentTheme().getColors().first, 100);
                case "Theme1" -> ColorUtils.reAlpha(Theme.getCurrentTheme().getColors().second, 100);
                default -> sb;
            };
            RenderUtils.drawBox(event.getMatrix(), box, sb,false, null, true);
        }
    }
}
