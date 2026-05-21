package cn.lazymoon.utils.player;

import cn.lazymoon.features.module.impl.world.ContainerStealer;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.misc.ItemUtils;
import lombok.experimental.UtilityClass;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.lazymoon.utils.misc.ItemUtils.getEnchantLevel;

/**
 * @Author:Guyuemang
 * @Time:03-01
 */
@UtilityClass
public class InventoryUtils implements InstanceAccess {
    public static boolean serverOpenInventory;
    public static final int HELMET_SLOT_INVENTORY = 39, HELMET_SLOT_INTERACTION_MANAGER = 5;
    public static final int CHEST_PLATE_SLOT_INVENTORY = 38, CHEST_PLATE_SLOT_INTERACTION_MANAGER = 6;
    public static final int LEGGINGS_SLOT_INVENTORY = 37, LEGGINGS_SLOT_INTERACTION_MANAGER = 7;
    public static final int BOOTS_SLOT_INVENTORY = 36, BOOTS_SLOT_INTERACTION_MANAGER = 8;

    public static boolean isContainerUsefulItem(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.isEmpty()) return false;

        Item item = itemStack.getItem();

        if (item instanceof ArmorItem) return InventoryUtils.getProtection(itemStack) > InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
        if (item instanceof CrossbowItem) return InventoryUtils.getCrossbowScore(itemStack) > InventoryUtils.getBestCrossbowScore();
        if (item instanceof BowItem) return true;
        if (item instanceof AxeItem) return InventoryUtils.getAxeScore(itemStack) > InventoryUtils.getBestAxeScore();
        if (item instanceof PickaxeItem) return InventoryUtils.getToolScore(itemStack) > InventoryUtils.getBestPickaxeScore();
        if (item instanceof ShovelItem) return InventoryUtils.getBestShovel() == itemStack;
        if (item instanceof HoeItem) return true;
        if (item instanceof SwordItem) return InventoryUtils.getSwordDamage(itemStack) > InventoryUtils.getBestSwordDamage();

        if (item instanceof PotionItem) return true;
        if (item instanceof PlayerHeadItem) return true;

        if (item instanceof BlockItem) {
            Block block = Block.getBlockFromItem(item);
            if (block instanceof AirBlock) return false;
            if (block instanceof StairsBlock) return false;
            if (block instanceof SlabBlock) return false;
            if (block instanceof FenceBlock) return false;
            if (block instanceof FenceGateBlock) return false;
            if (block instanceof DoorBlock) return false;
            if (block instanceof TrapdoorBlock) return false;
            if (block instanceof PressurePlateBlock) return false;
            if (block instanceof ButtonBlock) return false;
            if (block instanceof WallBlock) return false;
            if (block instanceof CarpetBlock) return false;
            if (block instanceof ConcretePowderBlock) return false;
            if (block instanceof TintedGlassBlock) return false;
            if (block instanceof StainedGlassBlock) return false;
            if (block instanceof StainedGlassPaneBlock) return false;
            if (block instanceof BannerBlock) return false;
            if (block instanceof LeavesBlock) return false;
            if (block instanceof SaplingBlock) return false;
            if (block instanceof MushroomBlock) return false;
            if (block instanceof MushroomPlantBlock) return false;
            if (block instanceof FlowerBlock) return false;
            if (block instanceof ComposterBlock) return false;
            if (block instanceof NoteBlock) return false;
            if (block instanceof JukeboxBlock) return false;
            if (block instanceof SignBlock) return false;
            if (block == Blocks.IRON_BARS) return false;
            if (block == Blocks.CHAIN) return false;
            if (block == Blocks.GLASS_PANE) return false;
            if (block == Blocks.DIRT_PATH) return false;
            if (block == Blocks.FARMLAND) return false;
            if (block == Blocks.SNOW) return false;
            if (block == Blocks.AMETHYST_CLUSTER) return false;
            if (block == Blocks.MANGROVE_ROOTS) return false;
            if (block == Blocks.SUGAR_CANE) return false;
            if (block == Blocks.CACTUS) return false;
            if (block == Blocks.LILY_PAD) return false;

            return true;
        }

