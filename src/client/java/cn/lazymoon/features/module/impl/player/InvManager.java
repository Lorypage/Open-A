package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.MotionEvent;
import cn.lazymoon.event.impl.world.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.combat.AntiKnockback;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.movement.GuiMove;
import cn.lazymoon.features.module.impl.world.Scaffold;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.misc.ItemUtils;
import cn.lazymoon.utils.player.ArmorUtils;
import cn.lazymoon.utils.player.InventoryUtils;
import cn.lazymoon.utils.player.MoveUtil;
import cn.lazymoon.utils.player.PlayerUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author:Guyuemang
 * @Time:02-28
 */
@ModuleInfo(name = "InvManager",description = "Help you automatically organize your backpack",key = 0, category = Category.Player,hidden = false)
public class InvManager extends Module {
    public static ModeValue mode = new ModeValue("Modes","OpenInv",new String[]{"OpenInv","Spoof"});

    public static BoolValue noMove = new BoolValue("No Move",false);

    public static BoolValue noLobby = new BoolValue("No Lobby",true);
    public static BoolValue nok = new BoolValue("No KillAura",true);
    public static BoolValue nos = new BoolValue("No Scaffold",true);
    public static BoolValue nosc = new BoolValue("No Screen",true);
    public static BoolValue noa = new BoolValue("No AntiKB",true);

    private final BoolValue randomOrder = new BoolValue("RandomOrder", true);
    public static BoolValue autoArmor = new BoolValue("Auto Armor",true);
    public static NumberValue sortDelay = (NumberValue) new NumberValue("Sort Delay",0,0,50,1);
    public static NumberValue throwDelay = (NumberValue) new NumberValue("Throw Delay",0,0,50,1);
    public static NumberValue autoArmorDelay = (NumberValue) new NumberValue("Auto Armor Delay",()-> autoArmor.get(),0,0,50,1);

    private static final String[] SLOT_OPTIONS = {
            "None",
            "Sword",
            "Bow",
            "Projectile",
            "Gapple",
            "Food",
            "Block",
            "Water",
            "Lava",
            "Potion",
            "Pickaxe",
            "Axe",
            "Shovel",
            "Pearl",
            "FishingRod",
            "TNT"
    };

    public static ModeValue slot1 = new ModeValue("Slot 1", "Sword", SLOT_OPTIONS);
    public static ModeValue slot2 = new ModeValue("Slot 2", "Projectile", SLOT_OPTIONS);
    public static ModeValue slot3 = new ModeValue("Slot 3", "Gapple", SLOT_OPTIONS);
    public static ModeValue slot4 = new ModeValue("Slot 4", "Block", SLOT_OPTIONS);
    public static ModeValue slot5 = new ModeValue("Slot 5", "Water", SLOT_OPTIONS);
    public static ModeValue slot6 = new ModeValue("Slot 6", "Potion", SLOT_OPTIONS);
    public static ModeValue slot7 = new ModeValue("Slot 7", "Pickaxe", SLOT_OPTIONS);
    public static ModeValue slot8 = new ModeValue("Slot 8", "Axe", SLOT_OPTIONS);
    public static ModeValue slot9 = new ModeValue("Slot 9", "Pearl", SLOT_OPTIONS);

    private static final List<Integer> slots = new ArrayList<>();
    public static TimerUtils sortTimer = new TimerUtils();
    public static TimerUtils throwTimer = new TimerUtils();
    public static TimerUtils autoArmorTimer = new TimerUtils();
    public static boolean randomised = false;
    public static boolean action = false;
    public static int coolDown = 0;

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @EventTarget
    public void onWorldChange(WorldEvent event) {
        this.setState(false);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        setSuffix(mode.get());
        if (event.getState() != MotionEvent.State.Pre) return;

        if (Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState()){
            return;
        }

        if (noMove.get() && MoveUtil.isMoving()){
            return;
        }

        if ((mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen) && nosc.get())
                || mc.player == null
                || mc.world == null
                || mc.player.isUsingItem()
                || (Client.INSTANCE.getModuleManager().getModule(Scaffold.class).isState() && nos.get())
                || (KillAura.target != null && nok.get())
                || (AntiKnockback.receiveVelocity && noa.get())) {
            return;
        }

        if (noLobby.get() && ClientUtils.isInLobbyOrSpectator()) {
            return;
        }

        if (coolDown > 0) {
            coolDown--;
            return;
        }

        if ((!mode.is("OpenInv") && mc.currentScreen == null)
                || mc.currentScreen instanceof InventoryScreen) {
            if (!randomised || !randomOrder.get()) {
                slots.clear();
                for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
                    slots.add(slot);
                }
            }
            if (randomOrder.get() && !randomised) {
                Collections.shuffle(slots);
                randomised = true;
            }

            boolean needInventoryAction = ((hasUselessItem()) || (autoArmor.get() && undressed()) || (unsorted()));

            if (!mode.is("OpenInv")) {
                InventoryUtils.serverOpenInventory = needInventoryAction;
            }

            if (!needInventoryAction || !mode.is("OpenInv") && !GuiMove.sent) {
                return;
            }

            autoThrowUseless();
            if (hasUselessItem())
                return;

            if (autoArmor.get()) {
                autoWear();
                if (undressed())
                    return;
            }

            autoSort();
            if (unsorted())
                return;

