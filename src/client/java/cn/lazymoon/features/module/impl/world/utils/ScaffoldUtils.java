package cn.lazymoon.features.module.impl.world.utils;

import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.block.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @Author:Guyuemang
 * @Time:03-08
 */
public class ScaffoldUtils implements InstanceAccess {
    public enum SearchMode {
        Normal, Hypixel
    }

    public static PlaceInfo getPlaceInfo(BlockPos origin, SearchMode mode) {
        return switch (mode) {
            case Normal -> searchNormal(origin);
            case Hypixel -> searchHypixel(origin);
        };
    }

    public static PlaceInfo searchHypixel(BlockPos original) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d feetPos = mc.player.getPos();
        int baseY = original.getY();
        BlockPos playerPos = BlockPos.ofFloored(feetPos.x, baseY, feetPos.z);

        BlockPos bestPos = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int x = playerPos.getX() - 5; x <= playerPos.getX() + 5; ++x) {
            for (int y = baseY - 1; y <= baseY; ++y) {
                for (int z = playerPos.getZ() - 5; z <= playerPos.getZ() + 5; ++z) {
                    mutablePos.set(x, y, z);

                    BlockState state = mc.world.getBlockState(mutablePos);
                    VoxelShape shape = state.getCollisionShape(mc.world, mutablePos);
                    if (shape.isEmpty()) continue;
                    if (isInteractable(mutablePos)) continue;

                    Box box = shape.getBoundingBox();
                    double ex = MathHelper.clamp(feetPos.x, x + box.minX, x + box.maxX);
                    double ey = MathHelper.clamp(feetPos.y, y + box.minY, y + box.maxY);
                    double ez = MathHelper.clamp(feetPos.z, z + box.minZ, z + box.maxZ);

                    double score = feetPos.squaredDistanceTo(ex, ey, ez);
                    if (score < bestScore) {
                        bestScore = score;
                        bestPos = new BlockPos(x, y, z);
                    }
                }
            }
        }

        if (bestPos == null) return null;

        Direction side = getPlaceSide(bestPos, baseY, feetPos);
        return side != null ? new PlaceInfo(bestPos, side) : null;
    }

    public static final Set<BlockPos> searchingBlocks = new HashSet<>();
    private static final Direction[] directions = new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP};

    private static boolean isAttachable(final BlockPos pos) {
        if (mc == null || mc.world == null || pos == null) {
            return false;
        }

        var state = mc.world.getBlockState(pos);

        return !state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty();
    }

    private record BlockInfo(BlockPos pos, int depth) {
        // empty.
    }

    public static PlaceInfo searchNormal(BlockPos blockPos) {
        if (mc == null || mc.world == null || blockPos == null) {
            return null;
        }
        searchingBlocks.clear();
        var blockInfos = new ArrayDeque<BlockInfo>();

        for (Direction face : directions) {
            var neighbor = blockPos.offset(face.getOpposite());
            searchingBlocks.add(neighbor);
            if (isAttachable(neighbor)) {
                return new PlaceInfo(neighbor, face);
            }

            blockInfos.addLast(new BlockInfo(neighbor, 1));
        }

        while (!blockInfos.isEmpty()) {
            var cur = blockInfos.removeFirst();

            if (cur.depth >= 4) {
                continue;
            }

            for (Direction nextFace : directions) {
                var next = cur.pos.offset(nextFace.getOpposite());
                searchingBlocks.add(next);
                if (isAttachable(next)) {
                    return new PlaceInfo(next, nextFace);
                }

                blockInfos.addLast(new BlockInfo(next, cur.depth + 1));
            }
        }

        for (int i = 1; i <= 2; i++) {
            var below = blockPos.down(i);
            searchingBlocks.add(below);
            if (isAttachable(below)) {
                return new PlaceInfo(below, Direction.UP);
            }
        }

        return null;
    }

    private static final int[][] OFFSETS = {
            {0, 1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    private static final Direction[] FACINGS = {
            Direction.UP,
            Direction.EAST,
            Direction.WEST,
            Direction.SOUTH,
            Direction.NORTH
    };

    private static Direction getPlaceSide(BlockPos blockPos, int baseY, Vec3d feetPos) {
        int playerBlockX = MathHelper.floor(feetPos.x);
        int playerBlockY = baseY + 1;
        int playerBlockZ = MathHelper.floor(feetPos.z);

        boolean isJumping = !mc.player.isOnGround()
                && InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode());

        double bestDistSq = Double.MAX_VALUE;
        Direction bestFacing = null;

        BlockPos.Mutable bp = new BlockPos.Mutable();
        int bx = blockPos.getX();
        int by = blockPos.getY();
        int bz = blockPos.getZ();

        for (int i = 0; i < OFFSETS.length; i++) {
            if (i == 0 && !isJumping) continue;

            int[] offset = OFFSETS[i];
            int testX = bx + offset[0];
            int testY = by + offset[1];
            int testZ = bz + offset[2];

            if (testX == playerBlockX && testY == playerBlockY && testZ == playerBlockZ) continue;

            bp.set(testX, testY, testZ);
            Direction facing = FACINGS[i];

            if (!canPlaceAt(bp)) continue;

            if (i == 0 && !checkBlock2(bp, facing)) continue;

            if (!canPlaceBlockOnSide(mc.player.getMainHandStack(), bp, facing)) continue;

            Vec3d hit = getBestHitFeet(bp, feetPos);
            double distSq = feetPos.squaredDistanceTo(hit);

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestFacing = facing;
            }
        }

        if (bestFacing != null) {
            Vec3d currentHit = getBestHitFeet(blockPos, feetPos);
            double currentDistSq = feetPos.squaredDistanceTo(currentHit);
            return (bestDistSq < currentDistSq) ? bestFacing : null;
        }

        return null;
    }

    private static boolean checkBlock2(BlockPos bp, Direction facing) {
        Vec3d center = Vec3d.ofCenter(bp);
        Vec3i dir = facing.getVector();
        Vec3d hit = center.add(dir.getX() * 0.5, dir.getY() * 0.5, dir.getZ() * 0.5);

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d relevant = hit.subtract(eyePos);
        Vec3d faceNormal = new Vec3d(-dir.getX(), -dir.getY(), -dir.getZ());

        return relevant.lengthSquared() <= 20.25
                && relevant.normalize().dotProduct(faceNormal.normalize()) >= 0.0;
    }

    private static Vec3d getBestHitFeet(BlockPos pos, Vec3d feetPos) {
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(mc.world, pos);

        if (shape.isEmpty()) {
            double ex = MathHelper.clamp(feetPos.x, pos.getX() + 0.05, pos.getX() + 0.95);
            double ey = MathHelper.clamp(feetPos.y, pos.getY() - 0.05, pos.getY() + 1.05);
            double ez = MathHelper.clamp(feetPos.z, pos.getZ() + 0.05, pos.getZ() + 0.95);
            return new Vec3d(ex, ey, ez);
        }

        Box box = shape.getBoundingBox();
        double ex = MathHelper.clamp(feetPos.x, pos.getX() + box.minX + 0.05, pos.getX() + box.maxX - 0.05);
        double ey = MathHelper.clamp(feetPos.y, pos.getY() + box.minY - 0.05, pos.getY() + box.maxY + 0.05);
        double ez = MathHelper.clamp(feetPos.z, pos.getZ() + box.minZ + 0.05, pos.getZ() + box.maxZ - 0.05);
        return new Vec3d(ex, ey, ez);
    }

    private static boolean canPlaceBlockOnSide(ItemStack stack, BlockPos pos, Direction side) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return false;

        ItemPlacementContext ctx = new ItemPlacementContext(
                mc.world, mc.player, Hand.MAIN_HAND, stack,
                new BlockHitResult(Vec3d.ofCenter(pos), side, pos, false)
        );
        return blockItem.getBlock().getPlacementState(ctx) != null;
    }

    public static boolean canPlaceAt(BlockPos pos) {
        if (ClientUtils.isNull()) {
            return false;
        }

        var blockState = Objects.requireNonNull(mc.world).getBlockState(pos);

        if (blockState.isAir()) {
            return true;
        }

        if (blockState.isOf(Blocks.SNOW) && blockState.contains(SnowBlock.LAYERS)) {
            return blockState.get(SnowBlock.LAYERS) <= 1;
        }

        return blockState.canReplace(new AutomaticItemPlacementContext(mc.world, pos, Direction.UP, ItemStack.EMPTY, Direction.UP));
    }

    private static boolean isInteractable(BlockPos blockPos) {
        if (ClientUtils.isNull()) {
            return false;
        }

        var block = Objects.requireNonNull(mc.world).getBlockState(blockPos).getBlock();

        if (block instanceof BlockEntityProvider) {
            return true;
        } else if (block instanceof CraftingTableBlock) {
            return true;
        } else if (block instanceof AnvilBlock) {
            return true;
        } else if (block instanceof BedBlock) {
            return true;
        } else if (block instanceof DoorBlock) {
            return true;
        } else if (block instanceof TrapdoorBlock) {
            return true;
        } else if (block instanceof FenceGateBlock) {
            return true;
        } else if (block instanceof FenceBlock) {
            return true;
        } else if (block instanceof ButtonBlock) {
            return true;
        } else if (block instanceof LeverBlock) {
            return true;
        }

        return block instanceof JukeboxBlock;
    }
}
