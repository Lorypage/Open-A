package cn.lazymoon.mixin.injector.entity;

import cn.lazymoon.event.impl.player.*;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.movement.NoSlow;
import cn.lazymoon.features.module.impl.visual.OldHitting;
import cn.lazymoon.utils.misc.ItemUtils;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.GameProfile;
import cn.lazymoon.Client;
import cn.lazymoon.utils.InstanceAccess;
import net.minecraft.block.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity implements InstanceAccess {
    @Shadow
    public abstract boolean isUsingItem();

    @Shadow
    protected abstract void sendSprintingPacket();

    @Shadow
    public double lastX;

    @Shadow
    public double lastBaseY;

    @Shadow
    public double lastZ;

    @Shadow
    public float lastYaw;

    @Shadow
    public float lastPitch;

    @Shadow
    private boolean lastOnGround;

    @Shadow
    private boolean lastHorizontalCollision;

    @Shadow
    private boolean autoJumpEnabled;

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    @Shadow
    protected abstract boolean isCamera();

    @Shadow
    public int ticksSinceLastPositionPacketSent;

    @Shadow
    @Final
    protected MinecraftClient client;

    @Shadow
    public Input input = new Input();

    @Shadow
    public int ticksLeftToDoubleTapSprint;

    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onUpdate(CallbackInfo ci) {
        Client.INSTANCE.getEventManager().call(new UpdateEvent());
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        Client.INSTANCE.getEventManager().call(event);
        ci.cancel();

        if (!event.isCancelled()) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
        }
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendSneakingPacket()V"))
    private boolean sendSneakingAfterSprinting(ClientPlayerEntity instance) {
        return !(Client.INSTANCE.getModuleManager().getModule(OldHitting.class).isState() && OldHitting.sneakTiming.get());
    }

    @Shadow protected abstract void pushOutOfBlocks(double x, double z);
    @Shadow public abstract Portal.Effect getCurrentPortalEffect();
    @Shadow protected abstract void tickNausea(boolean fromPortalEffect);
    @Shadow private boolean inSneakingPose;
    @Shadow public int ticksToNextAutoJump;
    @Shadow private boolean falling;
    @Shadow private int underwaterVisibilityTicks;

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    public void tickMovement(CallbackInfo ci) {
        ci.cancel();

        Client.INSTANCE.getEventManager().call(new LivingUpdateEvent());
        Client.INSTANCE.getEventManager().call(new TickMovementEvent());

        if (this.ticksLeftToDoubleTapSprint> 0) {
            --this.ticksLeftToDoubleTapSprint;
        }
        if (!(this.client.currentScreen instanceof DownloadingTerrainScreen)) {
            this.tickNausea(this.getCurrentPortalEffect() == Portal.Effect.CONFUSION);
            this.tickPortalCooldown();
        }
        boolean bl = this.input.playerInput.jump();
        boolean bl2 = this.input.playerInput.sneak();
        boolean bl3 = this.isWalking();
        PlayerAbilities playerAbilities = this.getAbilities();
        this.inSneakingPose = !playerAbilities.flying && !this.isSwimming() && !this.hasVehicle() && this.canChangeIntoPose(EntityPose.CROUCHING) && (this.isSneaking() || !this.isSleeping() && !this.canChangeIntoPose(EntityPose.STANDING));
        this.input.tick();
        this.client.getTutorialManager().onMovement(this.input);
        if (shouldStopSprinting()) {
            this.setSprinting(false);
        }
        if (this.isUsingItem() || KillAura.blocking && !this.hasVehicle()) {
            SlowDownEvent event = new SlowDownEvent(0.2F, 0.2F);
            Client.INSTANCE.getEventManager().call(event);
            Input var10000 = this.input;
            if (!event.isCancelled()) {
                var10000.movementSideways *= event.getSideways();
                var10000.movementForward *= event.getForward();
                this.ticksLeftToDoubleTapSprint = 0;
            }
        }
        if (this.shouldSlowDown()) {
            float f = (float)this.getAttributeValue(EntityAttributes.SNEAKING_SPEED);
            Input var17 = this.input;
            var17.movementSideways *= f;
            var17.movementForward *= f;
        }
        boolean bl4 = false;
        if (this.ticksToNextAutoJump > 0) {
            --this.ticksToNextAutoJump;
            bl4 = true;
            this.input.jump();
        }
        if (!this.noClip) {
            this.pushOutOfBlocks(this.getX() - (double)this.getWidth() * 0.35, this.getZ() + (double)this.getWidth() * 0.35);
            this.pushOutOfBlocks(this.getX() - (double)this.getWidth() * 0.35, this.getZ() - (double)this.getWidth() * 0.35);
            this.pushOutOfBlocks(this.getX() + (double)this.getWidth() * 0.35, this.getZ() - (double)this.getWidth() * 0.35);
            this.pushOutOfBlocks(this.getX() + (double)this.getWidth() * 0.35, this.getZ() + (double)this.getWidth() * 0.35);
        }
        if (bl2) {
            this.ticksLeftToDoubleTapSprint = 0;
        }
        boolean bl5 = canStartSprinting();
        boolean bl6 = this.hasVehicle() ? Objects.requireNonNull(this.getVehicle()).isOnGround() : this.isOnGround();
        boolean bl7 = !bl2 && !bl3;
        if ((bl6 || this.isSubmergedInWater()) && bl7 && bl5) {
            if (this.ticksLeftToDoubleTapSprint <= 0 && !this.client.options.sprintKey.isPressed()) {
                this.ticksLeftToDoubleTapSprint = 7;
            } else {
                this.setSprinting(true);
            }
        }
        if ((!this.isTouchingWater() || this.isSubmergedInWater() && !(OldHitting.cancelSwimming.get() && Client.INSTANCE.getModuleManager().getModule(OldHitting.class).isState())) && bl5 && this.client.options.sprintKey.isPressed()) {
            this.setSprinting(true);
        }
        if (this.isSprinting()) {
            boolean bl8 = !this.input.hasForwardMovement() || !this.canSprint();
            boolean bl9 = bl8 || this.horizontalCollision && !this.collidedSoftly || this.isTouchingWater() && !this.isSubmergedInWater() || this.isSubmergedInWater && (OldHitting.cancelSwimming.get() && Client.INSTANCE.getModuleManager().getModule(OldHitting.class).isState());
            if (this.isSwimming()) {
                if (!this.isOnGround() && !this.input.playerInput.sneak() && bl8 || !this.isTouchingWater()) {
                    this.setSprinting(false);
                }
            } else if (bl9) {
                this.setSprinting(false);
            }
        }
        boolean bl8 = false;
        if (playerAbilities.allowFlying) {
            if (Objects.requireNonNull(this.client.interactionManager).isFlyingLocked()) {
                if (!playerAbilities.flying) {
                    playerAbilities.flying = true;
                    bl8 = true;
                    this.sendAbilitiesUpdate();
                }
            } else if (!bl && this.input.playerInput.jump() && !bl4) {
                if (this.abilityResyncCountdown == 0) {
                    this.abilityResyncCountdown = 7;
                } else if (!this.isSwimming()) {
                    playerAbilities.flying = !playerAbilities.flying;
                    if (playerAbilities.flying && this.isOnGround()) {
                        this.jump();
                    }

                    bl8 = true;
                    this.sendAbilitiesUpdate();
                    this.abilityResyncCountdown = 0;
                }
            }
        }
        if (this.input.playerInput.jump() && !bl8 && !bl && !this.isClimbing() && this.checkGliding()) {
            this.networkHandler.sendPacket(new ClientCommandC2SPacket(this, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
        this.falling = this.isGliding();
        if (this.isTouchingWater() && this.input.playerInput.sneak() && this.shouldSwimInFluids()) {
            this.knockDownwards();
        }
        if (this.isSubmergedIn(FluidTags.WATER)) {
            int i = this.isSpectator() ? 10 : 1;
            this.underwaterVisibilityTicks = MathHelper.clamp(this.underwaterVisibilityTicks + i, 0, 600);
        } else if (this.underwaterVisibilityTicks > 0) {
            this.isSubmergedIn(FluidTags.WATER);
            this.underwaterVisibilityTicks = MathHelper.clamp(this.underwaterVisibilityTicks - 10, 0, 600);
        }
        if (playerAbilities.flying && this.isCamera()) {
            int i = 0;
            if (this.input.playerInput.sneak()) {
                --i;
            }
            if (this.input.playerInput.jump()) {
                ++i;
            }
            if (i != 0) {
                this.setVelocity(this.getVelocity().add(0.0F, (float)i * playerAbilities.getFlySpeed() * 3.0F, 0.0F));
            }
        }
        JumpingMount jumpingMount = this.getJumpingMount();
        if (jumpingMount != null && jumpingMount.getJumpCooldown() == 0) {
            if (this.field_3938 < 0) {
                ++this.field_3938;
                if (this.field_3938 == 0) {
                    this.mountJumpStrength = 0.0F;
                }
            }
            if (bl && !this.input.playerInput.jump()) {
                this.field_3938 = -10;
                jumpingMount.setJumpStrength(MathHelper.floor(this.getMountJumpStrength() * 100.0F));
                this.startRidingJump();
            } else if (!bl && this.input.playerInput.jump()) {
                this.field_3938 = 0;
                this.mountJumpStrength = 0.0F;
            } else if (bl) {
                ++this.field_3938;
                if (this.field_3938 < 10) {
                    this.mountJumpStrength = (float)this.field_3938 * 0.1F;
                } else {
                    this.mountJumpStrength = 0.8F + 2.0F / (float)(this.field_3938 - 9) * 0.1F;
                }
            }
        } else {
            this.mountJumpStrength = 0.0F;
        }
        Client.INSTANCE.getEventManager().call(new PostUpdateEvent());
        super.tickMovement();
        if (this.isOnGround() && playerAbilities.flying && !Objects.requireNonNull(this.client.interactionManager).isFlyingLocked()) {
            playerAbilities.flying = false;
            this.sendAbilitiesUpdate();
        }
    }

    @Shadow public abstract float getMountJumpStrength();
    @Shadow private float mountJumpStrength;
    @Shadow protected abstract void startRidingJump();
    @Shadow private int field_3938;
    @Shadow protected abstract boolean isBlind();
    @Shadow public abstract boolean shouldSlowDown();
    @Shadow protected abstract boolean isRidingCamel();
    @Shadow protected abstract boolean canSprint();
    @Shadow protected abstract boolean isWalking();
    @Shadow protected abstract boolean canVehicleSprint(Entity vehicle);
    @Shadow @Nullable
    public abstract JumpingMount getJumpingMount();

    @Unique
    private boolean shouldStopSprinting() {
        return this.isGliding() || this.isBlind() || this.shouldSlowDown() || this.hasVehicle() && !this.isRidingCamel() || (this.isUsingItem() && !(Objects.requireNonNull(Client.INSTANCE.getModuleManager().getModule(NoSlow.class)).isState() && !NoSlow.slowByWaitingServer && ((this.getMainHandStack().getItem() instanceof SwordItem && (this.isUsingItem() || KillAura.blocking))))) && !this.hasVehicle() && !this.isSubmergedInWater();
    }

    @Unique
    private boolean canStartSprinting() {
        return !this.isSprinting() && this.isWalking() && this.canSprint() && (!this.isUsingItem() || (Objects.requireNonNull(Client.INSTANCE.getModuleManager().getModule(NoSlow.class)).isState() && !NoSlow.slowByWaitingServer && ((this.getMainHandStack().getItem() instanceof SwordItem && (this.isUsingItem() || KillAura.blocking)) || (ItemUtils.isConsumable(this.getMainHandStack()) && this.isUsingItem())))) && !this.isBlind() && (!this.hasVehicle() || this.canVehicleSprint(this.getVehicle())) && !this.isGliding() && (!this.shouldSlowDown() || this.isSubmergedInWater() && !(OldHitting.cancelSwimming.get() && Client.INSTANCE.getModuleManager().getModule(OldHitting.class).isState()));
    }

    @Shadow protected abstract void sendSneakingPacket();

    /**
     * @author DSJ
     * @reason MotionEvent / R0tati0nManager
     */
    @Overwrite
    private void sendMovementPackets() {
        MotionEvent motionEvent = new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), this.onGround, this.horizontalCollision, MotionEvent.State.Pre);

        if ((Object) this == mc.player) {
            Client.INSTANCE.getEventManager().call(motionEvent);
        }

        this.sendSprintingPacket();
        if (Client.INSTANCE.getModuleManager().getModule(OldHitting.class).isState() && OldHitting.sneakTiming.get()) {
            this.sendSneakingPacket();
        }
        if (this.isCamera()) {
            float prevYaw = this.lastYaw;
            float prevPitch = this.lastPitch;

            double d = motionEvent.getX() - this.lastX;
            double e = motionEvent.getY() - this.lastBaseY;
            double f = motionEvent.getZ() - this.lastZ;

            float yaw = motionEvent.getYaw();
            float pitch = motionEvent.getPitch();

            double g = yaw - this.lastYaw;
            double h = pitch - this.lastPitch;

            ++this.ticksSinceLastPositionPacketSent;

            boolean bl = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
            boolean bl2 = g != 0.0 || h != 0.0;

            if (bl && bl2) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(motionEvent.getX(), motionEvent.getY(), motionEvent.getZ(), yaw, pitch, motionEvent.isOnGround(), motionEvent.isHorizontalCollision()));
            } else if (bl) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(motionEvent.getX(), motionEvent.getY(), motionEvent.getZ(), motionEvent.isOnGround(), motionEvent.isHorizontalCollision()));
            } else if (bl2) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, motionEvent.isOnGround(), motionEvent.isHorizontalCollision()));
            } else if (this.lastOnGround != motionEvent.isOnGround() || this.lastHorizontalCollision != motionEvent.isHorizontalCollision()) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(motionEvent.isOnGround(), motionEvent.isHorizontalCollision()));
            }

            if (bl) {
                this.lastX = motionEvent.getX();
                this.lastBaseY = motionEvent.getY();
                this.lastZ = motionEvent.getZ();
                this.ticksSinceLastPositionPacketSent = 0;
            }

            if (bl2) {
                this.lastYaw = yaw;
                this.lastPitch = pitch;
            }

            this.lastOnGround = motionEvent.isOnGround();
            this.lastHorizontalCollision = motionEvent.isHorizontalCollision();
            this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
        }

        Client.INSTANCE.getEventManager().call(new MotionEvent(MotionEvent.State.Post));
    }
}