            coolDown = 8;
        } else {
            randomised = false;
        }
    }

    public static void autoThrowUseless() {
        if (mc.interactionManager == null || mc.player == null) return;

        int delay = throwDelay.get().intValue() * 10;

        for (int slotID = 0; slotID < mc.player.getInventory().size(); slotID++) {
            int currentSlot = slots.get(slotID);

            ItemStack itemStack = mc.player.getInventory().getStack(currentSlot);

            if (itemStack == null) continue;
            if (itemStack.isEmpty()) continue;

            if (!InventoryUtils.isUsefulItem(itemStack)) {
                if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.THROW, mc.player);
                    throwTimer.reset();
                }
            }

            if (itemStack.getItem() instanceof BowItem) {
                if (itemStack != InventoryUtils.getBestPowerBow() || isDuplicateBestBow()) {
                    if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                        int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                        throwTimer.reset();
                    }
                }
            }

            if (itemStack.getItem() instanceof SwordItem) {
                if (itemStack != InventoryUtils.getBestSword() || isDuplicateBestSword()) {
                    if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                        int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                        throwTimer.reset();
                    }
                }
            }

            if (itemStack.getItem() instanceof PickaxeItem) {
                if (itemStack != InventoryUtils.getBestPickaxe() || isDuplicateBestPickaxe()) {
                    if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                        int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                        throwTimer.reset();
                    }
                }
            }

            if (itemStack.getItem() instanceof AxeItem) {
                if (itemStack != InventoryUtils.getBestAxe() || isDuplicateBestAxe()) {
                    if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                        int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                        throwTimer.reset();
                    }
                }
            }

            if (itemStack.getItem() instanceof ShovelItem) {
                if (itemStack != InventoryUtils.getBestShovel() || isDuplicateBestShovel()) {
                    if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                        int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                        throwTimer.reset();
                    }
                }
            }

            if (ArmorUtils.armorHelmets.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.HELMET_SLOT_INTERACTION_MANAGER, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        } else {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                } else if (isDuplicateBestArmor(itemStack)) {
                    if (mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (isBestArmor2 && currentSlot != InventoryUtils.HELMET_SLOT_INVENTORY) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                }
            }

            if (ArmorUtils.armorChestPlates.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.CHEST_PLATE_SLOT_INTERACTION_MANAGER, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        } else {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                } else if (isDuplicateBestArmor(itemStack)) {
                    if (mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (isBestArmor2 && currentSlot != InventoryUtils.CHEST_PLATE_SLOT_INVENTORY) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                }
            }

            if (ArmorUtils.armorLeggings.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.LEGGINGS_SLOT_INTERACTION_MANAGER, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        } else {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                } else if (isDuplicateBestArmor(itemStack)) {
                    if (mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (isBestArmor2 && currentSlot != InventoryUtils.LEGGINGS_SLOT_INVENTORY) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                }
            }

            if (ArmorUtils.armorBoots.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.BOOTS_SLOT_INTERACTION_MANAGER, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        } else {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                } else if (isDuplicateBestArmor(itemStack)) {
                    if (mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY).isEmpty()) {
                        if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                            throwTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (isBestArmor2 && currentSlot != InventoryUtils.BOOTS_SLOT_INVENTORY) {
                            if (throwTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.THROW, mc.player);
                                throwTimer.reset();
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean isDuplicateBestSword() {
        if (mc.player == null) return false;

        ItemStack best = InventoryUtils.getBestSword();
        if (best == null) return false;

        var inventory = mc.player.getInventory();
        int found = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack s = inventory.getStack(i);
            if (s.isEmpty()) continue;

            if (!(s.getItem() instanceof SwordItem)) continue;

            if (s == best) {
                found++;
                if (found > 1)
                    return true;
            }
        }
        return false;
    }

    public static boolean isDuplicateBestAxe() {
        if (mc.player == null) return false;

        ItemStack best = InventoryUtils.getBestAxe();
        if (best == null) return false;

        var inventory = mc.player.getInventory();
        int found = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack s = inventory.getStack(i);
            if (s.isEmpty()) continue;

            if (!(s.getItem() instanceof AxeItem)) continue;

            if (s == best) {
                found++;
                if (found > 1)
                    return true;
            }
        }
        return false;
    }

    public static boolean isDuplicateBestPickaxe() {
        if (mc.player == null) return false;

        ItemStack best = InventoryUtils.getBestPickaxe();
        if (best == null) return false;

        var inventory = mc.player.getInventory();
        int found = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack s = inventory.getStack(i);
            if (s.isEmpty()) continue;
            if (!(s.getItem() instanceof PickaxeItem)) continue;
            if (s == best) {
                found++;
                if (found > 1)
                    return true;
            }
        }
        return false;
    }

    public static boolean isDuplicateBestShovel() {
        if (mc.player == null) return false;

        ItemStack best = InventoryUtils.getBestShovel();
        if (best == null) return false;

        var inventory = mc.player.getInventory();
        int found = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack s = inventory.getStack(i);
            if (s.isEmpty()) continue;
            if (!(s.getItem() instanceof ShovelItem)) continue;
            if (s == best) {
                found++;
                if (found > 1)
                    return true;
            }
        }
        return false;
    }

    public static boolean isDuplicateBestArmor(ItemStack stack) {
        if (mc.player == null) return false;

        EquipmentType type = ItemUtils.getEquipmentType(stack);
        if (type == null) return false;

        float bestScore = InventoryUtils.getBestArmorScore(type);
        float thisScore = InventoryUtils.getProtection(stack);

        if (thisScore < bestScore) {
            return true;
        }

        int found = 0;

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isEmpty()) continue;

            if (ItemUtils.getEquipmentType(s) != type) continue;

            if (InventoryUtils.getProtection(s) == bestScore) {
                found++;
                if (found > 1) {
                    return true;
                }
            }
        }

        return false;
    }
    public static int getSlotWithMostBlock(int selectSlot) {
        if (mc.player == null || mc.player.getInventory() == null) {
            return -1;
        }

        int maxBlockSlot = -1;
        int maxBlockCount = 0;

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            if (selectSlot != 0 && slot1.is("Block") && slot == 0 || selectSlot != 1 && slot2.is("Block") && slot == 1 ||
                    selectSlot != 2 && slot3.is("Block") && slot == 2 || selectSlot != 3 && slot4.is("Block") && slot == 3
                    || selectSlot != 4 && slot5.is("Block") && slot == 4 || selectSlot != 5 && slot6.is("Block") && slot == 5 ||
                    selectSlot != 6 && slot7.is("Block") && slot == 6 || selectSlot != 7 && slot8.is("Block") && slot == 7 ||
                    selectSlot != 8 && slot9.is("Block") && slot == 8) continue;
            ItemStack itemStack = mc.player.getInventory().getStack(slot);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            if (itemStack.getItem() instanceof BlockItem) {
                int blockCount = itemStack.getCount();

                if (blockCount > maxBlockCount) {
                    maxBlockCount = blockCount;
                    maxBlockSlot = slot;
                }
            }
        }

        return maxBlockSlot;
    }

    public static boolean isFood(ItemStack itemStack) {
        return ItemUtils.isConsumable(itemStack) && !(itemStack.getItem() instanceof PotionItem) && itemStack.getItem() != Items.GOLDEN_APPLE && itemStack.getItem() != Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static int getSlotWithMostFood(int selectSlot) {
        if (mc.player == null || mc.player.getInventory() == null) {
            return -1;
        }

        int maxFoodSlot = -1;
        int maxFoodCount = 0;

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            if (selectSlot != 0 && slot1.is("Food") && slot == 0 || selectSlot != 1 && slot2.is("Food") && slot == 1 ||
                    selectSlot != 2 && slot3.is("Food") && slot == 2 || selectSlot != 3 && slot4.is("Food") && slot == 3
                    || selectSlot != 4 && slot5.is("Food") && slot == 4 || selectSlot != 5 && slot6.is("Food") && slot == 5 ||
                    selectSlot != 6 && slot7.is("Food") && slot == 6 || selectSlot != 7 && slot8.is("Food") && slot == 7 ||
                    selectSlot != 8 && slot9.is("Food") && slot == 8) continue;
            ItemStack itemStack = mc.player.getInventory().getStack(slot);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            if (isFood(itemStack)) {
                int foodCount = itemStack.getCount();

                if (foodCount > maxFoodCount) {
                    maxFoodCount = foodCount;
                    maxFoodSlot = slot;
                }
            }
        }

        return maxFoodSlot;
    }

    public static int getSlotWithMostGapple(int selectSlot) {
        if (mc.player == null || mc.player.getInventory() == null) {
            return -1;
        }

        int maxGappleSlot = -1;
        int maxGappleCount = 0;

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            if (selectSlot != 0 && slot1.is("Gapple") && slot == 0 || selectSlot != 1 && slot2.is("Gapple") && slot == 1 ||
                    selectSlot != 2 && slot3.is("Gapple") && slot == 2 || selectSlot != 3 && slot4.is("Gapple") && slot == 3
                    || selectSlot != 4 && slot5.is("Gapple") && slot == 4 || selectSlot != 5 && slot6.is("Gapple") && slot == 5 ||
                    selectSlot != 6 && slot7.is("Gapple") && slot == 6 || selectSlot != 7 && slot8.is("Gapple") && slot == 7 ||
                    selectSlot != 8 && slot9.is("Gapple") && slot == 8) continue;
            ItemStack itemStack = mc.player.getInventory().getStack(slot);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            if (itemStack.getItem() == Items.GOLDEN_APPLE) {
                int GappleCount = itemStack.getCount();

                if (GappleCount > maxGappleCount) {
                    maxGappleCount = GappleCount;
                    maxGappleSlot = slot;
                }
            }
        }

        return maxGappleSlot;
    }
    public static int getSlotWithMostProjectile(int selectSlot) {
        if (mc.player == null || mc.player.getInventory() == null) {
            return -1;
        }

        int maxProjectileSlot = -1;
        int maxProjectileCount = 0;

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            if (selectSlot != 0 && slot1.is("Projectile") && slot == 0 || selectSlot != 1 && slot2.is("Projectile") && slot == 1 ||
                    selectSlot != 2 && slot3.is("Projectile") && slot == 2 || selectSlot != 3 && slot4.is("Projectile") && slot == 3
                    || selectSlot != 4 && slot5.is("Projectile") && slot == 4 || selectSlot != 5 && slot6.is("Projectile") && slot == 5 ||
                    selectSlot != 6 && slot7.is("Projectile") && slot == 6 || selectSlot != 7 && slot8.is("Projectile") && slot == 7 ||
                    selectSlot != 8 && slot9.is("Projectile") && slot == 8) continue;
            ItemStack itemStack = mc.player.getInventory().getStack(slot);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            if (itemStack.getItem() == Items.EGG || itemStack.getItem() == Items.SNOWBALL) {
                int ProjectileCount = itemStack.getCount();

                if (ProjectileCount > maxProjectileCount) {
                    maxProjectileCount = ProjectileCount;
                    maxProjectileSlot = slot;
                }
            }
        }

        return maxProjectileSlot;
    }

    public static int getSlotWithMostPearl(int selectSlot) {
        if (mc.player == null || mc.player.getInventory() == null) {
            return -1;
        }

        int maxPearlSlot = -1;
        int maxPearlCount = 0;

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            if (selectSlot != 0 && slot1.is("Pearl") && slot == 0 || selectSlot != 1 && slot2.is("Pearl") && slot == 1 ||
                    selectSlot != 2 && slot3.is("Pearl") && slot == 2 || selectSlot != 3 && slot4.is("Pearl") && slot == 3
                    || selectSlot != 4 && slot5.is("Pearl") && slot == 4 || selectSlot != 5 && slot6.is("Pearl") && slot == 5 ||
                    selectSlot != 6 && slot7.is("Pearl") && slot == 6 || selectSlot != 7 && slot8.is("Pearl") && slot == 7 ||
                    selectSlot != 8 && slot9.is("Pearl") && slot == 8) continue;
            ItemStack itemStack = mc.player.getInventory().getStack(slot);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            if (itemStack.getItem() == Items.ENDER_PEARL) {
                int PearlCount = itemStack.getCount();

                if (PearlCount > maxPearlCount) {
                    maxPearlCount = PearlCount;
                    maxPearlSlot = slot;
                }
            }
        }

        return maxPearlSlot;
    }

    public static int getSlotWithMostTNT(int selectSlot) {
        if (mc.player == null || mc.player.getInventory() == null) {
            return -1;
        }

        int maxTNTSlot = -1;
        int maxTNTCount = 0;

        for (int slot = 0; slot < mc.player.getInventory().size(); slot++) {
            if (selectSlot != 0 && slot1.is("TNT") && slot == 0 || selectSlot != 1 && slot2.is("TNT") && slot == 1 ||
                    selectSlot != 2 && slot3.is("TNT") && slot == 2 || selectSlot != 3 && slot4.is("TNT") && slot == 3
                    || selectSlot != 4 && slot5.is("TNT") && slot == 4 || selectSlot != 5 && slot6.is("TNT") && slot == 5 ||
                    selectSlot != 6 && slot7.is("TNT") && slot == 6 || selectSlot != 7 && slot8.is("TNT") && slot == 7 ||
                    selectSlot != 8 && slot9.is("TNT") && slot == 8) continue;
            ItemStack itemStack = mc.player.getInventory().getStack(slot);

            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }

            if (itemStack.getItem() == Items.TNT) {
                int TNTCount = itemStack.getCount();

                if (TNTCount > maxTNTCount) {
                    maxTNTCount = TNTCount;
                    maxTNTSlot = slot;
                }
            }
        }

        return maxTNTSlot;
    }

    public static boolean hasUselessItem() {
        if (mc.player == null) return false;

        for (int slotID = 0; slotID < mc.player.getInventory().size(); slotID++) {
            int currentSlot = slots.get(slotID);

            ItemStack itemStack = mc.player.getInventory().getStack(currentSlot);
            if (itemStack == null) continue;
            if (itemStack.isEmpty()) continue;

            if (!InventoryUtils.isUsefulItem(itemStack))
                return true;

            if (itemStack.getItem() instanceof BowItem) {
                if (itemStack != InventoryUtils.getBestPowerBow() || isDuplicateBestBow())
                    return true;
            }

            if (itemStack.getItem() instanceof SwordItem) {
                if (itemStack != InventoryUtils.getBestSword() || isDuplicateBestSword())
                    return true;
            }

            if (itemStack.getItem() instanceof PickaxeItem) {
                if (itemStack != InventoryUtils.getBestPickaxe() || isDuplicateBestPickaxe())
                    return true;
            }

            if (itemStack.getItem() instanceof AxeItem) {
                if (itemStack != InventoryUtils.getBestAxe() || isDuplicateBestAxe())
                    return true;
            }

            if (itemStack.getItem() instanceof ShovelItem) {
                if (itemStack != InventoryUtils.getBestShovel() || isDuplicateBestShovel())
                    return true;
            }

            if (ArmorUtils.armorHelmets.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor)
                    return true;

                if (isDuplicateBestArmor(itemStack)) {
                    return true;
                }
            }

            if (ArmorUtils.armorChestPlates.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor)
                    return true;

                if (isDuplicateBestArmor(itemStack)) {
                    return true;
                }
            }

            if (ArmorUtils.armorLeggings.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor)
                    return true;

                if (isDuplicateBestArmor(itemStack)) {
                    return true;
                }
            }

            if (ArmorUtils.armorBoots.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (!isBestArmor)
                    return true;

                if (isDuplicateBestArmor(itemStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void autoWear() {
        if (mc.player == null || mc.interactionManager == null) return;

        int delay = autoArmorDelay.get().intValue() * 10;

        for (int slotID = 0; slotID < mc.player.getInventory().size(); slotID++) {
            int currentSlot = slots.get(slotID);

            ItemStack itemStack = mc.player.getInventory().getStack(currentSlot);

            if (itemStack == null) continue;
            if (itemStack.isEmpty()) continue;

            if (ArmorUtils.armorHelmets.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY).isEmpty()) {
                        if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.QUICK_MOVE, mc.player);
                            autoArmorTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.HELMET_SLOT_INTERACTION_MANAGER, 0, SlotActionType.QUICK_MOVE, mc.player);
                                autoArmorTimer.reset();
                            }
                        }
                    }
                }
            }
            if (ArmorUtils.armorChestPlates.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY).isEmpty()) {
                        if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.QUICK_MOVE, mc.player);
                            autoArmorTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.CHEST_PLATE_SLOT_INTERACTION_MANAGER, 0, SlotActionType.QUICK_MOVE, mc.player);
                                autoArmorTimer.reset();
                            }
                        }
                    }
                }
            }
            if (ArmorUtils.armorLeggings.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY).isEmpty()) {
                        if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.QUICK_MOVE, mc.player);
                            autoArmorTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.LEGGINGS_SLOT_INTERACTION_MANAGER, 0, SlotActionType.QUICK_MOVE, mc.player);
                                autoArmorTimer.reset();
                            }
                        }
                    }
                }
            }
            if (ArmorUtils.armorBoots.contains(itemStack.getItem())) {
                var bestArmorScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(itemStack));
                var armorScore = InventoryUtils.getProtection(itemStack);
                var isBestArmor = bestArmorScore == armorScore;

                if (isBestArmor) {
                    if (mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY).isEmpty()) {
                        if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                            int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.QUICK_MOVE, mc.player);
                            autoArmorTimer.reset();
                        }
                    } else {
                        var bestArmorScore2 = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY)));
                        var armorScore2 = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY));
                        var isBestArmor2 = bestArmorScore2 == armorScore2;

                        if (!isBestArmor2) {
                            if (autoArmorTimer.hasTimeElapsed(delay) || delay == 0) {
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, InventoryUtils.BOOTS_SLOT_INTERACTION_MANAGER, 0, SlotActionType.QUICK_MOVE, mc.player);
                                autoArmorTimer.reset();
                            }
                        }
                    }
                }
            }
        }
    }

    public void reset(){
        if (mc.player == null || mc.world == null) {
            return;
        }
        action = false;
        coolDown = -1;
        InventoryUtils.serverOpenInventory = false;
        sortTimer.reset();
        throwTimer.reset();
        autoArmorTimer.reset();
    }

    public static boolean unsorted() {
        if (mc.player == null) return false;

        switch (slot1.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(0) == null || mc.player.getInventory().getStack(0).isEmpty() || mc.player.getInventory().getStack(0) != bestSword)
                        && !(slot2.is("Sword") || slot3.is("Sword") || slot4.is("Sword") || slot5.is("Sword") || slot6.is("Sword") || slot7.is("Sword") || slot8.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(0) == null || mc.player.getInventory().getStack(0).isEmpty() || mc.player.getInventory().getStack(0) != bestBow)
                        && !(slot2.is("Bow") || slot3.is("Bow") || slot4.is("Bow") || slot5.is("Bow") || slot6.is("Bow") || slot7.is("Bow") || slot8.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(0) == null || mc.player.getInventory().getStack(0).isEmpty() || mc.player.getInventory().getStack(0) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot3.is("Pickaxe") || slot4.is("Pickaxe") || slot5.is("Pickaxe") || slot6.is("Pickaxe") || slot7.is("Pickaxe") || slot8.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(0) == null || mc.player.getInventory().getStack(0).isEmpty() || mc.player.getInventory().getStack(0) != bestAxe)
                        && !(slot2.is("Axe") || slot3.is("Axe") || slot4.is("Axe") || slot5.is("Axe") || slot6.is("Axe") || slot7.is("Axe") || slot8.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(0) == null || mc.player.getInventory().getStack(0).isEmpty() || mc.player.getInventory().getStack(0) != bestShovel)
                        && !(slot2.is("Shovel") || slot3.is("Shovel") || slot4.is("Shovel") || slot5.is("Shovel") || slot6.is("Shovel") || slot7.is("Shovel") || slot8.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(0) != 0 && getSlotWithMostFood(0) != -1 && !(isFood(mc.player.getInventory().getStack(0)) && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(0)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(0) != 0 && getSlotWithMostBlock(0) != -1 && !(mc.player.getInventory().getStack(0).getItem() instanceof BlockItem && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(0)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(0);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(0);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(0) != 0 && getSlotWithMostGapple(0) != -1 && !(mc.player.getInventory().getStack(0).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(0)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(0) != 0 && getSlotWithMostPearl(0) != -1 && !(mc.player.getInventory().getStack(0).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(0)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(0);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(0) != 0 && getSlotWithMostProjectile(0) != -1 && !((mc.player.getInventory().getStack(0).getItem() instanceof EggItem || mc.player.getInventory().getStack(0).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(0)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(0);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(0) != 0 && getSlotWithMostTNT(0) != -1 && !(mc.player.getInventory().getStack(0).getItem() == Items.TNT && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(0)).getCount())) return true;
                break;
        }
        switch (slot2.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(1) == null || mc.player.getInventory().getStack(1).isEmpty() || mc.player.getInventory().getStack(1) != bestSword)
                        && !(slot1.is("Sword") || slot3.is("Sword") || slot4.is("Sword") || slot5.is("Sword") || slot6.is("Sword") || slot7.is("Sword") || slot8.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(1) == null || mc.player.getInventory().getStack(1).isEmpty() || mc.player.getInventory().getStack(1) != bestBow)
                        && !(slot1.is("Bow") || slot3.is("Bow") || slot4.is("Bow") || slot5.is("Bow") || slot6.is("Bow") || slot7.is("Bow") || slot8.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(1) == null || mc.player.getInventory().getStack(1).isEmpty() || mc.player.getInventory().getStack(1) != bestPickaxe)
                        && !(slot1.is("Pickaxe") || slot3.is("Pickaxe") || slot4.is("Pickaxe") || slot5.is("Pickaxe") || slot6.is("Pickaxe") || slot7.is("Pickaxe") || slot8.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(1) == null || mc.player.getInventory().getStack(1).isEmpty() || mc.player.getInventory().getStack(1) != bestAxe)
                        && !(slot1.is("Axe") || slot3.is("Axe") || slot4.is("Axe") || slot5.is("Axe") || slot6.is("Axe") || slot7.is("Axe") || slot8.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(1) == null || mc.player.getInventory().getStack(1).isEmpty() || mc.player.getInventory().getStack(1) != bestShovel)
                        && !(slot1.is("Shovel") || slot3.is("Shovel") || slot4.is("Shovel") || slot5.is("Shovel") || slot6.is("Shovel") || slot7.is("Shovel") || slot8.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(1) != 1 && getSlotWithMostFood(1) != -1 && !(isFood(mc.player.getInventory().getStack(1)) && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(1)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(1) != 1 && getSlotWithMostBlock(1) != -1 && !(mc.player.getInventory().getStack(1).getItem() instanceof BlockItem && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(1)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(1);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(1);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(1) != 1 && getSlotWithMostGapple(1) != -1 && !(mc.player.getInventory().getStack(1).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(1)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(1) != 1 && getSlotWithMostPearl(1) != -1 && !(mc.player.getInventory().getStack(1).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(1)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(1);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(1) != 1 && getSlotWithMostProjectile(1) != -1 && !((mc.player.getInventory().getStack(1).getItem() instanceof EggItem || mc.player.getInventory().getStack(1).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(1)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(1);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(1) != 1 && getSlotWithMostTNT(1) != -1 && !(mc.player.getInventory().getStack(1).getItem() == Items.TNT && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(1)).getCount())) return true;
                break;
        }
        switch (slot3.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(2) == null || mc.player.getInventory().getStack(2).isEmpty() || mc.player.getInventory().getStack(2) != bestSword)
                        && !(slot2.is("Sword") || slot1.is("Sword") || slot4.is("Sword") || slot5.is("Sword") || slot6.is("Sword") || slot7.is("Sword") || slot8.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(2) == null || mc.player.getInventory().getStack(2).isEmpty() || mc.player.getInventory().getStack(2) != bestBow)
                        && !(slot2.is("Bow") || slot1.is("Bow") || slot4.is("Bow") || slot5.is("Bow") || slot6.is("Bow") || slot7.is("Bow") || slot8.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(2) == null || mc.player.getInventory().getStack(2).isEmpty() || mc.player.getInventory().getStack(2) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot1.is("Pickaxe") || slot4.is("Pickaxe") || slot5.is("Pickaxe") || slot6.is("Pickaxe") || slot7.is("Pickaxe") || slot8.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(2) == null || mc.player.getInventory().getStack(2).isEmpty() || mc.player.getInventory().getStack(2) != bestAxe)
                        && !(slot2.is("Axe") || slot1.is("Axe") || slot4.is("Axe") || slot5.is("Axe") || slot6.is("Axe") || slot7.is("Axe") || slot8.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(2) == null || mc.player.getInventory().getStack(2).isEmpty() || mc.player.getInventory().getStack(2) != bestShovel)
                        && !(slot2.is("Shovel") || slot1.is("Shovel") || slot4.is("Shovel") || slot5.is("Shovel") || slot6.is("Shovel") || slot7.is("Shovel") || slot8.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(2) != 2 && getSlotWithMostFood(2) != -1 && !(isFood(mc.player.getInventory().getStack(2)) && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(2)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(2) != 2 && getSlotWithMostBlock(2) != -1 && !(mc.player.getInventory().getStack(2).getItem() instanceof BlockItem && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(2)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(2);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(2);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(2) != 2 && getSlotWithMostGapple(2) != -1 && !(mc.player.getInventory().getStack(2).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(2)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(2) != 2 && getSlotWithMostPearl(2) != -1 && !(mc.player.getInventory().getStack(2).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(2)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(2);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(2) != 2 && getSlotWithMostProjectile(2) != -1 && !((mc.player.getInventory().getStack(2).getItem() instanceof EggItem || mc.player.getInventory().getStack(2).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(2)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(2);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(2) != 2 && getSlotWithMostTNT(2) != -1 && !(mc.player.getInventory().getStack(2).getItem() == Items.TNT && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(2)).getCount())) return true;
                break;
        }
        switch (slot4.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(3) == null || mc.player.getInventory().getStack(3).isEmpty() || mc.player.getInventory().getStack(3) != bestSword)
                        && !(slot2.is("Sword") || slot3.is("Sword") || slot1.is("Sword") || slot5.is("Sword") || slot6.is("Sword") || slot7.is("Sword") || slot8.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(3) == null || mc.player.getInventory().getStack(3).isEmpty() || mc.player.getInventory().getStack(3) != bestBow)
                        && !(slot2.is("Bow") || slot3.is("Bow") || slot1.is("Bow") || slot5.is("Bow") || slot6.is("Bow") || slot7.is("Bow") || slot8.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(3) == null || mc.player.getInventory().getStack(3).isEmpty() || mc.player.getInventory().getStack(3) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot3.is("Pickaxe") || slot1.is("Pickaxe") || slot5.is("Pickaxe") || slot6.is("Pickaxe") || slot7.is("Pickaxe") || slot8.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(3) == null || mc.player.getInventory().getStack(3).isEmpty() || mc.player.getInventory().getStack(3) != bestAxe)
                        && !(slot2.is("Axe") || slot3.is("Axe") || slot1.is("Axe") || slot5.is("Axe") || slot6.is("Axe") || slot7.is("Axe") || slot8.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(3) == null || mc.player.getInventory().getStack(3).isEmpty() || mc.player.getInventory().getStack(3) != bestShovel)
                        && !(slot2.is("Shovel") || slot3.is("Shovel") || slot1.is("Shovel") || slot5.is("Shovel") || slot6.is("Shovel") || slot7.is("Shovel") || slot8.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(3) != 3 && getSlotWithMostFood(3) != -1 && !(isFood(mc.player.getInventory().getStack(3)) && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(3)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(3) != 3 && getSlotWithMostBlock(3) != -1 && !(mc.player.getInventory().getStack(3).getItem() instanceof BlockItem && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(3)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(3);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(3);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(3) != 3 && getSlotWithMostGapple(3) != -1 && !(mc.player.getInventory().getStack(3).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(3)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(3) != 3 && getSlotWithMostPearl(3) != -1 && !(mc.player.getInventory().getStack(3).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(3)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(3);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(3) != 3 && getSlotWithMostProjectile(3) != -1 && !((mc.player.getInventory().getStack(3).getItem() instanceof EggItem || mc.player.getInventory().getStack(3).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(3)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(3);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(3) != 3 && getSlotWithMostTNT(3) != -1 && !(mc.player.getInventory().getStack(3).getItem() == Items.TNT && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(3)).getCount())) return true;
                break;
        }
        switch (slot5.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(4) == null || mc.player.getInventory().getStack(4).isEmpty() || mc.player.getInventory().getStack(4) != bestSword)
                        && !(slot2.is("Sword") || slot3.is("Sword") || slot4.is("Sword") || slot1.is("Sword") || slot6.is("Sword") || slot7.is("Sword") || slot8.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(4) == null || mc.player.getInventory().getStack(4).isEmpty() || mc.player.getInventory().getStack(4) != bestBow)
                        && !(slot2.is("Bow") || slot3.is("Bow") || slot4.is("Bow") || slot1.is("Bow") || slot6.is("Bow") || slot7.is("Bow") || slot8.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(4) == null || mc.player.getInventory().getStack(4).isEmpty() || mc.player.getInventory().getStack(4) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot3.is("Pickaxe") || slot4.is("Pickaxe") || slot1.is("Pickaxe") || slot6.is("Pickaxe") || slot7.is("Pickaxe") || slot8.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(4) == null || mc.player.getInventory().getStack(4).isEmpty() || mc.player.getInventory().getStack(4) != bestAxe)
                        && !(slot2.is("Axe") || slot3.is("Axe") || slot4.is("Axe") || slot1.is("Axe") || slot6.is("Axe") || slot7.is("Axe") || slot8.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(4) == null || mc.player.getInventory().getStack(4).isEmpty() || mc.player.getInventory().getStack(4) != bestShovel)
                        && !(slot2.is("Shovel") || slot3.is("Shovel") || slot4.is("Shovel") || slot1.is("Shovel") || slot6.is("Shovel") || slot7.is("Shovel") || slot8.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(4) != 4 && getSlotWithMostFood(4) != -1 && !(isFood(mc.player.getInventory().getStack(4)) && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(4)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(4) != 4 && getSlotWithMostBlock(4) != -1 && !(mc.player.getInventory().getStack(4).getItem() instanceof BlockItem && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(4)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(4);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(4);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(4) != 4 && getSlotWithMostGapple(4) != -1 && !(mc.player.getInventory().getStack(4).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(4)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(4) != 4 && getSlotWithMostPearl(4) != -1 && !(mc.player.getInventory().getStack(4).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(4)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(4);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(4) != 4 && getSlotWithMostProjectile(4) != -1 && !((mc.player.getInventory().getStack(4).getItem() instanceof EggItem || mc.player.getInventory().getStack(4).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(4)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(4);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(4) != 4 && getSlotWithMostTNT(4) != -1 && !(mc.player.getInventory().getStack(4).getItem() == Items.TNT && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(4)).getCount())) return true;
                break;
        }
        switch (slot6.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(5) == null || mc.player.getInventory().getStack(5).isEmpty() || mc.player.getInventory().getStack(5) != bestSword)
                        && !(slot2.is("Sword") || slot3.is("Sword") || slot4.is("Sword") || slot5.is("Sword") || slot1.is("Sword") || slot7.is("Sword") || slot8.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(5) == null || mc.player.getInventory().getStack(5).isEmpty() || mc.player.getInventory().getStack(5) != bestBow)
                        && !(slot2.is("Bow") || slot3.is("Bow") || slot4.is("Bow") || slot5.is("Bow") || slot1.is("Bow") || slot7.is("Bow") || slot8.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(5) == null || mc.player.getInventory().getStack(5).isEmpty() || mc.player.getInventory().getStack(5) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot3.is("Pickaxe") || slot4.is("Pickaxe") || slot5.is("Pickaxe") || slot1.is("Pickaxe") || slot7.is("Pickaxe") || slot8.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(5) == null || mc.player.getInventory().getStack(5).isEmpty() || mc.player.getInventory().getStack(5) != bestAxe)
                        && !(slot2.is("Axe") || slot3.is("Axe") || slot4.is("Axe") || slot5.is("Axe") || slot1.is("Axe") || slot7.is("Axe") || slot8.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(5) == null || mc.player.getInventory().getStack(5).isEmpty() || mc.player.getInventory().getStack(5) != bestShovel)
                        && !(slot2.is("Shovel") || slot3.is("Shovel") || slot4.is("Shovel") || slot5.is("Shovel") || slot1.is("Shovel") || slot7.is("Shovel") || slot8.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(5) != 5 && getSlotWithMostFood(5) != -1 && !(isFood(mc.player.getInventory().getStack(5)) && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(5)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(5) != 5 && getSlotWithMostBlock(5) != -1 && !(mc.player.getInventory().getStack(5).getItem() instanceof BlockItem && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(5)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(5);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(5);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(5) != 5 && getSlotWithMostGapple(5) != -1 && !(mc.player.getInventory().getStack(5).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(5)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(5) != 5 && getSlotWithMostPearl(5) != -1 && !(mc.player.getInventory().getStack(5).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(5)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(5);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(5) != 5 && getSlotWithMostProjectile(5) != -1 && !((mc.player.getInventory().getStack(5).getItem() instanceof EggItem || mc.player.getInventory().getStack(5).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(5)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(5);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(5) != 5 && getSlotWithMostTNT(5) != -1 && !(mc.player.getInventory().getStack(5).getItem() == Items.TNT && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(5)).getCount())) return true;
                break;
        }
        switch (slot7.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(6) == null || mc.player.getInventory().getStack(6).isEmpty() || mc.player.getInventory().getStack(6) != bestSword)
                        && !(slot2.is("Sword") || slot3.is("Sword") || slot4.is("Sword") || slot5.is("Sword") || slot6.is("Sword") || slot1.is("Sword") || slot8.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(6) == null || mc.player.getInventory().getStack(6).isEmpty() || mc.player.getInventory().getStack(6) != bestBow)
                        && !(slot2.is("Bow") || slot3.is("Bow") || slot4.is("Bow") || slot5.is("Bow") || slot6.is("Bow") || slot1.is("Bow") || slot8.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(6) == null || mc.player.getInventory().getStack(6).isEmpty() || mc.player.getInventory().getStack(6) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot3.is("Pickaxe") || slot4.is("Pickaxe") || slot5.is("Pickaxe") || slot6.is("Pickaxe") || slot1.is("Pickaxe") || slot8.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(6) == null || mc.player.getInventory().getStack(6).isEmpty() || mc.player.getInventory().getStack(6) != bestAxe)
                        && !(slot2.is("Axe") || slot3.is("Axe") || slot4.is("Axe") || slot5.is("Axe") || slot6.is("Axe") || slot1.is("Axe") || slot8.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(6) == null || mc.player.getInventory().getStack(6).isEmpty() || mc.player.getInventory().getStack(6) != bestShovel)
                        && !(slot2.is("Shovel") || slot3.is("Shovel") || slot4.is("Shovel") || slot5.is("Shovel") || slot6.is("Shovel") || slot1.is("Shovel") || slot8.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(6) != 6 && getSlotWithMostFood(6) != -1 && !(isFood(mc.player.getInventory().getStack(6)) && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(6)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(6) != 6 && getSlotWithMostBlock(6) != -1 && !(mc.player.getInventory().getStack(6).getItem() instanceof BlockItem && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(6)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(6);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(6);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(6) != 6 && getSlotWithMostGapple(6) != -1 && !(mc.player.getInventory().getStack(6).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(6)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(6) != 6 && getSlotWithMostPearl(6) != -1 && !(mc.player.getInventory().getStack(6).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(6)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(6);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(6) != 6 && getSlotWithMostProjectile(6) != -1 && !((mc.player.getInventory().getStack(6).getItem() instanceof EggItem || mc.player.getInventory().getStack(6).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(6)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(6);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(6) != 6 && getSlotWithMostTNT(6) != -1 && !(mc.player.getInventory().getStack(6).getItem() == Items.TNT && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(6)).getCount())) return true;
                break;
        }
        switch (slot8.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(7) == null || mc.player.getInventory().getStack(7).isEmpty() || mc.player.getInventory().getStack(7) != bestSword)
                        && !(slot2.is("Sword") || slot3.is("Sword") || slot4.is("Sword") || slot5.is("Sword") || slot6.is("Sword") || slot7.is("Sword") || slot1.is("Sword") || slot9.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(7) == null || mc.player.getInventory().getStack(7).isEmpty() || mc.player.getInventory().getStack(7) != bestBow)
                        && !(slot2.is("Bow") || slot3.is("Bow") || slot4.is("Bow") || slot5.is("Bow") || slot6.is("Bow") || slot7.is("Bow") || slot1.is("Bow") || slot9.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(7) == null || mc.player.getInventory().getStack(7).isEmpty() || mc.player.getInventory().getStack(7) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot3.is("Pickaxe") || slot4.is("Pickaxe") || slot5.is("Pickaxe") || slot6.is("Pickaxe") || slot7.is("Pickaxe") || slot1.is("Pickaxe") || slot9.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(7) == null || mc.player.getInventory().getStack(7).isEmpty() || mc.player.getInventory().getStack(7) != bestAxe)
                        && !(slot2.is("Axe") || slot3.is("Axe") || slot4.is("Axe") || slot5.is("Axe") || slot6.is("Axe") || slot7.is("Axe") || slot1.is("Axe") || slot9.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(7) == null || mc.player.getInventory().getStack(7).isEmpty() || mc.player.getInventory().getStack(7) != bestShovel)
                        && !(slot2.is("Shovel") || slot3.is("Shovel") || slot4.is("Shovel") || slot5.is("Shovel") || slot6.is("Shovel") || slot7.is("Shovel") || slot1.is("Shovel") || slot9.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(7) != 7 && getSlotWithMostFood(7) != -1 && !(isFood(mc.player.getInventory().getStack(7)) && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(7)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(7) != 7 && getSlotWithMostBlock(7) != -1 && !(mc.player.getInventory().getStack(7).getItem() instanceof BlockItem && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(7)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(7);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(7);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(7) != 7 && getSlotWithMostGapple(7) != -1 && !(mc.player.getInventory().getStack(7).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(7)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(7) != 7 && getSlotWithMostPearl(7) != -1 && !(mc.player.getInventory().getStack(7).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(7)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(7);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(7) != 7 && getSlotWithMostProjectile(7) != -1 && !((mc.player.getInventory().getStack(7).getItem() instanceof EggItem || mc.player.getInventory().getStack(7).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(7)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(7);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(7) != 7 && getSlotWithMostTNT(7) != -1 && !(mc.player.getInventory().getStack(7).getItem() == Items.TNT && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(7)).getCount())) return true;
                break;
        }
        switch (slot9.get()) {
            case "Sword":
                ItemStack bestSword = InventoryUtils.getBestSword();
                if (bestSword != null
                        && !bestSword.isEmpty()
                        && (mc.player.getInventory().getStack(8) == null || mc.player.getInventory().getStack(8).isEmpty() || mc.player.getInventory().getStack(8) != bestSword)
                        && !(slot2.is("Sword") || slot3.is("Sword") || slot4.is("Sword") || slot5.is("Sword") || slot6.is("Sword") || slot7.is("Sword") || slot8.is("Sword") || slot1.is("Sword"))) {
                    return true;
                }
                break;
            case "Bow":
                ItemStack bestBow = InventoryUtils.getBestPowerBow();
                if (bestBow != null
                        && !bestBow.isEmpty()
                        && (mc.player.getInventory().getStack(8) == null || mc.player.getInventory().getStack(8).isEmpty() || mc.player.getInventory().getStack(8) != bestBow)
                        && !(slot2.is("Bow") || slot3.is("Bow") || slot4.is("Bow") || slot5.is("Bow") || slot6.is("Bow") || slot7.is("Bow") || slot8.is("Bow") || slot1.is("Bow"))) {
                    return true;
                }
                break;
            case "Pickaxe":
                ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
                if (bestPickaxe != null
                        && !bestPickaxe.isEmpty()
                        && (mc.player.getInventory().getStack(8) == null || mc.player.getInventory().getStack(8).isEmpty() || mc.player.getInventory().getStack(8) != bestPickaxe)
                        && !(slot2.is("Pickaxe") || slot3.is("Pickaxe") || slot4.is("Pickaxe") || slot5.is("Pickaxe") || slot6.is("Pickaxe") || slot7.is("Pickaxe") || slot8.is("Pickaxe") || slot1.is("Pickaxe"))) {
                    return true;
                }
                break;
            case "Axe":
                ItemStack bestAxe = InventoryUtils.getBestAxe();
                if (bestAxe != null
                        && !bestAxe.isEmpty()
                        && (mc.player.getInventory().getStack(8) == null || mc.player.getInventory().getStack(8).isEmpty() || mc.player.getInventory().getStack(8) != bestAxe)
                        && !(slot2.is("Axe") || slot3.is("Axe") || slot4.is("Axe") || slot5.is("Axe") || slot6.is("Axe") || slot7.is("Axe") || slot8.is("Axe") || slot1.is("Axe"))) {
                    return true;
                }
                break;
            case "Shovel":
                ItemStack bestShovel = InventoryUtils.getBestShovel();

                if (bestShovel != null
                        && !bestShovel.isEmpty()
                        && (mc.player.getInventory().getStack(8) == null || mc.player.getInventory().getStack(8).isEmpty() || mc.player.getInventory().getStack(8) != bestShovel)
                        && !(slot2.is("Shovel") || slot3.is("Shovel") || slot4.is("Shovel") || slot5.is("Shovel") || slot6.is("Shovel") || slot7.is("Shovel") || slot8.is("Shovel") || slot1.is("Shovel"))) {
                    return true;
                }
                break;
            case "Food":
                if (getSlotWithMostFood(8) != 8 && getSlotWithMostFood(8) != -1 && !(isFood(mc.player.getInventory().getStack(8)) && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(8)).getCount())) return true;
                break;
            case "Block":
                if (getSlotWithMostBlock(8) != 8 && getSlotWithMostBlock(8) != -1 && !(mc.player.getInventory().getStack(8).getItem() instanceof BlockItem && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(8)).getCount())) return true;
                break;
            case "Water":
                ItemStack hotbar0Water = mc.player.getInventory().getStack(8);
                boolean hotbarIsWater = hotbar0Water != null && !hotbar0Water.isEmpty() && hotbar0Water.getItem() == Items.WATER_BUCKET;

                int waterSlot = PlayerUtils.findAllItem(Items.WATER_BUCKET);

                if (!hotbarIsWater && waterSlot != -1) {
                    return true;
                }
                break;
            case "Lava":
                ItemStack hotbar0Lava = mc.player.getInventory().getStack(8);
                boolean hotbarIsLava = hotbar0Lava != null && !hotbar0Lava.isEmpty() && hotbar0Lava.getItem() == Items.LAVA_BUCKET;

                int lavaSlot = PlayerUtils.findAllItem(Items.LAVA_BUCKET);

                if (!hotbarIsLava && lavaSlot != -1) {
                    return true;
                }
                break;
            case "Gapple":
                if (getSlotWithMostGapple(8) != 8 && getSlotWithMostGapple(8) != -1 && !(mc.player.getInventory().getStack(8).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(8)).getCount())) return true;
                break;
            case "Pearl":
                if (getSlotWithMostPearl(8) != 8 && getSlotWithMostPearl(8) != -1 && !(mc.player.getInventory().getStack(8).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(8)).getCount())) return true;
                break;
            case "Potion":
                ItemStack hotbar0 = mc.player.getInventory().getStack(8);
                boolean hotbarIsPotion =
                        !hotbar0.isEmpty() && hotbar0.getItem() instanceof PotionItem;

                int potionSlot = -1;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof PotionItem) {
                        potionSlot = i;
                        break;
                    }
                }

                if (!hotbarIsPotion && potionSlot != -1) {
                    return true;
                }

                break;
            case "Projectile":
                if (getSlotWithMostProjectile(8) != 8 && getSlotWithMostProjectile(8) != -1 && !((mc.player.getInventory().getStack(8).getItem() instanceof EggItem || mc.player.getInventory().getStack(8).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(8)).getCount())) return true;
                break;
            case "FishingRod":
                ItemStack hotbar0Rod = mc.player.getInventory().getStack(8);
                boolean hotbarIsRod = hotbar0Rod != null && !hotbar0Rod.isEmpty() && hotbar0Rod.getItem() == Items.FISHING_ROD;

                int rodSlot = PlayerUtils.findAllItem(Items.FISHING_ROD);

                if (!hotbarIsRod && rodSlot != -1) {
                    return true;
                }

                break;
            case "TNT":
                if (getSlotWithMostTNT(8) != 8 && getSlotWithMostTNT(8) != -1 && !(mc.player.getInventory().getStack(8).getItem() == Items.TNT && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(8)).getCount())) return true;
                break;
        }
        return false;
    }

    public static boolean undressed() {
        if (mc.player == null) return false;
        for (int slotID = 0; slotID < mc.player.getInventory().size(); slotID++) {
            int currentSlot = slots.get(slotID);

            ItemStack itemStack = mc.player.getInventory().getStack(currentSlot);

            if (itemStack == null) continue;
            if (itemStack.isEmpty()) continue;

            if (ArmorUtils.armorHelmets.contains(itemStack.getItem())) {
                if (mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY).isEmpty() || mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY) == null) return true;
            } else if (ArmorUtils.armorChestPlates.contains(itemStack.getItem())) {
                if (mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY).isEmpty() || mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY) == null) return true;
            } else if (ArmorUtils.armorLeggings.contains(itemStack.getItem())) {
                if (mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY).isEmpty() || mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY) == null) return true;
            } else if (ArmorUtils.armorBoots.contains(itemStack.getItem())) {
                if (mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY).isEmpty() || mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY) == null) return true;
            }
        }

        var bestHelmetScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY)));
        var helmetScore = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.HELMET_SLOT_INVENTORY));
        var isBestHelmet = bestHelmetScore == helmetScore;

        if (!isBestHelmet) return true;

        var bestChestPlateScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY)));
        var chestPlateScore = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.CHEST_PLATE_SLOT_INVENTORY));
        var isBestChestPlate = bestChestPlateScore == chestPlateScore;

        if (!isBestChestPlate) return true;

        var bestLeggingsScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY)));
        var leggingsScore = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.LEGGINGS_SLOT_INVENTORY));
        var isBestLeggings = bestLeggingsScore == leggingsScore;

        if (!isBestLeggings) return true;

        var bestBootsScore = InventoryUtils.getBestArmorScore(ItemUtils.getEquipmentType(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY)));
        var bootsScore = InventoryUtils.getProtection(mc.player.getInventory().getStack(InventoryUtils.BOOTS_SLOT_INVENTORY));
        var isBestBoots = bestBootsScore == bootsScore;

        return !isBestBoots;
    }

    public static void autoSort() {

        if (mc.player == null || mc.interactionManager == null) return;

        int delay = sortDelay.get().intValue() * 10;

        for (int slotID = 0; slotID < mc.player.getInventory().size(); slotID++) {
            int currentSlot = slots.get(slotID);

            ItemStack itemStack = mc.player.getInventory().getStack(currentSlot);

            if (itemStack == null) continue;
            if (itemStack.isEmpty()) continue;

            switch (slot1.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(0) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 3 && slot4.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(0) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(0) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(0) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(0) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(0)) {
                        if (getSlotWithMostFood(0) != 0 && !(isFood(mc.player.getInventory().getStack(0)) && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(0)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(0).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(0)) {
                        if (getSlotWithMostBlock(0) != 0 && !(mc.player.getInventory().getStack(0).getItem() instanceof BlockItem && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(0)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(0).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(0)) {
                        if (getSlotWithMostGapple(0) != 0 && !(mc.player.getInventory().getStack(0).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(0)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(0)) {
                        if (getSlotWithMostPearl(0) != 0 && !(mc.player.getInventory().getStack(0).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(0)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(0).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(0)) {
                        if (getSlotWithMostProjectile(0) != 0 && !((mc.player.getInventory().getStack(0).getItem() instanceof EggItem || mc.player.getInventory().getStack(0).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(0)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(0).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(0)) {
                        if (getSlotWithMostTNT(0) != 0 && !(mc.player.getInventory().getStack(0).getItem() == Items.TNT && mc.player.getInventory().getStack(0).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(0)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 0, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
            }

            switch (slot2.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 0 && slot1.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(1) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 0 && slot1.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 3 && slot4.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(1) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 0 && slot1.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(1) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 0 && slot1.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(1) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 0 && slot1.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(1) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(1)) {
                        if (getSlotWithMostFood(1) != 1&& !(isFood(mc.player.getInventory().getStack(1)) && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(1)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(1)) {
                        if (getSlotWithMostBlock(1) != 1 && !(mc.player.getInventory().getStack(1).getItem() instanceof BlockItem && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(1)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 0 && slot1.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(1).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 0 && slot1.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(1).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(1)) {
                        if (getSlotWithMostGapple(1) != 1 && !(mc.player.getInventory().getStack(1).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(1)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(1)) {
                        if (getSlotWithMostPearl(1) != 1 && !(mc.player.getInventory().getStack(1).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(1)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 0 && slot1.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(1).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(1)) {
                        if (getSlotWithMostProjectile(1) != 1 && !((mc.player.getInventory().getStack(1).getItem() instanceof EggItem || mc.player.getInventory().getStack(1).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(1)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 0 && slot1.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(1).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(1)) {
                        if (getSlotWithMostTNT(1) != 1 && !(mc.player.getInventory().getStack(1).getItem() == Items.TNT && mc.player.getInventory().getStack(1).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(1)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 1, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }

            switch (slot3.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 0 && slot1.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(2) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 0 && slot1.is("Bow") || currentSlot == 3 && slot4.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(2) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 0 && slot1.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(2) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 0 && slot1.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(2) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 0 && slot1.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(2) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(2)) {
                        if (getSlotWithMostFood(2) != 2&& !(isFood(mc.player.getInventory().getStack(2)) && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(2)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(2)) {
                        if (getSlotWithMostBlock(2) != 2 && !(mc.player.getInventory().getStack(2).getItem() instanceof BlockItem && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(2)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 0 && slot1.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(2).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 0 && slot1.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(2).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(2)) {
                        if (getSlotWithMostGapple(2) != 2 && !(mc.player.getInventory().getStack(2).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(2)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(2)) {
                        if (getSlotWithMostPearl(2) != 2 && !(mc.player.getInventory().getStack(2).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(2)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 0 && slot1.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(2).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(2)) {
                        if (getSlotWithMostProjectile(2) != 2 && !((mc.player.getInventory().getStack(2).getItem() instanceof EggItem || mc.player.getInventory().getStack(2).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(2)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 0 && slot1.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(2).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(2)) {
                        if (getSlotWithMostTNT(2) != 2 && !(mc.player.getInventory().getStack(2).getItem() == Items.TNT && mc.player.getInventory().getStack(2).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(2)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 2, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }
            switch (slot4.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 0 && slot1.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(3) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 0 && slot1.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(3) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 0 && slot1.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(3) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 0 && slot1.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(3) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 0 && slot1.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(3) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(3)) {
                        if (getSlotWithMostFood(3) != 3&& !(isFood(mc.player.getInventory().getStack(3)) && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(3)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(3)) {
                        if (getSlotWithMostBlock(3) != 3 && !(mc.player.getInventory().getStack(3).getItem() instanceof BlockItem && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(3)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 0 && slot1.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(3).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 0 && slot1.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(3).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(3)) {
                        if (getSlotWithMostGapple(3) != 3 && !(mc.player.getInventory().getStack(3).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(3)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(3)) {
                        if (getSlotWithMostPearl(3) != 3 && !(mc.player.getInventory().getStack(3).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(3)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 0 && slot1.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(3).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(3)) {
                        if (getSlotWithMostProjectile(3) != 3 && !((mc.player.getInventory().getStack(3).getItem() instanceof EggItem || mc.player.getInventory().getStack(3).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(3)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 0 && slot1.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(3).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(3)) {
                        if (getSlotWithMostTNT(3) != 3 && !(mc.player.getInventory().getStack(3).getItem() == Items.TNT && mc.player.getInventory().getStack(3).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(3)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 3, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }
            switch (slot5.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 0 && slot1.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(4) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 0 && slot1.is("Bow") ||
                                currentSlot == 0 && slot1.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(4) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 0 && slot1.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(4) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 0 && slot1.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(4) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 0 && slot1.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(4) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(4)) {
                        if (getSlotWithMostFood(4) != 4&& !(isFood(mc.player.getInventory().getStack(4)) && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(4)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(4)) {
                        if (getSlotWithMostBlock(4) != 4 && !(mc.player.getInventory().getStack(4).getItem() instanceof BlockItem && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(4)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 0 && slot1.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(4).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 0 && slot1.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(4).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(4)) {
                        if (getSlotWithMostGapple(4) != 4 && !(mc.player.getInventory().getStack(4).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(4)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(4)) {
                        if (getSlotWithMostPearl(4) != 4 && !(mc.player.getInventory().getStack(4).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(4)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 0 && slot1.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(4).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(4)) {
                        if (getSlotWithMostProjectile(4) != 4 && !((mc.player.getInventory().getStack(4).getItem() instanceof EggItem || mc.player.getInventory().getStack(4).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(4)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 0 && slot1.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(4).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(4)) {
                        if (getSlotWithMostTNT(4) != 4 && !(mc.player.getInventory().getStack(4).getItem() == Items.TNT && mc.player.getInventory().getStack(4).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(4)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 4, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }
            switch (slot6.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 0 && slot1.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(5) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 3 && slot4.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 0 && slot1.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(5) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 0 && slot1.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(5) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 0 && slot1.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(5) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 0 && slot1.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(5) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(5)) {
                        if (getSlotWithMostFood(5) != 5&& !(isFood(mc.player.getInventory().getStack(5)) && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(5)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(5)) {
                        if (getSlotWithMostBlock(5) != 5 && !(mc.player.getInventory().getStack(5).getItem() instanceof BlockItem && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(5)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 0 && slot1.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(5).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 0 && slot1.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(5).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(5)) {
                        if (getSlotWithMostGapple(5) != 5 && !(mc.player.getInventory().getStack(5).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(5)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(5)) {
                        if (getSlotWithMostPearl(5) != 5 && !(mc.player.getInventory().getStack(5).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(5)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 0 && slot1.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(5).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(5)) {
                        if (getSlotWithMostProjectile(5) != 5 && !((mc.player.getInventory().getStack(5).getItem() instanceof EggItem || mc.player.getInventory().getStack(5).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(5) == mc.player.getInventory().getStack(getSlotWithMostProjectile(5)))) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 0 && slot1.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(5).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(5)) {
                        if (getSlotWithMostTNT(5) != 5 && !(mc.player.getInventory().getStack(5).getItem() == Items.TNT && mc.player.getInventory().getStack(5).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(5)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 5, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }
            switch (slot7.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 0 && slot1.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(6) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 3 && slot4.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 0 && slot1.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(6) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 0 && slot1.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(6) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 0 && slot1.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(6) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 0 && slot1.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(6) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(6)) {
                        if (getSlotWithMostFood(6) != 6&& !(isFood(mc.player.getInventory().getStack(6)) && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(6)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(6)) {
                        if (getSlotWithMostBlock(6) != 6 && !(mc.player.getInventory().getStack(6).getItem() instanceof BlockItem && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(6)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 0 && slot1.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(6).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 0 && slot1.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(6).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(6)) {
                        if (getSlotWithMostGapple(6) != 6 && !(mc.player.getInventory().getStack(6).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(6)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(6)) {
                        if (getSlotWithMostPearl(6) != 6 && !(mc.player.getInventory().getStack(6).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(6)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 0 && slot1.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(6).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(6)) {
                        if (getSlotWithMostProjectile(6) != 6 && !((mc.player.getInventory().getStack(6).getItem() instanceof EggItem || mc.player.getInventory().getStack(6).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(6)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 0 && slot1.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(6).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(6)) {
                        if (getSlotWithMostTNT(6) != 6 && !(mc.player.getInventory().getStack(6).getItem() == Items.TNT && mc.player.getInventory().getStack(6).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(6)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 6, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }
            switch (slot8.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 0 && slot1.is("Sword") || currentSlot == 8 && slot9.is("Sword"))) {
                            if (mc.player.getInventory().getStack(7) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 3 && slot4.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 0 && slot1.is("Bow") || currentSlot == 8 && slot9.is("Bow"))) {
                            if (mc.player.getInventory().getStack(7) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 0 && slot1.is("Pickaxe") || currentSlot == 8 && slot9.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(7) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 0 && slot1.is("Axe") || currentSlot == 8 && slot9.is("Axe"))) {
                            if (mc.player.getInventory().getStack(7) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 0 && slot1.is("Shovel") || currentSlot == 8 && slot9.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(7) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(7)) {
                        if (getSlotWithMostFood(7) != 7&& !(isFood(mc.player.getInventory().getStack(7)) && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(7)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(7)) {
                        if (getSlotWithMostBlock(7) != 7 && !(mc.player.getInventory().getStack(7).getItem() instanceof BlockItem && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(7)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 0 && slot1.is("Water") || currentSlot == 8 && slot9.is("Water"))) {
                            if (mc.player.getInventory().getStack(7).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 0 && slot1.is("Lava") || currentSlot == 8 && slot9.is("Lava"))) {
                            if (mc.player.getInventory().getStack(7).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(7)) {
                        if (getSlotWithMostGapple(7) != 7 && !(mc.player.getInventory().getStack(7).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(7)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(7)) {
                        if (getSlotWithMostPearl(7) != 7 && !(mc.player.getInventory().getStack(7).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(7)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 0 && slot1.is("Potion") || currentSlot == 8 && slot9.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(7).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(7)) {
                        if (getSlotWithMostProjectile(7) != 7 && !((mc.player.getInventory().getStack(7).getItem() instanceof EggItem || mc.player.getInventory().getStack(7).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(7)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 0 && slot1.is("FishingRod") || currentSlot == 8 && slot9.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(7).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(7)) {
                        if (getSlotWithMostTNT(7) != 7 && !(mc.player.getInventory().getStack(7).getItem() == Items.TNT && mc.player.getInventory().getStack(7).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(7)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 7, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }
            switch (slot9.get()) {
                case "None":
                    break;
                case "Sword":
                    if (itemStack.getItem() instanceof SwordItem && itemStack == InventoryUtils.getBestSword()) {
                        if (!(currentSlot == 1 && slot2.is("Sword") || currentSlot == 2 && slot3.is("Sword") ||
                                currentSlot == 3 && slot4.is("Sword") || currentSlot == 4 && slot5.is("Sword") ||
                                currentSlot == 5 && slot6.is("Sword") || currentSlot == 6 && slot7.is("Sword") || currentSlot == 7 && slot8.is("Sword") || currentSlot == 0 && slot1.is("Sword"))) {
                            if (mc.player.getInventory().getStack(8) != InventoryUtils.getBestSword()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Bow":
                    if (itemStack.getItem() instanceof BowItem && itemStack == InventoryUtils.getBestPowerBow()) {
                        if (!(currentSlot == 1 && slot2.is("Bow") || currentSlot == 2 && slot3.is("Bow") || currentSlot == 3 && slot4.is("Bow") ||
                                currentSlot == 4 && slot5.is("Bow") || currentSlot == 5 && slot6.is("Bow") || currentSlot == 6 && slot7.is("Bow") || currentSlot == 7 && slot8.is("Bow") || currentSlot == 0 && slot1.is("Bow"))) {
                            if (mc.player.getInventory().getStack(8) != InventoryUtils.getBestPowerBow()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Pickaxe":
                    if (itemStack.getItem() instanceof PickaxeItem && itemStack == InventoryUtils.getBestPickaxe()) {
                        if (!(currentSlot == 1 && slot2.is("Pickaxe") || currentSlot == 2 && slot3.is("Pickaxe") ||
                                currentSlot == 3 && slot4.is("Pickaxe") || currentSlot == 4 && slot5.is("Pickaxe") ||
                                currentSlot == 5 && slot6.is("Pickaxe") || currentSlot == 6 && slot7.is("Pickaxe") || currentSlot == 7 && slot8.is("Pickaxe") || currentSlot == 0 && slot1.is("Pickaxe"))) {
                            if (mc.player.getInventory().getStack(8) != InventoryUtils.getBestPickaxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Axe":
                    if (itemStack.getItem() instanceof AxeItem && itemStack == InventoryUtils.getBestAxe()) {
                        if (!(currentSlot == 1 && slot2.is("Axe") || currentSlot == 2 && slot3.is("Axe") ||
                                currentSlot == 3 && slot4.is("Axe") || currentSlot == 4 && slot5.is("Axe") ||
                                currentSlot == 5 && slot6.is("Axe") || currentSlot == 6 && slot7.is("Axe") || currentSlot == 7 && slot8.is("Axe") || currentSlot == 0 && slot1.is("Axe"))) {
                            if (mc.player.getInventory().getStack(8) != InventoryUtils.getBestAxe()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Shovel":
                    if (itemStack.getItem() instanceof ShovelItem && itemStack == InventoryUtils.getBestShovel()) {
                        if (!(currentSlot == 1 && slot2.is("Shovel") || currentSlot == 2 && slot3.is("Shovel") ||
                                currentSlot == 3 && slot4.is("Shovel") || currentSlot == 4 && slot5.is("Shovel") ||
                                currentSlot == 5 && slot6.is("Shovel") || currentSlot == 6 && slot7.is("Shovel") || currentSlot == 7 && slot8.is("Shovel") || currentSlot == 0 && slot1.is("Shovel"))) {
                            if (mc.player.getInventory().getStack(8) != InventoryUtils.getBestShovel()) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Food":
                    if (currentSlot == getSlotWithMostFood(8)) {
                        if (getSlotWithMostFood(8) != 8&& !(isFood(mc.player.getInventory().getStack(8)) && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostFood(8)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Block":
                    if (currentSlot == getSlotWithMostBlock(8)) {
                        if (getSlotWithMostBlock(8) != 8 && !(mc.player.getInventory().getStack(8).getItem() instanceof BlockItem && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostBlock(8)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;
                case "Water":
                    if (itemStack.getItem() == Items.WATER_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Water") || currentSlot == 2 && slot3.is("Water") ||
                                currentSlot == 3 && slot4.is("Water") || currentSlot == 4 && slot5.is("Water") ||
                                currentSlot == 5 && slot6.is("Water") || currentSlot == 6 && slot7.is("Water") || currentSlot == 7 && slot8.is("Water") || currentSlot == 0 && slot1.is("Water"))) {
                            if (mc.player.getInventory().getStack(8).getItem() != Items.WATER_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Lava":
                    if (itemStack.getItem() == Items.LAVA_BUCKET) {
                        if (!(currentSlot == 1 && slot2.is("Lava") || currentSlot == 2 && slot3.is("Lava") ||
                                currentSlot == 3 && slot4.is("Lava") || currentSlot == 4 && slot5.is("Lava") ||
                                currentSlot == 5 && slot6.is("Lava") || currentSlot == 6 && slot7.is("Lava") || currentSlot == 7 && slot8.is("Lava") || currentSlot == 0 && slot1.is("Lava"))) {
                            if (mc.player.getInventory().getStack(8).getItem() != Items.LAVA_BUCKET) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Gapple":
                    if (currentSlot == getSlotWithMostGapple(8)) {
                        if (getSlotWithMostGapple(8) != 8 && !(mc.player.getInventory().getStack(8).getItem() == Items.GOLDEN_APPLE && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostGapple(8)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Pearl":
                    if (currentSlot == getSlotWithMostPearl(8)) {
                        if (getSlotWithMostPearl(8) != 8 && !(mc.player.getInventory().getStack(8).getItem() == Items.ENDER_PEARL && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostPearl(8)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "Potion":
                    if (itemStack.getItem() instanceof PotionItem) {
                        if (!(currentSlot == 1 && slot2.is("Potion") || currentSlot == 2 && slot3.is("Potion") ||
                                currentSlot == 3 && slot4.is("Potion") || currentSlot == 4 && slot5.is("Potion") ||
                                currentSlot == 5 && slot6.is("Potion") || currentSlot == 6 && slot7.is("Potion") || currentSlot == 7 && slot8.is("Potion") || currentSlot == 0 && slot1.is("Potion"))) {
                            if (!(mc.player.getInventory().getStack(8).getItem() instanceof PotionItem)) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "Projectile":
                    if (currentSlot == getSlotWithMostProjectile(8)) {
                        if (getSlotWithMostProjectile(8) != 8 && !((mc.player.getInventory().getStack(8).getItem() instanceof EggItem || mc.player.getInventory().getStack(8).getItem() instanceof SnowballItem) && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostProjectile(8)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

                case "FishingRod":
                    if (itemStack.getItem() == Items.FISHING_ROD) {
                        if (!(currentSlot == 1 && slot2.is("FishingRod") || currentSlot == 2 && slot3.is("FishingRod") ||
                                currentSlot == 3 && slot4.is("FishingRod") || currentSlot == 4 && slot5.is("FishingRod") ||
                                currentSlot == 5 && slot6.is("FishingRod") || currentSlot == 6 && slot7.is("FishingRod") || currentSlot == 7 && slot8.is("FishingRod") || currentSlot == 0 && slot1.is("FishingRod"))) {
                            if (mc.player.getInventory().getStack(8).getItem() != Items.FISHING_ROD) {
                                if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                    int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                    sortTimer.reset();
                                }
                            }
                        }
                    }
                    break;
                case "TNT":
                    if (currentSlot == getSlotWithMostTNT(8)) {
                        if (getSlotWithMostTNT(8) != 8 && !(mc.player.getInventory().getStack(8).getItem() == Items.TNT && mc.player.getInventory().getStack(8).getCount() == mc.player.getInventory().getStack(getSlotWithMostTNT(8)).getCount())) {
                            if (sortTimer.hasTimeElapsed(delay) || delay == 0) {
                                int trueID = currentSlot <= 8 ? currentSlot + 36 : currentSlot;
                                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, trueID, 8, SlotActionType.SWAP, mc.player);
                                sortTimer.reset();
                            }
                        }
                    }
                    break;

            }
        }
    }

    public static boolean isDuplicateBestBow() {
        if (mc.player == null) return false;

        ItemStack best = InventoryUtils.getBestPowerBow();
        if (best == null) return false;

        var inventory = mc.player.getInventory();
        int found = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack s = inventory.getStack(i);
            if (s.isEmpty()) continue;

            if (!(s.getItem() instanceof BowItem)) continue;

            if (s == best) {
                found++;
                if (found > 1)
                    return true;
            }
        }
        return false;
    }
}
