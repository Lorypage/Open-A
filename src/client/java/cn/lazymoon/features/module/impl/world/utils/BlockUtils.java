package cn.lazymoon.features.module.impl.world.utils;

import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class BlockUtils implements InstanceAccess {
    public static BlockPos pos(double x, double y, double z) {
        return (new BlockPos(MathHelper.floor(x),MathHelper.floor(y),MathHelper.floor(z)));
    }
    public static Map<BlockPos, Block> searchBlocks(int radius) {
        Map<BlockPos, Block> blocks = new HashMap<>();

        if (ClientUtils.isNull()) return blocks;

        World world = mc.world;
        Vec3d playerPos = mc.player.getPos();
        BlockPos playerBlockPos = mc.player.getBlockPos();

        int r = radius;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerBlockPos.add(x, y, z);

                    double distance = playerPos.distanceTo(Vec3d.ofCenter(pos));

                    if (distance <= radius) {
                        Block block = world.getBlockState(pos).getBlock();
                        blocks.put(pos, block);
                    }
                }
            }
        }

        return blocks;
    }
    public static double getCenterDistance(BlockPos blockPos) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return Double.MAX_VALUE;
        Vec3d center = Vec3d.ofCenter(blockPos);
        return player.squaredDistanceTo(center);
    }
}
