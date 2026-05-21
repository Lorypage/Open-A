package cn.lazymoon.features.module.impl.visual;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.utils.color.ColorUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Arrays;

/**
 * @Author:Guyuemang
 * @Time:02-28
 */
@ModuleInfo(name = "ESP", description = "Display all entities in the world", key = 0, category = Category.Visual, hidden = false)
public class ESP extends Module {
    public static ModeValue mode = new ModeValue("Mode", new String[]{"Glow", "ESP2D"}, "Glow");

    // 原来的 Glow
    public static BoolValue glow = new BoolValue("Glow", true);
    public static MultiBoolValue glowaddons = (MultiBoolValue) new MultiBoolValue("Glow Addons", () -> glow.get(), Arrays.asList(
            new BoolValue("Player", true),
            new BoolValue("Item", true),
            new BoolValue("Mob", true),
            new BoolValue("Animals", true),
            new BoolValue("Arrows", true)
    ));

    // 新增 ESP2D 选项
    public static MultiBoolValue esp2daddons = new MultiBoolValue("ESP2D Addons", () -> mode.is("ESP2D"), Arrays.asList(
            new BoolValue("Box", true),
            new BoolValue("Health Bar", true),
            new BoolValue("Armor Bar", true),
            new BoolValue("Held Item", true),
            new BoolValue("NameTag", true),
            new BoolValue("Self", true)
    ));

    public static boolean shouldGlow(Entity entity) {
        ESP module = Client.INSTANCE.getModuleManager().getModule(ESP.class);
        if (!module.isState()) return false;

        if (mode.is("Glow")) {
            if (entity instanceof PlayerEntity && glowaddons.isEnabled("Player")) return true;
            if (entity instanceof ItemEntity && glowaddons.isEnabled("Item")) return true;
            if (entity instanceof MobEntity && glowaddons.isEnabled("Mob")) return true;
            if (entity instanceof AnimalEntity && glowaddons.isEnabled("Animals")) return true;
            return entity instanceof ArrowEntity && glowaddons.isEnabled("Arrows");
        }

        return false;
    }

    @EventTarget
    public void onRenderNvg(RenderNvgEvent event) {
        if (!isState() || mc.world == null || mc.player == null) return;
        if (!mode.is("ESP2D")) return;

        // 这里默认 RenderNvgEvent 里能拿到 drawContext / matrix
        // 如果你项目里的方法名不同，改成你自己的即可
        DrawContext drawContext = event.drawContext();
        Matrix4f modelView = event.matrix4f();
        Matrix4f projection = event.matrix4f();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.isRemoved() || !player.isAlive()) continue;
            if (player == mc.player && !esp2daddons.isEnabled("Self")) continue;

            render2DESP(drawContext, player, modelView, projection);
        }
    }

    private void render2DESP(DrawContext drawContext, PlayerEntity player, Matrix4f modelView, Matrix4f projection) {
        float[] box = getScreenBounds(player, modelView, projection);
        if (box == null) return;

        float x1 = box[0];
        float y1 = box[1];
        float x2 = box[2];
        float y2 = box[3];

        float width = x2 - x1;
        float height = y2 - y1;
        if (width <= 1 || height <= 1) return;

        boolean self = player == mc.player;
        Color main = self ? new Color(0, 200, 255, 255) : new Color(255, 255, 255, 255);
        Color bg = new Color(0, 0, 0, 80);

        // 方框
        if (esp2daddons.isEnabled("Box")) {
            RenderHelper.drawRoundedRect(x1 - 1.5f, y1 - 1.5f, width + 3f, height + 3f, bg, 2f);
            RenderHelper.drawOutlineRect(x1 - 1.5f, y1 - 1.5f, width + 3f, height + 3f, main, 1.25f);
        }

        // NameTag
        float textY = y1 - 12f;
        String name = player.getName().getString();
        String tag = name;

        if (esp2daddons.isEnabled("NameTag")) {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            tag = name + String.format(" §7[%.1f/%.1f]", health, maxHealth);

            float textW = FontManager.medium.getStringWidth(tag, 14);
            FontManager.medium.drawString(
                    14,
                    tag,
                    x1 + width / 2f - textW / 2f,
                    textY,
                    Color.WHITE,
                    true
            );
        }

        // 血条
        if (esp2daddons.isEnabled("Health Bar") && player instanceof LivingEntity living) {
            float health = Math.max(0f, living.getHealth());
            float maxHealth = Math.max(1f, living.getMaxHealth());
            float hpProgress = Math.min(1f, health / maxHealth);

            float barX = x1 - 5f;
            float barY = y1;
            float barH = height;
            float filledH = barH * hpProgress;

            RenderHelper.drawRoundedRect(barX, barY, 3f, barH, bg, 1.2f);
            RenderHelper.drawRoundedRect(barX, barY + (barH - filledH), 3f, filledH,
                    getHealthColor(hpProgress), 1.2f);
        }

        // 护甲条
        if (esp2daddons.isEnabled("Armor Bar")) {
            int armor = player.getArmor();
            float armorProgress = Math.min(1f, armor / 20f);

            float barX = x2 + 2f;
            float barY = y1;
            float barH = height;
            float filledH = barH * armorProgress;

            RenderHelper.drawRoundedRect(barX, barY, 3f, barH, bg, 1.2f);
            RenderHelper.drawRoundedRect(barX, barY + (barH - filledH), 3f, filledH,
                    new Color(80, 160, 255, 255), 1.2f);
        }

        // 手持物品
        if (esp2daddons.isEnabled("Held Item")) {
            ItemStack stack = player.getMainHandStack();
            if (!stack.isEmpty()) {
                float itemX = x1 + width / 2f - 8f;
                float itemY = y2 + 3f;

                // 物品图标
                RenderUtils.renderItemAtFloatPos(drawContext, stack, itemX, itemY);

                // 物品名
                String itemName = stack.getName().getString();
                FontManager.medium.drawString(
                        12,
                        itemName,
                        x1 + width / 2f - FontManager.medium.getStringWidth(itemName, 12) / 2f,
                        itemY + 18f,
                        new Color(230, 230, 230, 255),
                        true
                );
            }
        }
    }

    /**
     * 将实体 3D 包围盒投影成屏幕 2D 区域
     */
    private float[] getScreenBounds(PlayerEntity entity, Matrix4f modelView, Matrix4f projection) {
        Box box = entity.getBoundingBox().expand(0.08);

        Vec3d[] corners = new Vec3d[]{
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ)
        };

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean anyVisible = false;

        for (Vec3d corner : corners) {
            Pair<Vec3d, Boolean> projected = RenderUtils.project(modelView, projection, corner);
            if (projected == null || projected.getRight() == null || !projected.getRight()) continue;

            Vec3d screen = projected.getLeft();
            anyVisible = true;

            minX = Math.min(minX, (float) screen.x);
            minY = Math.min(minY, (float) screen.y);
            maxX = Math.max(maxX, (float) screen.x);
            maxY = Math.max(maxY, (float) screen.y);
        }

        if (!anyVisible) return null;
        if (minX == Float.MAX_VALUE || minY == Float.MAX_VALUE) return null;

        return new float[]{minX, minY, maxX, maxY};
    }

    private Color getHealthColor(float progress) {
        progress = Math.max(0f, Math.min(1f, progress));

        int r = (int) (255 * (1f - progress));
        int g = (int) (255 * progress);
        return new Color(r, g, 60, 255);
    }
}