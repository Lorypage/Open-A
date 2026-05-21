package cn.lazymoon.utils.client;

import net.minecraft.block.*;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

public class LegacyBlockingUtil {
    public static int getHandMultiplier(PlayerEntity player, Hand hand) {
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        return getArmMultiplier(arm);
    }

    public static int getArmMultiplier(Arm arm) {
        return arm == Arm.RIGHT ? 1 : -1;
    }

    public static void applyLegacyFirstpersonTransforms(MatrixStack poseStack, int direction, Runnable runnable) {
     //   poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 45.0F));
      //  poseStack.scale(0.4F, 0.4F, 0.4F);
        runnable.run();
        poseStack.scale(1 / 0.4F, 1 / 0.4F, 1 / 0.4F);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -45.0F));
    }

    public static boolean shoulditemPositionsInThirdPerson(EntityRenderState entityState) {
     //   if (AnimatiumConfig.instance().itemPositionsInThirdPerson) {
            return true;
    //    } else {
      //      final Entity entity = LegacyEntityUtils.getEntityByState(entityState);
     //       if (entity instanceof LivingEntity livingEntity) {
     //           return livingEntity.isBlocking();
     //       } else {
     //           return false;
    //        }
    //    }
    }

    public static boolean isBlock3d(ItemStack stack, ItemRenderState itemStackRenderState) {
        if (!stack.isEmpty()) {
            return stack.getItem() instanceof BlockItem && itemStackRenderState.hasDepth();
        } else {
            return false;
        }
    }

    public static boolean isItemBlacklisted(ItemStack stack) {
        if (!stack.isEmpty()) {
            final Item item = stack.getItem();
            return isShieldItem(item) ||
                    isBlockItemBlacklisted(stack) ||
                    item instanceof CrossbowItem;
        } else {
            return false;
        }
    }


    public static boolean isShieldItem(Item item) {
        return item == Items.SHIELD || item instanceof ShieldItem;
    }

    public static boolean isBlockItemBlacklisted(ItemStack stack) {
        if (!stack.isEmpty()) {
            final Block block = Block.getBlockFromItem(stack.getItem());
            return block instanceof BannerBlock ||
                    block instanceof RodBlock ||
                    block instanceof BedBlock ||
                    isSkullBlock(stack);
        } else {
            return false;
        }
    }

    public static boolean isSkullBlock(ItemStack stack) {
        if (!stack.isEmpty()) {
            return Block.getBlockFromItem(stack.getItem()) instanceof SkullBlock;
        } else {
            return false;
        }
    }


    public static boolean isFishingRodItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            final Item item = stack.getItem();
            return item instanceof FishingRodItem ||
                    item instanceof OnAStickItem<?>;
        } else {
            return false;
        }
    }

    public static boolean isHandheldItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            return isDiggerItem(stack) ||
                    isSwordItem(stack) ||
                    isFishingRodItem(stack) ||
                    List.of(Items.MACE,
                            Items.TRIDENT,
                            Items.STICK,
                            Items.BREEZE_ROD,
                            Items.BLAZE_ROD).contains(stack.getItem());
        } else {
            return false;
        }
    }

    public static boolean isSwordItem(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS);
    }

    public static boolean isAxeItem(ItemStack stack) {
        return stack.isIn(ItemTags.AXES);
    }

    public static boolean isPickaxeItem(ItemStack stack) {
        return stack.isIn(ItemTags.PICKAXES);
    }

    public static boolean isShovelItem(ItemStack stack) {
        return stack.isIn(ItemTags.SHOVELS);
    }

    public static boolean isHoeItem(ItemStack stack) {
        return stack.isIn(ItemTags.HOES);
    }

    public static boolean isDiggerItem(ItemStack stack) {
        return isAxeItem(stack) || isPickaxeItem(stack) || isShovelItem(stack) || isHoeItem(stack);
    }


}
