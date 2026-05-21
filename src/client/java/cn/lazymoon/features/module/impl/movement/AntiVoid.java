package cn.lazymoon.features.module.impl.movement;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.api.event.CancellableEvent;
import cn.lazymoon.event.impl.input.MoveInputEvent;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.event.impl.world.TickEvent;
import cn.lazymoon.event.impl.world.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.module.impl.world.utils.PlaceInfo;
import cn.lazymoon.features.module.impl.world.utils.ScaffoldUtils;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.MultiBoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.utils.client.ClientData;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.entity.Rotation;
import cn.lazymoon.utils.pack.PacketUtils;
import cn.lazymoon.utils.rotation.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(name = "AntiVoid",description = "Prevent you from falling into the void",key = 0,category = Category.Movement,hidden = false)
public class AntiVoid extends Module {
    private final NumberValue fallDistanceLimit = new NumberValue("Fall Distance Limit", 3, 1, 20,1);
    private final BoolValue limitDuration = new BoolValue("Limit Duration", false);
    private final NumberValue maxDuration = (NumberValue) new NumberValue("Max Duration",()->limitDuration.get(), 100, 0, 1200, 10);
    private final BoolValue autoResume = (BoolValue) new BoolValue("Auto Resume",()->limitDuration.get(), false);
    private final NumberValue cooldown = (NumberValue) new NumberValue("Cooldown",()->limitDuration.get(), 20, 0, 40, 1);
    public static BoolValue checkClutch = new BoolValue("Check for Clutch",false);
    private final BoolValue pearlOnly = new BoolValue("Pearl Only", false);
    private final BoolValue usePearl = new BoolValue("Use Pearl", true);

    private final Queue<CommonPongC2SPacket> pendingPongs = new ConcurrentLinkedQueue<>();
    private final LinkedList<Runnable> delayedActions = new LinkedList<>();
    private boolean isDisabled;
    private Rotation previousView;
    private boolean viewChanged;
    private boolean isFrozen;
    private int freezeCounter;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        isDisabled = false;
        if (autoResume.get()) {
            while (!delayedActions.isEmpty()) {
                delayedActions.poll().run();
            }
        }

