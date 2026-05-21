package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.AttackEvent;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.player.AutoTool;
import cn.lazymoon.features.module.impl.world.BedBreaker;
import cn.lazymoon.features.module.impl.world.utils.ItemSpoofUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.utils.pack.PacketUtils;
import cn.lazymoon.utils.player.InventoryUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;

import static net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.ATTACK;

/**
 * @Author:XiaoyueChen
 * @CreateTime:2026-03-27
 */
@ModuleInfo(name = "AutoWeapon",description = "Switch to a sword when helping you attack a target",key = 0,category = Category.Combat,hidden = false)
public class AutoWeapon extends Module {
    public static BoolValue onlySword = new BoolValue("Only Sword", true);

    private boolean attackEnemy = false;
    public static LinkedBlockingDeque<UpdateSelectedSlotC2SPacket> packets = new LinkedBlockingDeque<>();

    private record WeaponSlot(int slot, float damage) {
    }

    @SuppressWarnings("unused")
    @EventTarget
    public void onAttack(AttackEvent event) {
        attackEnemy = true;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.isReceive() || mc.player == null) return;

        if (event.packet instanceof PlayerInteractEntityC2SPacket packet && packet.type == ATTACK && attackEnemy) {
            attackEnemy = false;

            WeaponSlot bestWeapon = findBestWeapon();
            if (bestWeapon == null) {
                return;
            }

            int currentSlot = mc.player.getInventory().selectedSlot;
            float currentDamage = InventoryUtils.getAttackDamage(mc.player.getInventory().getStack(currentSlot));

            if (currentDamage >= bestWeapon.damage) {
                return;
            }

            int slot = bestWeapon.slot;
            if (slot == currentSlot || (Client.INSTANCE.getModuleManager().getModule(BedBreaker.class).isState() && BedBreaker.breakingBlockPos != null && slot == ItemSpoofUtils.originalSlot && Client.INSTANCE.getModuleManager().getModule(AutoTool.class).isState() && AutoTool.spoof.get())) {
                return;
            }

            if (Client.INSTANCE.getModuleManager().getModule(BedBreaker.class).isState() && BedBreaker.breakingBlockPos != null && Client.INSTANCE.getModuleManager().getModule(AutoTool.class).isState() && AutoTool.spoof.get()) {
                ItemSpoofUtils.originalSlot = slot;
            } else {
                mc.player.getInventory().selectedSlot = slot;
            }
            PacketUtils.sendPacketNoEvent(packet);
            event.setCancelled(true);
        }
    }

    private WeaponSlot findBestWeapon() {
        WeaponSlot bestWeapon = null;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = Objects.requireNonNull(mc.player).getInventory().getStack(i);

            if (onlySword.getValue() && !(stack.getItem() instanceof SwordItem)) {
                continue;
            }

            float damage = InventoryUtils.getAttackDamage(stack);

            if (bestWeapon == null || damage > bestWeapon.damage) {
                bestWeapon = new WeaponSlot(i, damage);
            }
        }

        return bestWeapon;
    }
}
