package cn.lazymoon.utils.entity;

import cn.lazymoon.features.module.impl.world.ContainerAura;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class RaycastUtils implements InstanceAccess {
    public static HitResult rayTraceBlocks(
            Vec3d start,
            Vec3d end,
            boolean stopOnLiquid,
            boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock,
            Entity entity
    ) {

        // Shape Type (替代 Block.COLLIDER / OUTLINE / VISUAL)
        RaycastContext.ShapeType shape;

        if (ignoreBlockWithoutBoundingBox) {
            // 只检测能碰撞的方块
            shape = RaycastContext.ShapeType.COLLIDER;
        } else {
            // returnLastUncollidableBlock → 视觉判定
            shape = returnLastUncollidableBlock
                    ? RaycastContext.ShapeType.VISUAL
                    : RaycastContext.ShapeType.OUTLINE;
        }

        // Fluid Handling（替代 Fluid.ANY / NONE）
        RaycastContext.FluidHandling fluids =
                stopOnLiquid ? RaycastContext.FluidHandling.ANY
                        : RaycastContext.FluidHandling.NONE;

        RaycastContext ctx = new RaycastContext(
                start,
                end,
                shape,
                fluids,
                entity
        );

        return Objects.requireNonNull(entity.getWorld()).raycast(ctx);
    }

    public static BlockHitResult rayCastContainer(Rotation rotation, Entity entity, World world, double reach, boolean throughWalls) {
        Vec3d start = entity.getCameraPosVec(1.0F);

        Vec3d direction = Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw());
        Vec3d end = start.add(direction.multiply(reach));

        if (!throughWalls) {
            BlockHitResult hit = world.raycast(new RaycastContext(
                    start, end,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    entity
            ));

            BlockState state = world.getBlockState(hit.getBlockPos());
            if (state.getBlock() instanceof ChestBlock && ContainerAura.container.isEnabled("Chest")
                    || state.getBlock() instanceof FurnaceBlock && ContainerAura.container.isEnabled("Furnace")
                    || state.getBlock() instanceof BlastFurnaceBlock && ContainerAura.container.isEnabled("BlastFurnace")
                    || state.getBlock() instanceof SmokerBlock && ContainerAura.container.isEnabled("SmokerFurnace")
                    || state.getBlock() instanceof BrewingStandBlock && ContainerAura.container.isEnabled("BrewingStand")) {
                return hit;
            } else {
                return null;
            }
        }

        double step = 0.1;
        double closestDistanceSq = Double.MAX_VALUE;
        BlockHitResult closestHit = null;

        Vec3d current = start;
        while (start.distanceTo(current) <= reach) {
            BlockPos pos = BlockPos.ofFloored(current);
            BlockState state = world.getBlockState(pos);

            if (state.getBlock() instanceof ChestBlock && ContainerAura.container.isEnabled("Chest")
                    || state.getBlock() instanceof FurnaceBlock && ContainerAura.container.isEnabled("Furnace")
                    || state.getBlock() instanceof BlastFurnaceBlock && ContainerAura.container.isEnabled("BlastFurnace")
                    || state.getBlock() instanceof SmokerBlock && ContainerAura.container.isEnabled("SmokerFurnace")
                    || state.getBlock() instanceof BrewingStandBlock && ContainerAura.container.isEnabled("BrewingStand")) {
                VoxelShape shape = state.getOutlineShape(world, pos);
                if (!shape.isEmpty()) {
                    BlockHitResult hitResult = shape.raycast(start, end, pos);
                    if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                        Vec3d hitVec = hitResult.getPos();
                        double distanceSq = start.squaredDistanceTo(hitVec);
                        if (distanceSq < closestDistanceSq) {
                            closestDistanceSq = distanceSq;
                            closestHit = new BlockHitResult(
                                    hitVec,
                                    hitResult.getSide(),
                                    pos,
                                    false
                            );
                        }
                    }
                }
            }

            current = current.add(direction.multiply(step));
        }

        return closestHit;
    }

    public static @Nullable BlockPos rayCastBlock(double reach) {
        return rayCastBlock(RotationUtils.getRotationOrElseMC(), reach);
    }

    public static boolean couldHit(Entity hitEntity, float currentYaw, float currentPitch,float range) {
        Vec3d positionEyes = mc.player.getEyePos();

        float f11 = hitEntity.getTargetingMargin();
        double ex = MathHelper.clamp(
                positionEyes.x, hitEntity.getBoundingBox().minX - (double)f11, hitEntity.getBoundingBox().maxX + (double)f11
        );
        double ey = MathHelper.clamp(
                positionEyes.y, hitEntity.getBoundingBox().minY - (double)f11, hitEntity.getBoundingBox().maxY + (double)f11
        );
        double ez = MathHelper.clamp(
                positionEyes.z, hitEntity.getBoundingBox().minZ - (double)f11, hitEntity.getBoundingBox().maxZ + (double)f11
        );

        double x = ex - mc.player.getX();
        double y = ey - (mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()));
        double z = ez - mc.player.getZ();
        float calcYaw = (float)(MathHelper.atan2(z, x) * 180.0 / Math.PI - 90.0);
        float calcPitch = (float)(-(MathHelper.atan2(y, (double)MathHelper.sqrt((float)(x * x + z * z))) * 180.0 / Math.PI));
        float yaw = updateRotation(currentYaw, calcYaw, 180.0F);
        float pitch = updateRotation(currentPitch, calcPitch, 180.0F);

        HitResult objectMouseOver = rayCastEntityHit(new Rotation(yaw,pitch),range,false);

        if (objectMouseOver == null || objectMouseOver.getType() != HitResult.Type.ENTITY) return false;
        EntityHitResult entityHitResult = (EntityHitResult) objectMouseOver;

        return entityHitResult.getEntity().getId() == hitEntity.getId();
    }

    public static float updateRotation(float current, float calc, float maxDelta) {
        float f = MathHelper.wrapDegrees(calc - current);
        if (f > maxDelta) {
            f = maxDelta;
        }

        if (f < -maxDelta) {
            f = -maxDelta;
        }

        return current + f;
    }


    public static @Nullable ChestHit findChestOnSightWithPoint(PlayerEntity player, double maxDistance) {
        var dir = player.getRotationVec(1).normalize();
        var start = player.getCameraPosVec(1);

        var x = MathHelper.floor(start.x);
        var y = MathHelper.floor(start.y);
        var z = MathHelper.floor(start.z);

        var stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        var stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        var stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        double tMaxX, tMaxY, tMaxZ;
        double tDeltaX, tDeltaY, tDeltaZ;

        if (stepX != 0) {
            var nextBoundaryX = stepX > 0 ? (x + 1.0) : x;
            tMaxX = (nextBoundaryX - start.x) / dir.x;
            tDeltaX = 1.0 / Math.abs(dir.x);
        } else {
            tMaxX = Double.POSITIVE_INFINITY;
            tDeltaX = Double.POSITIVE_INFINITY;
        }

        if (stepY != 0) {
            var nextBoundaryY = stepY > 0 ? (y + 1.0) : y;
            tMaxY = (nextBoundaryY - start.y) / dir.y;
            tDeltaY = 1.0 / Math.abs(dir.y);
        } else {
            tMaxY = Double.POSITIVE_INFINITY;
            tDeltaY = Double.POSITIVE_INFINITY;
        }

        if (stepZ != 0) {
            var nextBoundaryZ = stepZ > 0 ? (z + 1.0) : z;
            tMaxZ = (nextBoundaryZ - start.z) / dir.z;
            tDeltaZ = 1.0 / Math.abs(dir.z);
        } else {
            tMaxZ = Double.POSITIVE_INFINITY;
            tDeltaZ = Double.POSITIVE_INFINITY;
        }

        var startPos = new BlockPos(x, y, z);

        if (isChest(player.getWorld().getBlockState(startPos))) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                if (tMaxX <= maxDistance) {
                    var face = stepX > 0 ? Direction.EAST : Direction.WEST;
                    var hit = start.add(dir.multiply(tMaxX));

                    return new ChestHit(startPos, face, hit, toLocal(hit, startPos));
                }
            } else if (tMaxY < tMaxZ) {
                if (tMaxY <= maxDistance) {
                    var face = stepY > 0 ? Direction.UP : Direction.DOWN;
                    var hit = start.add(dir.multiply(tMaxY));

                    return new ChestHit(startPos, face, hit, toLocal(hit, startPos));
                }
            } else {
                if (tMaxZ <= maxDistance) {
                    var face = stepZ > 0 ? Direction.SOUTH : Direction.NORTH;
                    var hit = start.add(dir.multiply(tMaxZ));

                    return new ChestHit(startPos, face, hit, toLocal(hit, startPos));
                }
            }
        }

        var t = 0.0;

        while (t <= maxDistance) {
            var lastAxis = -1;
            var lastSign = -1;

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                t = tMaxX;
                tMaxX += tDeltaX;
                lastAxis = 0;
                lastSign = stepX;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                t = tMaxY;
                tMaxY += tDeltaY;
                lastAxis = 1;
                lastSign = stepY;
            } else {
                z += stepZ;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                lastAxis = 2;
                lastSign = stepZ;
            }

            if (t > maxDistance) {
                break;
            }

            var pos = new BlockPos(x, y, z);
            var state = player.getWorld().getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            if (isChest(state)) {
                var face = faceFromStep(lastAxis, lastSign);
                var hit = start.add(dir.multiply(t));

                return new ChestHit(pos, face, hit, toLocal(hit, pos));
            }
        }

        return null;
    }

    private static boolean isChest(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof TrappedChestBlock || state.getBlock() instanceof EnderChestBlock;
    }

    private static Direction faceFromStep(int axis, int sign) {
        return switch (axis) {
            case 0 -> sign > 0 ? Direction.WEST : Direction.EAST;
            case 1 -> sign > 0 ? Direction.DOWN : Direction.UP;
            case 2 -> sign > 0 ? Direction.NORTH : Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    private static Vec3d toLocal(Vec3d worldHit, BlockPos pos) {
        return new Vec3d(worldHit.x - pos.getX(), worldHit.y - pos.getY(), worldHit.z - pos.getZ());
    }

    public static Entity raycastEntity(
            double range,
            float yaw,
            float pitch,
            java.util.function.Predicate<Entity> entityFilter,
            ClientWorld world,
            Entity renderViewEntity
    ) {
        if (renderViewEntity == null || world == null)
            return null;

        double blockReachDistance = range;
        Vec3d eyePosition = renderViewEntity.getCameraPosVec(1.0F);
        Vec3d entityLook = getVectorForRotation(yaw, pitch);
        Vec3d vec = eyePosition.add(entityLook.multiply(blockReachDistance));

        List<Entity> entityList = world.getOtherEntities(
                renderViewEntity,
                renderViewEntity.getBoundingBox()
                        .stretch(entityLook.x * blockReachDistance, entityLook.y * blockReachDistance, entityLook.z * blockReachDistance)
                        .expand(1.0, 1.0, 1.0),
                entity -> entity != null
                        && (!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).isSpectator())
                        && entity.canHit()
        );

        Entity pointedEntity = null;

        for (Entity entity : entityList) {
            if (!entityFilter.test(entity)) continue;

            Box box = entity.getBoundingBox();
            Optional<Vec3d> hitOpt = box.raycast(eyePosition, vec);

            if (box.contains(eyePosition)) {
                if (blockReachDistance >= 0.0) {
                    pointedEntity = entity;
                    blockReachDistance = 0.0;
                }
            } else if (hitOpt.isPresent()) {
                double eyeDistance = eyePosition.distanceTo(hitOpt.get());

                if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                    if (entity == renderViewEntity.getVehicle() && !renderViewEntity.hasPassenger(entity)) {
                        if (blockReachDistance == 0.0) pointedEntity = entity;
                    } else {
                        pointedEntity = entity;
                        blockReachDistance = eyeDistance;
                    }
                }
            }
        }

        return pointedEntity;
    }
    public static Vec3d calculateViewVector(float pitch, float yaw) {
        // 将角度转换为弧度
        float pitchRad = pitch * ((float) Math.PI / 180F);
        float yawRad = -yaw * ((float) Math.PI / 180F);

        // 计算方向向量
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);

        return new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
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

    public static @Nullable EntityHitResult rayCastEntityHit(Rotation rotation, double reach, boolean throughWalls) {
        if (ClientUtils.isNull()) {
            return null;
        }

        var start = mc.player.getCameraPosVec(1);
        var look = getRotationVector(rotation.getPitch(), rotation.getYaw());
        var end = start.add(look.multiply(reach));
        var maxSq = reach * reach;

        if (!throughWalls) {
            var blockHit = rayCast(rotation, mc.player, mc.world, reach, false);

            if (blockHit != null) {
                var blockDistSq = start.squaredDistanceTo(blockHit.getPos());

                if (blockDistSq < maxSq) {
                    maxSq = blockDistSq;
                }
            }
        }

        return ProjectileUtil.raycast(mc.player, start, end, mc.player.getBoundingBox().stretch(look.multiply(reach)).expand(1.0, 1.0, 1.0), e -> e != mc.player && !e.isSpectator() && e.canHit(), maxSq);
    }

    public static @Nullable BlockPos rayCastBlock(Rotation rotation, double reach) {
        if (ClientUtils.isNull()) {
            return null;
        }

        var blockHit = rayCast(rotation, mc.player, mc.world, reach, false);

        if (blockHit != null) {
            return blockHit.getBlockPos();
        }

        return null;
    }

    public static BlockHitResult rayCast(Rotation rotation, Entity entity, World world, double maxDistance, boolean includeFluids) {
        var vec3d = entity.getCameraPosVec(1);
        var vec3d2 = getRotationVector(rotation.getPitch(), rotation.getYaw());
        var vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);

        return rayCast(vec3d, vec3d3, includeFluids, false, entity, world);
    }

    public static BlockHitResult rayCast(Vec3d start, Vec3d end, boolean includeFluids, boolean ignoreBlockWithoutBoundingBox, Entity entity, World world) {
        return world.raycast(new RaycastContext(start, end, ignoreBlockWithoutBoundingBox ? RaycastContext.ShapeType.COLLIDER : RaycastContext.ShapeType.OUTLINE, includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, entity));
    }
    public static BlockHitResult rayCastBlock(Rotation rotation, Entity entity, World world, double reach) {
        Vec3d start = entity.getCameraPosVec(1);
        Vec3d direction = getRotationVector(rotation.getPitch(), rotation.getYaw());
        Vec3d end = start.add(direction.multiply(reach));

        return world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity));

    }

    public static HitResult rayCast(
            Rotation rotation,
            double buildDist,
            double reach,
            float selfPartialTicks,
            boolean serversidePosition,
            boolean throughWalls
    ) {
        Vec3d eye = mc.player.getCameraPosVec(selfPartialTicks);
        Vec3d look = Vec3d.fromPolar(rotation.getPitch(), rotation.getYaw());
        Vec3d direction = eye.add(look.x * buildDist, look.y * buildDist, look.z * buildDist);

        BlockHitResult blockHit = mc.world.raycast(new RaycastContext(
                eye, direction,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        double blockDistance = buildDist;
        if (blockHit.getType() == HitResult.Type.BLOCK && !throughWalls) {
            blockDistance = blockHit.getPos().distanceTo(eye);
        }

        List<Entity> list = mc.world.getOtherEntities(
                mc.getCameraEntity(),
                mc.getCameraEntity().getBoundingBox()
                        .stretch(look.x * buildDist, look.y * buildDist, look.z * buildDist)
                        .expand(1.0, 1.0, 1.0),
                EntityPredicates.EXCEPT_SPECTATOR
        );

        Entity closestEntity = null;
        Vec3d closestHitVec = null;
        double closestDistance = blockDistance;

        for (Entity entity : list) {

            float borderSize = entity.getTargetingMargin();
            Box currentBBox = entity.getBoundingBox().expand(borderSize, borderSize, borderSize);
            Optional<Vec3d> currentHit = currentBBox.raycast(eye, direction);

            if (currentBBox.contains(eye)) {
                if (closestDistance >= 0.0D) {
                    closestEntity = entity;
                    closestHitVec = eye;
                    closestDistance = 0.0D;
                }
            } else if (currentHit.isPresent()) {
                double distance = eye.distanceTo(currentHit.get());
                if (distance < closestDistance || closestDistance == 0.0D) {
                    closestEntity = entity;
                    closestHitVec = currentHit.get();
                    closestDistance = distance;
                }
            }

            if (serversidePosition && (currentHit.isEmpty() || !currentBBox.contains(eye))) {
                Box predictedBBox = predictPlayerBBox(entity);
                Optional<Vec3d> predictedHit = predictedBBox.raycast(eye, direction);

                if (predictedBBox.contains(eye)) {
                    if (closestDistance >= 0.0D) {
                        closestEntity = entity;
                        closestHitVec = eye;
                        closestDistance = 0.0D;
                    }
                } else if (predictedHit.isPresent()) {
                    double distance = eye.distanceTo(predictedHit.get());
                    if (distance < closestDistance || closestDistance == 0.0D) {
                        closestEntity = entity;
                        closestHitVec = predictedHit.get();
                        closestDistance = distance;
                    }
                }
            }
        }

        double maxBlockReachDist = mc.player.getBlockInteractionRange();
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            if (eye.squaredDistanceTo(blockHit.getPos()) >= maxBlockReachDist * maxBlockReachDist) {
                blockHit = BlockHitResult.createMissed(blockHit.getPos(), blockHit.getSide(), blockHit.getBlockPos());
            }
        }

        if (closestEntity != null && eye.squaredDistanceTo(closestHitVec) > reach * reach) {
            return BlockHitResult.createMissed(closestHitVec, Direction.UP, BlockPos.ofFloored(closestHitVec));
        }

        if (closestEntity != null && (closestDistance < blockDistance || blockHit.getType() == HitResult.Type.MISS)) {
            if (closestEntity instanceof LivingEntity || closestEntity instanceof ItemFrameEntity) {
                mc.targetedEntity = closestEntity;
            }
            return new EntityHitResult(closestEntity, closestHitVec);
        }

        return blockHit;
    }

    public static Box predictPlayerBBox(Entity player) {
        double xOff = player.getLerpTargetX() - player.getX();
        double yOff = player.getLerpTargetY() - player.getY();
        double zOff = player.getLerpTargetZ() - player.getZ();
        float f1 = player.getTargetingMargin();

        return player.getBoundingBox().expand(f1, f1, f1).offset(xOff, yOff, zOff);
    }

    public static Vec3d getRotationVector(float pitch, float yaw) {
        var f = pitch * (float) (Math.PI / 180.0);
        var g = -yaw * (float) (Math.PI / 180.0);
        var h = MathHelper.cos(g);
        var i = MathHelper.sin(g);
        var j = MathHelper.cos(f);
        var k = MathHelper.sin(f);

        return new Vec3d(i * j, -k, h * j);
    }

    public static EntityHitResult raycast(Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, double maxDistance) {
        World world = entity.getWorld();
        double d = maxDistance;
        Entity entity2 = null;
        Vec3d vec3d = null;

        for(Entity entity3 : world.getOtherEntities(entity, box, predicate)) {
            Box box2 = entity3.getBoundingBox().expand((double)entity3.getTargetingMargin());
            Optional<Vec3d> optional = box2.raycast(min, max);
            if (box2.contains(min)) {
                if (d >= (double)0.0F) {
                    entity2 = entity3;
                    vec3d = (Vec3d)optional.orElse(min);
                    d = (double)0.0F;
                }
            } else if (optional.isPresent()) {
                Vec3d vec3d2 = (Vec3d)optional.get();
                double e = min.squaredDistanceTo(vec3d2);
                if (e < d || d == (double)0.0F) {
                    if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                        if (d == (double)0.0F) {
                            entity2 = entity3;
                            vec3d = vec3d2;
                        }
                    } else {
                        entity2 = entity3;
                        vec3d = vec3d2;
                        d = e;
                    }
                }
            }
        }

        if (entity2 == null) {
            return null;
        } else {
            return new EntityHitResult(entity2, vec3d);
        }
    }
    public static HitResult rayCast(float partialTicks, Rotation rotations) {
        HitResult objectMouseOver = null;
        Entity entity = mc.getCameraEntity();
        if (entity != null && mc.world != null) {
            double distance = 4.5F;
            objectMouseOver = pick(distance, partialTicks, true, rotations.getYaw(), rotations.getPitch());
        }

        return objectMouseOver;
    }

    public static HitResult rayCast(float partialTicks, Rotation rotations,double distance) {
        HitResult objectMouseOver = null;
        Entity entity = mc.getCameraEntity();
        if (entity != null && mc.world != null) {
            objectMouseOver = pick(distance, partialTicks, true, rotations.getYaw(), rotations.getPitch());
        }

        return objectMouseOver;
    }

    public static HitResult pick(double pHitDistance, float pPartialTicks, boolean pHitFluids, float pYRot, float pXRot) {
        Vec3d vec3 = new Vec3d(mc.player.getX(), mc.player.getY() + 1.62, mc.player.getZ());
        Vec3d vec31 = calculateViewVector(pXRot, pYRot);
        Vec3d vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
        return mc.world.raycast(new RaycastContext(vec3, vec32, RaycastContext.ShapeType.OUTLINE, pHitFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, mc.player));
    }
    public static BlockHitResult rayCast(Vector2f toRotation, double reach) {
        Vec3d eyesPos = mc.player.getCameraPosVec(1f);
        Vec3d rotationVec = getVectorForRotation(toRotation);
        return mc.world.raycast(new RaycastContext(eyesPos,eyesPos.add(new Vec3d(rotationVec.x * reach, rotationVec.y * reach, rotationVec.z * reach)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE,mc.player));
    }

    public static Vec3d getVectorForRotation(final Vector2f rotation) {
        float yawCos = (float) Math.cos(-rotation.x * 0.017453292F - (float) Math.PI);
        float yawSin = (float) Math.sin(-rotation.x * 0.017453292F - (float) Math.PI);
        float pitchCos = (float) -Math.cos(-rotation.y * 0.017453292F);
        float pitchSin = (float) Math.sin(-rotation.y * 0.017453292F);
        return new Vec3d(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    public record ChestHit(BlockPos pos, Direction face, Vec3d hit, Vec3d hitLocal) {
        // Empty.
    }
}
