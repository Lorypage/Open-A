package cn.lazymoon.features.module.impl.world;

import cn.lazymoon.Client;
import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.component.rotation.utils.MovementFix;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.event.impl.render.Render3DEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.player.AutoTool;
import cn.lazymoon.features.module.impl.render.InterFace;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.block.BreakUtils;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.math.VecRotation;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ModuleInfo(name = "BedNuker", description = "Help you destroy beds within range", key = 0, category = Category.World, hidden = false)
public class BedBreaker extends Module {
    public static ModeValue mode = new ModeValue("Mode", new String[]{"Swap", "Legit"}, "Swap");
    public static NumberValue range = new NumberValue("Range", 4, 0, 7, 0.01);
    public static BoolValue noDelay = new BoolValue("No Delay", true);
    public static BoolValue noHit = new BoolValue("No KillAura", true);
    public static BoolValue renderBox = new BoolValue("Render", true);
    public static BoolValue whitelistOwnBed = new BoolValue("Whitelist Own Bed", false);

    public static Integer targetX;
    public static Integer targetZ;
    public static BlockPos pos, oldPos;
    private int blockHitDelay = 0;
    private static final TimerUtils searchTimer = new TimerUtils();
    public static boolean hitBlock = false;
    public static float currentDamage = 0F;
    public static BlockPos breakingBlockPos = null;
    public static BreakState breakState = BreakState.NONE;

    private Vec3d homePos = null;
    private Vec3d lastPos = null;

    public enum BreakState {
        NONE,
        PREPARE,
        BREAKING,
        FINISHING
    }

    @Override
    public void onEnable() {
        breakState = BreakState.NONE;
        if (mc.interactionManager == null || mc.getNetworkHandler() == null) return;
        if (pos != null && !mc.interactionManager.getCurrentGameMode().isCreative()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
        }
        hitBlock = false;
        breakingBlockPos = null;
        currentDamage = 0F;
        pos = null;
        // 重置位置记录，避免误判
        lastPos = null;
        homePos = null;
    }

    @Override
    public void onDisable() {
        breakState = BreakState.NONE;
        currentDamage = 0F;
        pos = null;
        breakingBlockPos = null;
        hitBlock = false;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.world == null) return;

        // ======================= 传送检测（白名单床功能） =======================
        if (event.isPre() && whitelistOwnBed.get()) {
            Vec3d currentPos = mc.player.getPos();
            if (lastPos != null) {
                double distanceSq = currentPos.squaredDistanceTo(lastPos);
                // 位移超过 40 格视为传送
                if (distanceSq > 40 * 40) {
                    homePos = currentPos;
                }
            }
            lastPos = currentPos;
        }
        // ====================================================================

        if (noHit.get() && (KillAura.target != null || !KillAura.targets.isEmpty())) {
            pos = null;
            breakingBlockPos = null;
            hitBlock = false;
            breakState = BreakState.NONE;
            currentDamage = 0F;
            oldPos = null;
        }

        if (targetX != null) {
            if (mc.player.getZ() > targetZ - 10 && mc.player.getZ() < targetZ + 10 && mc.player.getX() > targetX - 10 && mc.player.getX() < targetX + 10) {
                return;
            }
        }

        if (pos == null || !(mc.world.getBlockState(pos).getBlock() instanceof BedBlock) || mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.get() * range.get()) {
            if (breakingBlockPos != null) {
                pos = breakingBlockPos;
            } else pos = findBed();

            if (pos == null || mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.get() * range.get() || mc.world.getBlockState(pos).isAir()) {
                hitBlock = false;
                breakingBlockPos = null;
                breakState = BreakState.NONE;
                pos = null;
            }
        }

        if (pos == null) {
            currentDamage = 0F;
            breakState = BreakState.NONE;
            return;
        }