        return false;
    }

    public static final int OFF_HAND_SLOT_INVENTORY = 40;
    
    public static boolean playerInventoryHasEmptySlot() {
        for (int slotID = 0; slotID < mc.player.getInventory().size(); ++slotID) {
            if (slotID == HELMET_SLOT_INVENTORY || slotID == CHEST_PLATE_SLOT_INVENTORY || slotID == LEGGINGS_SLOT_INVENTORY || slotID == BOOTS_SLOT_INVENTORY || slotID == OFF_HAND_SLOT_INVENTORY)
                continue;

            ItemStack itemStack = mc.player.getInventory().getStack(slotID);

            if (itemStack == null) return true;
            if (itemStack.isEmpty()) return true;
        }

        return false;
    }

    public float getBestSwordDamage() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof SwordItem && ItemUtils.isValidItem(item)).map(InventoryUtils::getSwordDamage).max(Float::compareTo).orElse(0f);
    }

    public float getBestAxeScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && ItemUtils.isValidItem(item)).map(InventoryUtils::getAxeScore).max(Float::compareTo).orElse(0f);
    }

    public float getBestPickaxeScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof PickaxeItem && ItemUtils.isValidItem(item)).map(InventoryUtils::getToolScore).max(Float::compareTo).orElse(0f);
    }

    public float getBestCrossbowScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && ItemUtils.isValidItem(item)).map(InventoryUtils::getCrossbowScore).max(Float::compareTo).orElse(0f);
    }

    public float getBestPowerBowScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && ItemUtils.isValidItem(item)).map(InventoryUtils::getPowerBowScore).max(Float::compareTo).orElse(0f);
    }

    public float getBestPunchBowScore() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && ItemUtils.isValidItem(item)).map(InventoryUtils::getPunchBowScore).max(Float::compareTo).orElse(0f);
    }

    public float getBestArmorScore(EquipmentType type) {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof ArmorItem && ItemUtils.getEquipmentType(item) == type).map(InventoryUtils::getProtection).max(Float::compareTo).orElse(0f);
    }

    public int getBlockCountInInventory() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && ItemUtils.isValidBlock(item)).mapToInt(ItemStack::getCount).sum();
    }

    public int getFoodCountInInventoryExcludingGapple() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && ItemUtils.isFood(item) && item.getItem() != Items.GOLDEN_APPLE && item.getItem() != Items.ENCHANTED_GOLDEN_APPLE && ItemUtils.isValidItem(item)).mapToInt(ItemStack::getCount).sum();
    }

    public ItemStack getBestSword() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof SwordItem && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(stack -> (int) (getSwordDamage(stack) * 100))).orElse(null);
    }

    public ItemStack getBestAxe() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof AxeItem && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getAxeScore(s) * 100))).orElse(null);
    }

    public ItemStack getBestPickaxe() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof PickaxeItem && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getToolScore(s) * 100))).orElse(null);
    }

    public ItemStack getBestShovel() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof ShovelItem && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getToolScore(s) * 100))).orElse(null);
    }

    public ItemStack getBestCrossbow() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getCrossbowScore(s) * 100))).orElse(null);
    }

    public ItemStack getBestPunchBow() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getPunchBowScore(s) * 100))).orElse(null);
    }

    public ItemStack getBestPowerBow() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(s -> (int) (getPowerBowScore(s) * 100))).orElse(null);
    }

    public ItemStack getBestGapple() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && (item.getItem() == Items.GOLDEN_APPLE || item.getItem() == Items.ENCHANTED_GOLDEN_APPLE) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public ItemStack getBestEnderPearl() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == Items.ENDER_PEARL && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public ItemStack getBestBlock() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && ItemUtils.isValidBlock(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public ItemStack getWorstBlock() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && ItemUtils.isValidBlock(item)).min(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public ItemStack getBestThrowable() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && ItemUtils.isValidItem(item)).max(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public ItemStack getWorstThrowable() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && ItemUtils.isValidItem(item)).min(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public ItemStack getWorstFoodExcludingGapple() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && ItemUtils.isFood(item) && item.getItem() != Items.GOLDEN_APPLE && item.getItem() != Items.ENCHANTED_GOLDEN_APPLE && ItemUtils.isValidItem(item)).min(Comparator.comparingInt(ItemStack::getCount)).orElse(null);
    }

    public int getItemCount(Item checkItem) {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == checkItem).mapToInt(ItemStack::getCount).sum();
    }

    public static float getSwordDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
            return 0;
        }

        float valence = 0;
        var comp = stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);

        if (comp != null) {
            double add = 0;
            double addBase = 0;
            double addTotal = 0;

            for (var entry : comp.modifiers()) {
                if (entry.slot() == AttributeModifierSlot.MAINHAND && entry.attribute().matchesKey(EntityAttributes.ATTACK_DAMAGE.getKey().orElseThrow())) {
                    switch (entry.modifier().operation()) {
                        case ADD_VALUE -> add += entry.modifier().value();
                        case ADD_MULTIPLIED_BASE -> addBase += entry.modifier().value();
                        case ADD_MULTIPLIED_TOTAL -> addTotal += entry.modifier().value();
                    }
                }
            }

            valence += (float) ((add * (1 + addBase)) * (1 + addTotal));
        }

        int itemEnchantmentLevel = getEnchantLevel(stack, Enchantments.SHARPNESS);

        if (itemEnchantmentLevel > 0) {
            valence += 0.5f * itemEnchantmentLevel + 0.5f;
        }

        return valence;
    }

    public static float getAttackDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0f;
        }

        float damage = 0f;

        var attrComp = stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attrComp != null) {
            for (var entry : attrComp.modifiers()) {
                if (entry.slot() == AttributeModifierSlot.MAINHAND && entry.attribute().matchesKey(EntityAttributes.ATTACK_DAMAGE.getKey().orElseThrow())) {

                    var mod = entry.modifier();
                    switch (mod.operation()) {
                        case ADD_VALUE -> damage += mod.value();
                        case ADD_MULTIPLIED_BASE -> damage += mod.value();
                        case ADD_MULTIPLIED_TOTAL -> damage += mod.value();
                    }
                }
            }
        }

        int sharpnessLevel = getEnchantLevel(stack, Enchantments.SHARPNESS);
        if (sharpnessLevel > 0) {
            damage += 0.5f * sharpnessLevel + 0.5f;
        }

        return damage;
    }

    public static float getAxeScore(ItemStack stack) {
        float valence = getToolScore(stack);

        if (ItemUtils.isGodItem(stack)) {
            return valence * 1000;
        }

        int itemEnchantmentLevel = getEnchantLevel(stack, Enchantments.SHARPNESS);

        if (itemEnchantmentLevel > 0) {
            valence += 0.5f * itemEnchantmentLevel + 0.5f;
        }

        return valence;
    }

    public static float getToolScore(ItemStack stack) {
        float valence = 0;

        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        switch (stack.getItem()) {
            case PickaxeItem ignored -> valence += stack.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState());
            case AxeItem ignored -> valence += stack.getMiningSpeedMultiplier(Blocks.OAK_LOG.getDefaultState());
            case ShovelItem ignored -> valence += stack.getMiningSpeedMultiplier(Blocks.DIRT.getDefaultState());
            case null, default -> {
                return 0;
            }
        }

        int efficiency = getEnchantLevel(stack, Enchantments.EFFICIENCY);

        if (efficiency > 0) {
            valence += efficiency * 0.0075F;
        }

        return valence;
    }

    public static float getCrossbowScore(ItemStack stack) {
        int valence = 0;

        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.getItem() instanceof CrossbowItem) {
            valence += getEnchantLevel(stack, Enchantments.QUICK_CHARGE);
            valence += getEnchantLevel(stack, Enchantments.MULTISHOT);
            valence += getEnchantLevel(stack, Enchantments.PIERCING);
        }

        return valence;
    }

    public static float getPunchBowScore(ItemStack stack) {
        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.getItem() instanceof BowItem) {
            float valence = 10;
            valence += getEnchantLevel(stack, Enchantments.PUNCH);
            valence += getEnchantLevel(stack, Enchantments.INFINITY);
            valence += getEnchantLevel(stack, Enchantments.FLAME);
            valence += getEnchantLevel(stack, Enchantments.POWER) / 10F;
            return valence + ((float) stack.getDamage() / stack.getMaxDamage());
        }

        return 0;
    }

    public static float getPowerBowScore(ItemStack stack) {
        if (stack == null) {
            return 0;
        }

        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.getItem() instanceof BowItem) {
            float valence = 10;
            valence += getEnchantLevel(stack, Enchantments.PUNCH) / 10F;
            valence += getEnchantLevel(stack, Enchantments.INFINITY);
            valence += getEnchantLevel(stack, Enchantments.FLAME);
            valence += getEnchantLevel(stack, Enchantments.POWER);
            return valence + ((float) stack.getDamage() / stack.getMaxDamage());
        }

        return 0;
    }

    public static float getProtection(ItemStack stack) {
        float valence = 0;

        if (stack == null || stack.isEmpty()) {
            return valence;
        }

        var modifiers = stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);

        if (modifiers != null) {
            for (var entry : modifiers.modifiers()) {
                if (entry.attribute().matchesKey(EntityAttributes.ARMOR.getKey().orElseThrow())) {
                    var mod = entry.modifier();

                    if (mod.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                        valence += (float) mod.value();
                    }
                }

                if (entry.attribute().matchesKey(EntityAttributes.ARMOR_TOUGHNESS.getKey().orElseThrow())) {
                    var mod = entry.modifier();

                    if (mod.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                        valence += (float) mod.value();
                    }
                }
            }
        }

        valence += getEnchantLevel(stack, Enchantments.PROTECTION);

        return valence;
    }

    public List<ItemStack> getAllItems() {
        ArrayList<ItemStack> list = new ArrayList<>(41);

        if (mc.player != null) {
            list.addAll(mc.player.getInventory().main);
            list.addAll(mc.player.getInventory().offHand);
            list.addAll(mc.player.getInventory().armor);
        }

        return list;
    }

    public static boolean isUsefulItem(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.isEmpty()) return false;

        Item item = itemStack.getItem();

        if (item instanceof ArmorItem) return true;
        if (item instanceof CrossbowItem) return true;
        if (item instanceof BowItem) return true;
        if (item instanceof AxeItem) return true;
        if (item instanceof PickaxeItem) return true;
        if (item instanceof ShovelItem) return true;
        if (item instanceof HoeItem) return true;
        if (item instanceof SwordItem) return true;

        if (item instanceof PotionItem) return true;
        if (item instanceof PlayerHeadItem) return true;

        if (item instanceof BlockItem) {
            Block block = Block.getBlockFromItem(item);
            if (block instanceof AirBlock) return false;
            if (block instanceof StairsBlock) return false;
            if (block instanceof SlabBlock) return false;
            if (block instanceof FenceBlock) return false;
            if (block instanceof FenceGateBlock) return false;
            if (block instanceof DoorBlock) return false;
            if (block instanceof TrapdoorBlock) return false;
            if (block instanceof PressurePlateBlock) return false;
            if (block instanceof ButtonBlock) return false;
            if (block instanceof WallBlock) return false;
            if (block instanceof CarpetBlock) return false;
            if (block instanceof ConcretePowderBlock) return false;
            if (block instanceof TintedGlassBlock) return false;
            if (block instanceof StainedGlassBlock) return false;
            if (block instanceof StainedGlassPaneBlock) return false;
            if (block instanceof BannerBlock) return false;
            if (block instanceof LeavesBlock) return false;
            if (block instanceof SaplingBlock) return false;
            if (block instanceof MushroomBlock) return false;
            if (block instanceof MushroomPlantBlock) return false;
            if (block instanceof FlowerBlock) return false;
            if (block instanceof ComposterBlock) return false;
            if (block instanceof NoteBlock) return false;
            if (block instanceof JukeboxBlock) return false;
            if (block instanceof SignBlock) return false;
            if (block == Blocks.IRON_BARS) return false;
            if (block == Blocks.CHAIN) return false;
            if (block == Blocks.GLASS_PANE) return false;
            if (block == Blocks.DIRT_PATH) return false;
            if (block == Blocks.FARMLAND) return false;
            if (block == Blocks.SNOW) return false;
            if (block == Blocks.AMETHYST_CLUSTER) return false;
            if (block == Blocks.MANGROVE_ROOTS) return false;
            if (block == Blocks.SUGAR_CANE) return false;
            if (block == Blocks.CACTUS) return false;
            return block != Blocks.LILY_PAD;
        }

        if (itemStack.getItem() == Items.SLIME_BALL) return true;
        if (itemStack.getItem() == Items.FIRE_CHARGE)
            return true;
        if (itemStack.getItem() == Items.TOTEM_OF_UNDYING)
            return true;
        if (itemStack.getItem() == Items.GOLDEN_APPLE)
            return true;
        if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE)
            return true;
        if (itemStack.getItem() == Items.FISHING_ROD)
            return true;
        if (itemStack.getItem() == Items.SNOWBALL) return true;
        if (itemStack.getItem() == Items.EGG) return true;
        if (itemStack.getItem() == Items.BUCKET) return true;
        if (itemStack.getItem() == Items.WATER_BUCKET)
            return true;
        if (itemStack.getItem() == Items.LAVA_BUCKET)
            return true;
        if (itemStack.getItem() == Items.MILK_BUCKET)
            return true;
        if (itemStack.getItem() == Items.ENDER_PEARL)
            return true;
        if (itemStack.getItem() == Items.ARROW) return true;
        if (itemStack.getItem() == Items.SHIELD) return true;
        if (itemStack.getItem() == Items.END_CRYSTAL)
            return true;
        if (itemStack.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            return true;
        }

        return false;
    }

    public static boolean isItemValid(ItemStack s) {
        if (!s.isEmpty()) {
            if (s.getItem() instanceof PlayerHeadItem) {
                return false;
            }

            String string = s.getItemName().getString();
            if (string.contains("Click")) {
                return false;
            }

            if (string.contains("Right")) {
                return false;
            }

            if (string.contains("点击")) {
                return false;
            }

            if (string.contains("Teleport")) {
                return false;
            }

            if (string.contains("使用")) {
                return false;
            }

            if (string.contains("传送")) {
                return false;
            }

            if (string.contains("再来")) {
                return false;
            }
        }

        return true;
    }
}