        if (usePearl.get()) {
            while (!pendingPongs.isEmpty()) {
                PacketUtils.sendPacketNoEvent(pendingPongs.poll());
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost()) return;
        if (ClientUtils.isNull()) {
            return;
        }
        if (!isDisabled && isFallingIntoVoid() && ClientData.getFallDistance() > fallDistanceLimit.get() &&
                (!pearlOnly.get() || (pearlOnly.get() && hasEnderPearl())) && (!checkClutch.get() || (canPlace() || hasEnderPearl()))) {
            if (!delayedActions.isEmpty()) {
                for (var action : delayedActions) {
                    action.run();
                }
                delayedActions.clear();
            }

            viewChanged = false;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (mc.player.onGround) isDisabled = false;

        if (!isDisabled && isFallingIntoVoid() && ClientData.getFallDistance() > fallDistanceLimit.get() &&
                (!pearlOnly.get() || (pearlOnly.get() && hasEnderPearl())) && (!checkClutch.get() || (canPlace() || hasEnderPearl()))) {

            if (usePearl.get() && hasEnderPearl()) {
                ItemStack currentItem = mc.player.getMainHandStack();
                if (currentItem == null || !(currentItem.getItem() instanceof EnderPearlItem)) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (stack != null && stack.getItem() instanceof EnderPearlItem) {
                            mc.player.getInventory().selectedSlot = i;
                            break;
                        }
                    }
                }
            }

            if (isFrozen) {
                mc.player.setVelocity(new Vec3d(0, 0, 0));
            }

            if (isFrozen || freezeCounter < 0) {
                ++freezeCounter;
            }

            if (limitDuration.getValue() && (freezeCounter < 0 || freezeCounter >= maxDuration.getValue())) {
                isFrozen = false;

                if (autoResume.getValue()) {
                    if (freezeCounter > 0) {
                        freezeCounter = -cooldown.getValue().intValue();
                    }
                } else {
                    isDisabled = true;
                }
            }
        } else {
            isFrozen = false;
            reset();
            if (autoResume.get()) {
                while (!delayedActions.isEmpty()) {
                    delayedActions.poll().run();
                }
            }

            if (usePearl.get()) {
                while (!pendingPongs.isEmpty()) {
                    PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (isFrozen) {
            event.strafe = 0;
            event.forward = 0;
            event.sneak = false;
            event.jump = false;
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        reset();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;

        if (!isDisabled && isFallingIntoVoid() && ClientData.getFallDistance() > fallDistanceLimit.get() &&
                (!pearlOnly.get() || (pearlOnly.get() && hasEnderPearl())) && (!checkClutch.get() || (canPlace() || hasEnderPearl()))) {
            if (event.isSend()) {
                if (!isFrozen && freezeCounter == 0) {
                    if (event.getPacket() instanceof PlayerMoveC2SPacket packet) {
                        if (packet.changesPosition()) {
                            isFrozen = true;
                        }

                        if (packet.changesLook()) {
                            previousView = new Rotation(packet.yaw, packet.pitch);
                        }
                    }
                } else {
                    if (event.getPacket() instanceof PlayerMoveC2SPacket) {
                        event.setCancelled(true);
                    } else if (event.getPacket() instanceof CommonPongC2SPacket packet && usePearl.get()) {
                        pendingPongs.offer(packet);
                        event.setCancelled(true);
                    } else if ((event.getPacket() instanceof PlayerActionC2SPacket packet && packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM && (mc.player.getActiveItem().getItem() instanceof BowItem || mc.player.getActiveItem().getItem() instanceof CrossbowItem)) || event.getPacket() instanceof PlayerInteractItemC2SPacket || event.getPacket() instanceof PlayerInteractBlockC2SPacket) {
                        if (previousView == null || !previousView.equals(RotationUtils.getRotationOrElseMC())) {
                            var rotation = RotationUtils.getRotationOrElseMC();
                            PacketUtils.sendPacketNoEvent(new PlayerMoveC2SPacket.LookAndOnGround(rotation.getYaw(), rotation.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));

                            previousView = rotation;
                            viewChanged = true;
                            freezeCounter = 0;

                            if (usePearl.get()) {
                                if (autoResume.get()) {
                                    delayedActions.add(() -> {
                                        while (!pendingPongs.isEmpty()) {
                                            PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                                        }
                                    });
                                } else {
                                    while (!pendingPongs.isEmpty()) {
                                        PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                                    }
                                }
                            }
                        }

                        if (autoResume.get() && viewChanged) {
                            delayedActions.add(() -> PacketUtils.sendPacketNoEvent(event.getPacket()));
                        } else {
                            PacketUtils.sendPacketNoEvent(event.getPacket());
                        }

                        event.setCancelled(true);
                    } else if (event.getPacket() instanceof HandSwingC2SPacket) {
                        if (autoResume.get() && viewChanged) {
                            delayedActions.add(() -> PacketUtils.sendPacketNoEvent(event.getPacket()));
                            event.setCancelled(true);
                        }
                    }
                }
            } else if (isFrozen) {
                if (event.getPacket() instanceof EntityDamageS2CPacket packet && packet.entityId() == mc.player.getId()) {
                    if (autoResume.get()) {
                        reset();
                        isFrozen = false;
                        isDisabled = true;
                        while (!delayedActions.isEmpty()) {
                            delayedActions.poll().run();
                        }
                    }

                    if (usePearl.get()) {
                        while (!pendingPongs.isEmpty()) {
                            PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                        }
                    }
                } else if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
                    if (autoResume.get()) {
                        reset();
                        isFrozen = false;
                        isDisabled = true;
                        while (!delayedActions.isEmpty()) {
                            delayedActions.poll().run();
                        }
                    }

                    if (usePearl.get()) {
                        while (!pendingPongs.isEmpty()) {
                            PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                        }
                    }
                }
            }
        } else {
            isFrozen = false;
            if (autoResume.get()) {
                while (!delayedActions.isEmpty()) {
                    delayedActions.poll().run();
                }
            }

            if (usePearl.get()) {
                while (!pendingPongs.isEmpty()) {
                    PacketUtils.sendPacketNoEvent(pendingPongs.poll());
                }
            }
        }
    }

    private void reset() {
        delayedActions.clear();
        pendingPongs.clear();
        previousView = null;
        freezeCounter = 0;
        isFrozen = false;
    }

    public static boolean canPlace() {
        if (mc.player == null) return false;

        int x = MathHelper.floor(mc.player.getX());
        int z = MathHelper.floor(mc.player.getZ());
        int startY = MathHelper.floor(mc.player.getY() - 1);

        ScaffoldUtils.SearchMode mode = ScaffoldUtils.SearchMode.Hypixel;

        BlockPos pos = new BlockPos(x, startY, z);

        PlaceInfo info = ScaffoldUtils.getPlaceInfo(pos, mode);
        return info != null;
    }

    private boolean hasEnderPearl() {
        if (mc.player == null || mc.player.getInventory() == null) {
            return false;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != null && stack.getItem() instanceof EnderPearlItem) {
                return true;
            }
        }
        return false;
    }

    private boolean isFallingIntoVoid() {
        if (mc.player == null) {
            return false;
        }

        for (int i = 0; i <= 128; i++) {
            if (isOnGround(mc.player,i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOnGround(Entity entity, double height) {
        return entity.getWorld().getBlockCollisions(entity, entity.getBoundingBox().offset(0, -height, 0)).iterator().hasNext();
    }
}