        BlockPos currentPos = pos;
        VecRotation spot = getBestAimVecForBed(currentPos);
        if (mode.is("Legit") || mode.is("Swap")) {
            BlockPos blockPos;
            if (mode.is("Swap")) {
                ClientPlayerEntity player = mc.player;
                World world = mc.world;

                if (breakingBlockPos != null) {
                    blockPos = breakingBlockPos;
                } else {
                    BlockState bedState = world.getBlockState(currentPos);
                    if (!(bedState.getBlock() instanceof BedBlock)) return;

                    Direction facing = bedState.get(BedBlock.FACING);
                    boolean isHead = bedState.get(BedBlock.PART) == BedPart.HEAD;
                    BlockPos otherBedPos = isHead ? currentPos.offset(facing.getOpposite()) : currentPos.offset(facing);

                    List<BlockPos> bedParts = new ArrayList<>();
                    bedParts.add(currentPos);
                    bedParts.add(otherBedPos);

                    Direction[] directions = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

                    List<BlockPos> solidBlocks = new ArrayList<>();
                    boolean hasAir = false;

                    for (BlockPos bedPart : bedParts) {
                        for (Direction dir : directions) {
                            BlockPos offsetPos = bedPart.offset(dir);
                            BlockState offsetState = world.getBlockState(offsetPos);

                            if (offsetState.isAir()) {
                                hasAir = true;
                                break;
                            }

                            if (!(offsetState.getBlock() instanceof BedBlock)) {
                                solidBlocks.add(offsetPos);
                            }
                        }
                        if (hasAir) break;
                    }

                    if (hasAir) {
                        blockPos = currentPos;
                    } else {
                        double bestTime = Double.MAX_VALUE;
                        double bestDistance = Double.MAX_VALUE;
                        BlockPos bestPos = null;

                        for (BlockPos solidPos : solidBlocks) {
                            float relativeHardness = BreakUtils.calcBlockBreakingDelta(solidPos, world, solidPos);
                            if (relativeHardness <= 0) relativeHardness = 0.0001f;
                            double time = 1.0 / relativeHardness;
                            double distance = player.squaredDistanceTo(Vec3d.ofCenter(solidPos));

                            if (time < bestTime || (Math.abs(time - bestTime) < 1e-5 && distance < bestDistance)) {
                                bestTime = time;
                                bestDistance = distance;
                                bestPos = solidPos;
                            }
                        }

                        blockPos = bestPos != null ? bestPos : currentPos.up();
                    }

                    breakingBlockPos = blockPos;
                }
            } else {
                var hitResult = mc.world.raycast(new net.minecraft.world.RaycastContext(
                        mc.player.getEyePos(),
                        Vec3d.ofCenter(currentPos),
                        net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        mc.player
                ));
                blockPos = hitResult != null && hitResult.getType() == HitResult.Type.BLOCK
                        ? hitResult.getBlockPos()
                        : null;
            }

            if (blockPos != null) {
                Block blockAtHit = mc.world.getBlockState(blockPos).getBlock();
                if (!(blockAtHit instanceof BedBlock)) {
                    pos = blockPos;
                    currentPos = pos;
                    spot = getBestAimVecForBed(currentPos);
                }
            }
        }

        if (oldPos != null && !oldPos.equals(currentPos)) {
            mc.world.setBlockBreakingInfo(mc.player.getId(), oldPos, -1);
            currentDamage = 0F;
        }
        oldPos = currentPos;
        if (blockHitDelay > 0 && !noDelay.get()) {
            blockHitDelay--;
            return;
        }
        if (spot != null && !hitBlock) {
            breakState = BreakState.PREPARE;
        }
        boolean validTarget =
                pos != null &&
                        mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= range.get() * range.get() &&
                        !mc.world.getBlockState(pos).isAir();

