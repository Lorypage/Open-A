package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.Render3DEvent;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.render.PostProcessing;
import cn.lazymoon.features.module.impl.world.BedBreaker;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.sxmurxy.builders.Builder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.BlurTaskInstance;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import cn.lazymoon.utils.color.ColorPanel;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "BedPlates",description = "Show the position of the bed",key = 0,category = Category.Visual,hidden = false)
public class BedESP extends Module {
    private final NumberValue checkLayers = new NumberValue("Check Layers", 1, 5, 1,1);

    public Map<BlockEntity, Pair<Rectangle, Boolean>> hashMap = new HashMap<>();
    public static final Identifier CIRCLE_TEXTURE =
            Identifier.of("arcane", "particles/circle.png");

    @EventTarget
    public void Render3DEvent(Render3DEvent event) {
        hashMap.clear();
        if (mc.player == null || mc.world == null) return;
        ChunkPos currentPosition = mc.player.getChunkPos();
        int viewDistance = mc.options.getClampedViewDistance();
        ChunkPos start = new ChunkPos(currentPosition.x - viewDistance, currentPosition.z - viewDistance);
        ChunkPos end = new ChunkPos(currentPosition.x + viewDistance, currentPosition.z + viewDistance);
        for (int x = start.x; x <= end.x; x++) {
            for (int z = start.z; z <= end.z; z++) {
                if (!mc.world.isChunkLoaded(x, z)) continue;
                for (BlockPos pos : mc.world.getChunk(x, z).getBlockEntityPositions()) {
                    BlockState blockState = mc.world.getBlockState(pos);
                    if (!(blockState.getBlock() instanceof BedBlock)) continue;
                    if (blockState.get(BedBlock.PART) == BedPart.HEAD) continue;
                    BlockEntity blockEntity = mc.world.getBlockEntity(pos);
                    if (blockEntity == null) continue;
                    int tickDelta = (int) event.getTickCounter().getTickDelta(false);
                    Vec3d prevPos = new Vec3d(blockEntity.getPos().getX() + 0.5, blockEntity.getPos().getY(), blockEntity.getPos().getZ() + 0.5);
                    Vec3d interpolated = prevPos.add(blockEntity.getPos().toCenterPos().add(prevPos).multiply(tickDelta));
                    Box boundingBox = new Box(
                            interpolated.x,
                            interpolated.y,
                            interpolated.z,
                            interpolated.x,
                            interpolated.y + 1,
                            interpolated.z
                    ).expand(0.1, 0.1, 0.1);
                    Vec3d[] corners = new Vec3d[]{
                            new Vec3d(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                            new Vec3d(boundingBox.maxX, boundingBox.minY, boundingBox.minZ),
                            new Vec3d(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ),
                            new Vec3d(boundingBox.minX, boundingBox.minY, boundingBox.maxZ),
                            new Vec3d(boundingBox.minX, boundingBox.maxY + 0.1, boundingBox.minZ),
                            new Vec3d(boundingBox.maxX, boundingBox.maxY + 0.1, boundingBox.minZ),
                            new Vec3d(boundingBox.maxX, boundingBox.maxY + 0.1, boundingBox.maxZ),
                            new Vec3d(boundingBox.minX, boundingBox.maxY + 0.1, boundingBox.maxZ)
                    };
                    Rectangle rectangle = null;
                    boolean visible = false;
                    for (Vec3d corner : corners) {
                        Pair<Vec3d, Boolean> projection = RenderUtils.project(event.getMatrix().peek().getPositionMatrix(), event.getProjectionMatrix(), corner);
                        if (projection.getRight()) {
                            visible = true;
                        }
                        Vec3d projected = projection.getLeft();
                        if (rectangle == null) {
                            rectangle = new Rectangle((int) projected.getX(), (int) projected.getY(), (int) projected.getX(), (int) projected.getY());
                        } else {
                            if (rectangle.x > projected.getX()) {
                                rectangle.x = (int) projected.getX();
                            }
                            if (rectangle.y > projected.getY()) {
                                rectangle.y = (int) projected.getY();
                            }
                            if (rectangle.z < projected.getX()) {
                                rectangle.z = (int) projected.getX();
                            }
                            if (rectangle.w < projected.getY()) {
                                rectangle.w = (int) projected.getY();
                            }
                        }
                    }
                    hashMap.put(blockEntity, new Pair<>(rectangle, visible));
                }
            }
        }
    }

    @EventTarget
    public void RenderNvgEvent(RenderNvgEvent event) {
        DrawContext context = event.drawContext();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        if (!hashMap.isEmpty() && hashMap.entrySet().stream().anyMatch(entityPairEntry -> entityPairEntry.getValue().getRight())) {
            // 动态生成 offsets
            Set<int[]> offsets = new LinkedHashSet<>();
            int layers = checkLayers.getValue().intValue();
            for (int layer = 1; layer <= layers; layer++) {
                offsets.add(new int[]{layer, 0, 0});
                offsets.add(new int[]{-layer, 0, 0});
                offsets.add(new int[]{0, 0, layer});
                offsets.add(new int[]{0, 0, -layer});
                if (layer == 1) {
                    offsets.add(new int[]{0, 1, 0});
                }
            }

            for (Map.Entry<BlockEntity, Pair<Rectangle, Boolean>> entry : hashMap.entrySet()) {
                Pair<Rectangle, Boolean> pair = entry.getValue();
                if (!pair.getRight()) continue;

                BlockPos bedPos = entry.getKey().getPos();
                World world = mc.world;

                if (mc.world == null) continue;

                BlockState bedState = world.getBlockState(bedPos);
                if (!(bedState.getBlock() instanceof BedBlock)) continue;

                Direction facing = bedState.get(BedBlock.FACING);
                boolean isHead = bedState.get(BedBlock.PART) == BedPart.HEAD;

                BlockPos otherBedPos = isHead
                        ? bedPos.offset(facing.getOpposite())
                        : bedPos.offset(facing);

                BlockPos[] bedParts = new BlockPos[]{bedPos, otherBedPos};

                boolean hasAir = false;
                Direction[] checkDirs = {
                        Direction.UP,
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.EAST,
                        Direction.WEST
                };

                for (BlockPos part : bedParts) {
                    for (Direction dir : checkDirs) {
                        if (world.getBlockState(part.offset(dir)).isAir()) {
                            hasAir = true;
                            break;
                        }
                    }
                    if (hasAir) break;
                }

                Set<ItemStack> blocks = new LinkedHashSet<>();

                for (BlockPos part : bedParts) {
                    for (int[] offsetArr : offsets) {
                        BlockPos newPos = part.add(offsetArr[0], offsetArr[1], offsetArr[2]);
                        BlockState state = world.getBlockState(newPos);
                        Block block = state.getBlock();

                        if (block instanceof BedBlock) continue;
                        if (state.isAir()) continue;

                        ItemStack stack = block.asItem().getDefaultStack();
                        boolean exists = blocks.stream()
                                .noneMatch(s -> s.getItem().equals(stack.getItem()));

                        if (exists) {
                            blocks.add(stack);
                        }
                    }
                }

                if (blocks.isEmpty()) continue;

                float y = (float) (pair.getLeft().y - 2 - 1 - 34.5f);
                int xOffset = Math.max(blocks.size() * 18, 100);
                float centerX = (float) Math.max(
                        ((pair.getLeft().x + pair.getLeft().z) / 2 - (double) xOffset / 2),
                        -100
                );

                if (PostProcessing.bloom.get()) {
                    RenderHelper.drawRoundRectBloomApple(centerX - 2,
                            y - 2,
                            xOffset + 2,
                            19, 10, 2, new Color(255, 255, 255, 10));
                }
                RenderHelper.drawGradientAppleRoundedRectLR(centerX - 2,
                        y - 2,
                        xOffset + 2,
                        19, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),10);

                int itemOffset = 0;


                for (ItemStack item : blocks) {

                    float outSpace = 2f;
                    if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
                        BuiltBlur blur = Builder.blur()
                                .matrix4f(event.matrix4f())
                                .size(new SizeState(
                                        xOffset + 2 + outSpace * 2f,
                                        19 + 8f + outSpace * 2f
                                ))
                                .radius(new QuadRadiusState(10))
                                .blurRadius(20)
                                .smoothness(5f)
                                .color(QuadColorState.TRANSPARENT)
                                .position(new PositionState(
                                        centerX - 2 - outSpace,
                                        y - 2 - 8f - outSpace
                                ))
                                .build();
                        BlurTaskInstance.addTask(blur);
                    }

                    context.drawItem(item, (int) (centerX + itemOffset), (int) y);
                    itemOffset += 18;
                }
            }

        }
        RenderSystem.disableBlend();
    }

    public static class Rectangle {
        public double x;
        public double y;
        public double z;
        public double w;

        public Rectangle(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }
}
