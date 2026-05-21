package cn.lazymoon.features.module.impl.world;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import cn.lazymoon.event.impl.player.RotationAppliedEvent;
import cn.lazymoon.event.impl.player.RotationEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.world.utils.BlockUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.utils.entity.RaycastUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.player.PlayerUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.Vector2f;

import java.util.*;

@ModuleInfo(name = "Eliminate", description = "Help you eliminate dangerous things within a distance",key = 0, category = Category.World,hidden = false)
public class Eliminate extends Module {
    public static BoolValue blockLava = new BoolValue("Lava",true);
    public static BoolValue breakFire = new BoolValue("Fire",true);
    public static BoolValue place = new BoolValue("Water",true);

    record PlaceData(BlockPos pos, Box bb) {
    }

    public static boolean interactRequired = false;
    public static int oldSlot = -1;
    public static boolean shouldReceive = false;
    private static PlaceData lastData;
    public static boolean canRotation;
    public static int lastSlot = -1;
    public static boolean needPlaceWater;
    public static boolean placed;
    public static boolean set;
    public static Vec3d pos;

    @Override
    public void onEnable() {
        interactRequired = false;
        shouldReceive = false;
        set = false;
        canRotation = false;
        lastSlot = -1;
        placed = false;
        needPlaceWater = false;
    }

    @Override
    public void onDisable() {
        interactRequired = false;
        shouldReceive = false;
        lastData = null;
    }

    @EventTarget
    public void onRotation(RotationEvent event) {
        if (mc.player == null || PlayerUtils.ticksSinceTeleport < 3) return;
        if (mc.player.isCreative()) return;
        if (mc.player.isUsingItem() || mc.currentScreen != null) return;

        if (place.get()) {
            boolean shouldPlace = mc.player.isOnFire() && !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && nextTickWillLanding() && !mc.player.isInsideWaterOrBubbleColumn() && nextTickWillLanding() && KillAura.target == null && !Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState();

            if (shouldPlace) {
                int waterBucketSlot = PlayerUtils.findSlot(Items.WATER_BUCKET);
                if (waterBucketSlot == -1) {
                    shouldPlace = false;
                }
                placeWaterBucket(waterBucketSlot);
            }
            if (!shouldPlace && hasEmptyBucket() && shouldReceive) {
                retrieveWaterBlock();
            }
        }
    }