        if (spot != null && validTarget) {
            Client.INSTANCE.getRotationComponent().setRotations(spot.rotation, 1, MovementFix.SILENT);
        }
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isSpectator()) return;

        Rotation serverRotation = RotationComponent.serverRotation;
        BlockPos currentPos = pos;

        if (mode.is("Swap") && currentPos != null && mc.world.getBlockState(currentPos).getBlock() instanceof BedBlock) {
            BlockState bedState = mc.world.getBlockState(currentPos);
            Direction facing = bedState.get(BedBlock.FACING);
            boolean isHead = bedState.get(BedBlock.PART) == BedPart.HEAD;
            BlockPos otherBedPos = isHead ? currentPos.offset(facing.getOpposite()) : currentPos.offset(facing);

            boolean hasAir = false;

            for (BlockPos bedPart : new BlockPos[]{currentPos, otherBedPos}) {
                for (Direction dir : Direction.values()) {
                    BlockPos offset = bedPart.offset(dir);
                    BlockState offsetState = mc.world.getBlockState(offset);
                    if (offsetState.isAir()) {
                        hasAir = true;
                        break;
                    }
                }
                if (hasAir) break;
            }

            if (!hasAir && breakingBlockPos != null && hitBlock) {
                breakingBlockPos = null;
                currentDamage = 0F;
                hitBlock = false;
                breakState = BreakState.NONE;
                mc.world.setBlockBreakingInfo(mc.player.getId(), currentPos, -1);
                return;
            }
        }

        BlockHitResult raytrace = performRaytrace(currentPos, serverRotation, range.get());

        if (raytrace == null && (!hitBlock || mode.is("Normal"))) {
            breakState = BreakState.NONE;
            return;
        }

        AutoTool autoTool = Client.INSTANCE.getModuleManager().getModule(AutoTool.class);
        if (Client.INSTANCE.getModuleManager().getModule(AutoTool.class).isState()) {
            autoTool.switchSlot(currentPos);
        }

        Block block = mc.world.getBlockState(currentPos).getBlock();
        if (block == null) {
            return;
        }

        if (currentDamage == 0F) {
            breakState = BreakState.PREPARE;
            if (!Client.INSTANCE.getModuleManager().getModule(KillAura.class).isState() || KillAura.target == null || !KillAura.noWorking) {
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, Objects.requireNonNull(currentPos), Objects.requireNonNull(raytrace).getSide()));
                hitBlock = true;
                breakState = BreakState.BREAKING;
                if (mc.player.isCreative() || mc.world.getBlockState(currentPos).getHardness(mc.world, currentPos) == 0.0f) {
                    mc.world.breakBlock(currentPos, true);
                    currentDamage = 0F;
                    pos = null;
                    return;
                }
            }
        }

        BlockState state = mc.world.getBlockState(currentPos);
        float relativeHardness = state.calcBlockBreakingDelta(mc.player, mc.world, currentPos);
        if (relativeHardness <= 0) return;

        currentDamage += relativeHardness;

        if (hitBlock) {
            breakState = BreakState.BREAKING;
        }

        int breakStage = (int) (currentDamage * 10F);
        if (breakStage > 9) breakStage = 9;

        mc.particleManager.addBlockBreakingParticles(currentPos, Direction.byId(breakStage));
        mc.world.setBlockBreakingInfo(mc.player.getId(), currentPos, breakStage);

        if (currentDamage >= 1F) {
            breakState = BreakState.FINISHING;
            if (!Client.INSTANCE.getModuleManager().getModule(KillAura.class).isState() || KillAura.target == null || !KillAura.noWorking) {
                hitBlock = false;
                if (raytrace == null) {
                    breakState = BreakState.NONE;
                    return;
                }

                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.networkHandler.sendPacket(
                        new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, Objects.requireNonNull(currentPos), raytrace.getSide())
                );

                mc.world.breakBlock(currentPos, true);

                blockHitDelay = 4;
                currentDamage = 0F;
                mc.world.setBlockBreakingInfo(mc.player.getId(), currentPos, -1);
                pos = null;

                breakingBlockPos = null;

                breakState = BreakState.NONE;
            }
        }
    }

    public static BlockHitResult performRaytrace(BlockPos blockPos, Rotation rotation, double reach) {
        if (ClientUtils.isNull()) return null;

        Vec3d eyes = Objects.requireNonNull(mc.player).getEyePos();
        Vec3d endPos = eyes.add(getVectorForRotation(rotation.getYaw(), rotation.getPitch()).multiply(reach));

        BlockState state = Objects.requireNonNull(mc.world).getBlockState(blockPos);
        VoxelShape shape = state.getCollisionShape(mc.world, blockPos);

        return shape.raycast(eyes, endPos, blockPos);
    }

    public static Vec3d getVectorForRotation(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float f = MathHelper.cos(-yawRad - (float) Math.PI);
        float f1 = MathHelper.sin(-yawRad - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitchRad);
        float f3 = MathHelper.sin(-pitchRad);

        return new Vec3d(f1 * f2, f3, f * f2);
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null) return;
        if (mc.player.isSpectator()) return;
        if (renderBox.get() && pos != null) {
            Box originalBox = new Box(pos);

            float scaleX = currentDamage;
            float scaleY = currentDamage;
            float scaleZ = currentDamage;

            double newWidth = (originalBox.maxX - originalBox.minX) * scaleX;
            double newHeight = (originalBox.maxY - originalBox.minY) * scaleY;
            double newDepth = (originalBox.maxZ - originalBox.minZ) * scaleZ;

            double centerX = (originalBox.minX + originalBox.maxX) / 2.0;
            double centerY = (originalBox.minY + originalBox.maxY) / 2.0;
            double centerZ = (originalBox.minZ + originalBox.maxZ) / 2.0;

            Box scaledBox = new Box(
                    centerX - newWidth / 2.0, centerY - newHeight / 2.0, centerZ - newDepth / 2.0,
                    centerX + newWidth / 2.0, centerY + newHeight / 2.0, centerZ + newDepth / 2.0
            );

            RenderUtils.drawBox(event.getMatrix(), scaledBox, ColorUtils.reAlpha(InterFace.color(12), 140), false, null, true);
        }
    }

    public static void updateClosestBlockPos() {
        if (mc.player == null || mc.world == null) return;
        if (!searchTimer.hasTimeElapsed(500L)) {
            return;
        }
        searchTimer.reset();

        double posX = mc.player.getX();
        double posY = mc.player.getY();
        double posZ = mc.player.getZ();
        List<BlockPos> targetBlockList = new ArrayList<>();
        int searchDistance = 10;
        for (int SearchX = (int) (posX - searchDistance); SearchX < (int) (posX + searchDistance); SearchX++) {
            for (int SearchY = (int) (posY - searchDistance); SearchY < (int) (posY + searchDistance); SearchY++) {
                for (int SearchZ = (int) (posZ - searchDistance); SearchZ < (int) (posZ + searchDistance); SearchZ++) {
                    BlockPos blp = new BlockPos(SearchX, SearchY, SearchZ);
                    if (mc.world.getBlockState(blp).getBlock() != net.minecraft.block.Blocks.AIR) {
                        Block block = mc.world.getBlockState(blp).getBlock();
                        if (block instanceof BedBlock) {
                            targetBlockList.add(blp);
                        }
                    }
                }
            }
        }
        if (targetBlockList.isEmpty()) {
            targetX = null;
            targetZ = null;
        } else {
            BlockPos closestBlp = getClosestBlock(mc.player.getX(), mc.player.getY(), mc.player.getZ(), targetBlockList);
            if (closestBlp != null) {
                targetX = closestBlp.getX();
                targetZ = closestBlp.getZ();
            }
        }
    }

    private static BlockPos getClosestBlock(double posX, double posY, double posZ, List<BlockPos> blpList) {
        blpList.sort((blockPosA, blockPosB) -> {
            double distanceA = blockPosA.getSquaredDistance(posX, posY, posZ);
            double distanceB = blockPosB.getSquaredDistance(posX, posY, posZ);
            return Double.compare(distanceA, distanceB);
        });
        return blpList.isEmpty() ? null : blpList.getFirst();
    }

    private BlockPos findBed() {
        ClientPlayerEntity player = mc.player;
        World world = mc.world;
        if (player == null || world == null) return null;

        // ======================= 白名单床功能：在自家范围内不破坏 =======================
        if (whitelistOwnBed.get() && homePos != null) {
            double distToHomeSq = player.squaredDistanceTo(homePos);
            if (distToHomeSq < 35 * 35) {
                return null; // 距离家小于 35 格，不破坏床
            }
        }
        // ========================================================================

        int radius = (int) (range.get() + 3);
        double nearestDistance = Double.MAX_VALUE;
        BlockPos nearest = null;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = player.getBlockPos().add(x, y, z);
                    Block block = world.getBlockState(checkPos).getBlock();
                    if (!(block instanceof BedBlock)) continue;

                    double distSq = player.squaredDistanceTo(Vec3d.ofCenter(checkPos));

                    if (distSq > range.get() * range.get()) continue;

                    if (distSq < nearestDistance && (isHittable(checkPos) || mode.is("Legit") || mode.is("Swap"))) {
                        nearestDistance = distSq;
                        nearest = checkPos;
                    }
                }
            }
        }
        return nearest;
    }

    private boolean isHittable(BlockPos blockPos) {
        if (mc.world == null) return false;

        for (Direction direction : Direction.values()) {
            BlockPos offsetPos = blockPos.offset(direction);
            BlockState offsetState = mc.world.getBlockState(offsetPos);

            if (!offsetState.isFullCube(mc.world, offsetPos)) {
                return true;
            }
        }
        return false;
    }

    private VecRotation getBestAimVecForBed(BlockPos bedPos) {
        if (mc.player == null) return null;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d bestHitVec = null;
        Rotation bestRot = null;
        double bestDist = Double.MAX_VALUE;

        for (double x = 0; x <= 1.0; x += 0.5) {
            for (double y = 0; y <= 1.0; y += 0.5) {
                for (double z = 0; z <= 1.0; z += 0.5) {
                    Vec3d hitVec = new Vec3d(bedPos.getX() + x, bedPos.getY() + y, bedPos.getZ() + z);
                    Rotation rot = RotationUtils.toRotation(hitVec, false);
                    BlockHitResult hit = performRaytrace(bedPos, rot, range.get());
                    if (hit == null || !hit.getBlockPos().equals(bedPos)) continue;

                    double dist = eyePos.squaredDistanceTo(hitVec);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestHitVec = hitVec;
                        bestRot = rot;
                    }
                }
            }
        }
        if (bestHitVec == null) {
            Vec3d center = Vec3d.ofCenter(bedPos);
            bestRot = RotationUtils.toRotation(center, false);
            bestHitVec = center;
        }

        return new VecRotation(bestHitVec, bestRot);
    }
}