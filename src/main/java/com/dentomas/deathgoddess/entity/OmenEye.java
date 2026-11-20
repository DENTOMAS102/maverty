package com.dentomas.deathgoddess.entity;

import com.dentomas.deathgoddess.registry.ModEntities;
import com.dentomas.deathgoddess.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * The great eye that opens above a player, watches them for about seven seconds and closes again.
 * It does no harm. The watched player's client narrows and shakes the view while it is open
 * (see WatchedEffects); everyone else just sees the eye hanging over them.
 */
public class OmenEye extends Entity {
    public static final int OPEN_TICKS = 14;
    public static final int WATCH_TICKS = 140;
    public static final int CLOSE_TICKS = 12;
    private static final int LIFETIME = OPEN_TICKS + WATCH_TICKS + CLOSE_TICKS;

    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(OmenEye.class, EntityDataSerializers.INT);

    @Nullable
    private ServerPlayer watched;

    public OmenEye(EntityType<? extends OmenEye> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void open(ServerPlayer player) {
        OmenEye eye = new OmenEye(ModEntities.OMEN_EYE.get(), player.level);
        eye.watched = player;
        eye.entityData.set(TARGET, player.getId());
        Vec3 at = eye.perch(player);
        eye.setPos(at.x, at.y, at.z);
        player.level.addFreshEntity(eye);
        // Heard by the watched player alone.
        player.playNotifySound(ModSounds.WATCHED.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET, -1);
    }

    public int getTargetId() {
        return this.entityData.get(TARGET);
    }

    /** Above the head and a little ahead of where they are facing, so it sits at the top of their view. */
    private Vec3 perch(Entity target) {
        Vec3 eyes = target.getEyePosition();
        Vec3 ahead = Vec3.directionFromRotation(0.0F, target.getYRot());
        Vec3 wanted = eyes.add(ahead.scale(3.2D)).add(0.0D, 3.4D, 0.0D);
        HitResult hit = this.level.clip(new ClipContext(eyes, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
        if (hit.getType() == HitResult.Type.MISS) {
            return wanted;
        }
        // Under a low ceiling it comes as close as the stone allows.
        Vec3 back = eyes.subtract(wanted).normalize().scale(0.7D);
        return hit.getLocation().add(back);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            if (this.tickCount > OPEN_TICKS && this.random.nextInt(3) == 0) {
                this.level.addParticle(ParticleTypes.SMOKE, this.getX() + (this.random.nextDouble() - 0.5D) * 3.0D,
                        this.getY() + (this.random.nextDouble() - 0.5D) * 1.6D, this.getZ() + (this.random.nextDouble() - 0.5D) * 3.0D,
                        0.0D, -0.01D, 0.0D);
            }
            return;
        }
        if (this.watched == null || !this.watched.isAlive() || this.watched.level != this.level || this.tickCount >= LIFETIME) {
            this.discard();
            return;
        }
        Vec3 next = this.position().lerp(this.perch(this.watched), 0.18D);
        this.setPos(next.x, next.y, next.z);
    }

    /** 0 closed, 1 fully open. */
    public float openness(float partialTick) {
        float age = this.tickCount + partialTick;
        if (age < OPEN_TICKS) {
            return Mth.clamp(age / OPEN_TICKS, 0.0F, 1.0F);
        }
        return Mth.clamp((LIFETIME - age) / CLOSE_TICKS, 0.0F, 1.0F);
    }

    /** How strongly the watched player feels it: rises once the eye has opened, fades as it closes. */
    public float gaze() {
        float age = this.tickCount;
        float rise = Mth.clamp((age - OPEN_TICKS * 0.5F) / 20.0F, 0.0F, 1.0F);
        float fall = Mth.clamp((LIFETIME - age) / 16.0F, 0.0F, 1.0F);
        return Math.min(rise, fall);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0D * 96.0D;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