    @EventTarget
    public void onRotationApplied(RotationAppliedEvent event) {
        if (RotationComponent.targetRotation == null || mc.player == null || mc.interactionManager == null) return;

        if (place.get()) {
            if (interactRequired) {
                BlockHitResult result = RaycastUtils.rayCast(new Vector2f(RotationComponent.targetRotation.getYaw(), RotationComponent.targetRotation.getPitch()), 4.5);
                if (result == null) {
                    if (mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem().equals(Items.BUCKET)) {
                        shouldReceive = true;
                    }
                    if (oldSlot != -1) {
                        mc.player.getInventory().selectedSlot = oldSlot;
                        oldSlot = -1;
                    }
                    return;
                }

                interactRequired = false;
                mc.crosshairTarget = result;
                float currentYaw = mc.player.getYaw();
                float currentPitch = mc.player.getPitch();
                mc.player.setYaw(RotationComponent.targetRotation.getYaw());
                mc.player.setPitch(RotationComponent.targetRotation.getPitch());

                var res = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.setYaw(currentYaw);
                mc.player.setPitch(currentPitch);
                if (res.isAccepted()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                if (mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem().equals(Items.BUCKET)) {
                    shouldReceive = true;
                }
                if (oldSlot != -1) {
                    mc.player.getInventory().selectedSlot = oldSlot;
                    oldSlot = -1;
                }
            }
        }
    }

    @EventTarget
    private void onMovementInput(MoveInputEvent event) {
        if (lastData != null && mc.player != null && place.get()) {
            mc.player.setSprinting(false);
        }
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.interactionManager == null || Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState()) return;

        if (blockLava.get() && notInStillLava() && findBlock() != -1) {

            for (Map.Entry<BlockPos, Block> block : BlockUtils.searchBlocks(5).entrySet()) {
                BlockPos blockpos = block.getKey();

                if (notStillLava(mc.world, blockpos)) continue;
                if (hasAnyEntityIntersectingBlock(blockpos)) continue;

                Direction[] faces = new Direction[]{
                        Direction.UP,
                        Direction.DOWN,
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST
                };

                BlockPos[] candidates = new BlockPos[]{
                        blockpos.up(),
                        blockpos.down(),
                        blockpos.north(),
                        blockpos.south(),
                        blockpos.east(),
                        blockpos.west()
                };

                Vec3d[] aimVec3d = new Vec3d[]{
                        new Vec3d(blockpos.getX() + 0.5, blockpos.getY() + 1.0, blockpos.getZ() + 0.5), // up
                        new Vec3d(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5),       // down
                        new Vec3d(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ()),       // north
                        new Vec3d(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 1.0), // south
                        new Vec3d(blockpos.getX() + 1.0, blockpos.getY() + 0.5, blockpos.getZ() + 0.5), // east
                        new Vec3d(blockpos.getX(), blockpos.getY() + 0.5, blockpos.getZ() + 0.5)        // west
                };

                BlockPos bestPos = null;
                Vec3d bestAim = null;
                Direction bestFace = null;

                for (int i = 0; i < 6; i++) {
                    BlockPos placePos = candidates[i];
                    Direction expectedFace = faces[i];

                    Vec3d aimVec = aimVec3d[i].add(
                            expectedFace.getOffsetX() * 0.01,
                            expectedFace.getOffsetY() * 0.01,
                            expectedFace.getOffsetZ() * 0.01
                    );

                    // 必须是可作为放置面的方块
                    if (!mc.world.getFluidState(placePos).isEmpty()) continue;
                    if (mc.world.getBlockState(placePos).isAir()) continue;
                    if (!mc.world.getBlockState(placePos)
                            .isSideSolidFullSquare(mc.world, placePos, expectedFace)) continue;

                    // 距离检查
                    if (mc.player.getEyePos().distanceTo(aimVec) > 4.5f) continue;

                    // 预测射线
                    Rotation rot = RotationUtils.toRotation(aimVec, false);
                    BlockHitResult predictHit = RaycastUtils.rayCastBlock(rot, mc.player, mc.world, 4.5f);

                    if (predictHit == null) continue;
                    if (!predictHit.getBlockPos().equals(placePos)) continue;
                    if (predictHit.getSide() != expectedFace.getOpposite()) continue;

                    bestPos = placePos;
                    bestAim = aimVec;
                    bestFace = expectedFace;
                    break;
                }

                if (bestPos == null) continue;

                Rotation rotation = RotationUtils.toRotation(bestAim, false);
                Client.INSTANCE.getRotationComponent().setRotations(rotation, 1, MovementFix.SILENT);

                BlockHitResult hit = RaycastUtils.rayCastBlock(
                        RotationComponent.serverRotation, mc.player, mc.world, 4.5F
                );

                if (hit == null) continue;
                if (!hit.getBlockPos().equals(bestPos)) continue;
                if (hit.getSide() != bestFace.getOpposite()) continue;

                int lastSlot = mc.player.getInventory().selectedSlot;
                int blockSlot = findBlock();
                if (blockSlot == -1) continue;

                mc.player.getInventory().selectedSlot = blockSlot;
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = lastSlot;
            }
        }


        if (breakFire.get()) {
            /// SearchBlock对火方块的方位进行检查
            for (Map.Entry<BlockPos, Block> block : BlockUtils.searchBlocks(5).entrySet()) {
                BlockPos blockpos = block.getKey();
                Vec3d vec3d = new Vec3d(blockpos.getX() + 0.5,blockpos.getY(),blockpos.getZ() + 0.5);
                Block blocks = block.getValue();
                if (blocks instanceof FireBlock && mc.player.getEyePos().distanceTo(vec3d) <= 4.5F) {
                    /// 转头对准
                    Rotation rotation = RotationUtils.toRotation(vec3d,false);

                    BlockHitResult predictHit = RaycastUtils.rayCastBlock(rotation, mc.player, mc.world, 4.5F);

                    /// 预测是否可触及到火焰方块
                    if (predictHit == null || !predictHit.getBlockPos().equals(blockpos)) {
                        continue;
                    }

                    Client.INSTANCE.getRotationComponent().setRotations(rotation, 1, MovementFix.SILENT);

                    BlockHitResult hit = RaycastUtils.rayCastBlock(RotationComponent.serverRotation, mc.player, mc.world, 4.5F);

                    if (hit == null) continue;

                    if (hit.getBlockPos().equals(blockpos)) {
                        mc.interactionManager.attackBlock(blockpos,Direction.UP);
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            }
        }
    }

    public static boolean hasAnyEntityIntersectingBlock(BlockPos blockPos) {
        World world = mc.world;

        if (world == null) return false;

        Box checkBox = new Box(blockPos);

        for (Entity entity : world.getOtherEntities(null, checkBox)) {

            if (!entity.isAlive()) continue;
            if (entity.isSpectator()) continue;
            if (!entity.canHit()) continue;

            Box box = entity.getBoundingBox();

            int minX = MathHelper.floor(box.minX);
            int maxX = MathHelper.floor(box.maxX);
            int minY = MathHelper.floor(box.minY);
            int maxY = MathHelper.floor(box.maxY);
            int minZ = MathHelper.floor(box.minZ);
            int maxZ = MathHelper.floor(box.maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (blockPos.getX() == x
                                && blockPos.getY() == y
                                && blockPos.getZ() == z) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }



    public static boolean notStillLava(World world, BlockPos pos) {
        FluidState fluid = world.getFluidState(pos);
        return !fluid.isIn(FluidTags.LAVA) || !fluid.isStill();
    }

    public static int findBlock() {
        if (mc.player == null) return -1;
        int blockSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() instanceof BlockItem) {
                blockSlot = i;
            }
        }
        return blockSlot;
    }

    private static void retrieveWaterBlock() {
        if (mc.player == null) return;

        oldSlot = mc.player.getInventory().selectedSlot;
        int emptyBucketSlot = PlayerUtils.findSlot(Items.BUCKET);
        if (emptyBucketSlot == -1) return;
        BlockPos pos = findScoopableWaterBlock();
        if (pos == null) {
            return;
        }
        var r = getLookAtWaterBlock(pos);

        if (mc.player.getMainHandStack().getItem() != Items.BUCKET)
            mc.player.getInventory().selectedSlot = emptyBucketSlot;
        interactItem(r);
        shouldReceive = false;
    }

    public static BlockPos findScoopableWaterBlock() {
        if (mc.player == null) return null;

        World world = mc.player.getWorld();
        BlockPos playerBlockPos = mc.player.getBlockPos();
        double maxReachDistance = mc.player.getBlockInteractionRange();
        int reach = (int) Math.ceil(maxReachDistance);
        List<BlockPos> possible = new ArrayList<>();
        for (int x = -reach; x <= reach; x++) {
            for (int y = -reach; y <= reach; y++) {
                for (int z = -reach; z <= reach; z++) {

                    BlockPos currentPos = playerBlockPos.add(x, y, z);

                    FluidState fluidState = world.getFluidState(currentPos);

                    if (fluidState.isStill() && fluidState.getFluid() == Fluids.WATER) {
                        possible.add(currentPos);
                    }
                }
            }
        }
        possible.removeIf(blockPos -> {
            Rotation rotation = getLookAtWaterBlock(blockPos);
            return RaycastUtils.rayCast(new Vector2f(rotation.getYaw(),rotation.getPitch()),4.5).getType() != HitResult.Type.BLOCK;
        });
        if (possible.isEmpty()) return null;
        return possible.getFirst();
    }

    public static boolean notInStillLava() {
        if (mc.player == null || mc.world == null) return false;

        Box box = mc.player.getBoundingBox();

        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (!notStillLava(mc.world, pos)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int getYMotion() {
        if (mc.player == null) return -1;
        return (int) Math.ceil(mc.player.getVelocity().y * mc.player.getVelocity().y);
    }

    private static int getXMotion() {
        if (mc.player == null) return -1;
        return (int) Math.ceil(mc.player.getVelocity().x * mc.player.getVelocity().x);
    }

    private static int getZMotion() {
        if (mc.player == null) return -1;
        return (int) Math.ceil(mc.player.getVelocity().z * mc.player.getVelocity().z);
    }

    private static boolean hasEmptyBucket() {
        if (mc.player == null) return false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() == Items.BUCKET) {
                return true;
            }
        }
        return false;
    }

    public static void interactItem(Rotation rotation) {
        Client.INSTANCE.getRotationComponent().setRotations(rotation, 1, MovementFix.SILENT);
        interactRequired = true;
    }

    private static PlaceData findBestPlacePos() {
        if (mc.player == null || mc.world == null) return null;

        List<PlaceData> possibleBoxes = new ArrayList<>();

        var playerBox = mc.player.getBoundingBox();
        playerBox.offset(mc.player.getVelocity());

        for (int x = -getXMotion(); x <= getXMotion(); x++) {
            for (int z = -getZMotion(); z <= getZMotion(); z++) {
                for (int y = 0; y <= getYMotion(); y++) {
                    var pos = mc.player.getBlockPos().add(x, 0, z).down(y);
                    var state = mc.world.getBlockState(pos);

                    var bb = new Box(pos).withMaxY(pos.getY() + 3);
                    if (bb.intersects(playerBox) && !state.isAir()) {
                        possibleBoxes.add(new PlaceData(pos, bb));
                    }
                }
            }
        }

        possibleBoxes.sort(Comparator.comparingDouble(p -> p.pos.getY()));
        Collections.reverse(possibleBoxes);
        var best = possibleBoxes.getFirst();

        possibleBoxes.removeIf(p -> p.pos.getY() != best.pos().getY());

        possibleBoxes.sort(Comparator.comparingDouble(p -> Vec3d.of(p.pos()).squaredDistanceTo(
                playerBox.getCenter().withAxis(Direction.Axis.Y, playerBox.minY)
        )));
        possibleBoxes.sort(Comparator.comparing(p -> !p.bb.contains(mc.player.getPos())));
        return possibleBoxes.getFirst();
    }
    private static boolean nextTickWillLanding() {
        return !isAirBlocksBelow(getYMotion());
    }

    public static boolean isAirBlocksBelow(int high) {
        if (mc.player == null || mc.world == null) return false;

        List<PlaceData> possibleBoxes = new ArrayList<>();

        for (int x = -getXMotion(); x <= getXMotion(); x++) {
            for (int z = -getZMotion(); z <= getZMotion(); z++) {
                for (int y = 0; y <= high; y++) {
                    var pos = mc.player.getBlockPos().add(x, 0, z).down(y);
                    var state = mc.world.getBlockState(pos);

                    var bb = new Box(pos).withMaxY(pos.getY() + 3);
                    if (bb.intersects(mc.player.getBoundingBox()) && !state.isAir()) {
                        possibleBoxes.add(new PlaceData(pos, bb));
                    }
                }
            }
        }

        return possibleBoxes.isEmpty();
    }
    private static Rotation getLookAtWaterBlock(BlockPos targetPos) {
        if (mc.player == null || targetPos == null) {
            return new Rotation(0, 0);
        }

        return RotationUtils.toRotation(Vec3d.of(targetPos),false);
    }

    public static void placeWaterBucket(int waterBucketSlot) {
        if (waterBucketSlot == -1 || mc.player == null) {
            return;
        }
        oldSlot = mc.player.getInventory().selectedSlot;

        var best = findBestPlacePos();
        lastData = best;

        if (best == null) return;

        var r = getLookAtWaterBlock(best.pos());

        if (mc.player.getMainHandStack().getItem() != Items.WATER_BUCKET)
            mc.player.getInventory().selectedSlot = waterBucketSlot;
        interactItem(r);
        shouldReceive = true;
    }
}
