package com.dentomas.deathgoddess.entity;

import com.dentomas.deathgoddess.registry.ModEntities;
import com.dentomas.deathgoddess.registry.ModSounds;
import com.mojang.math.Vector3f;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

/** An eye torn from the halo: hangs for a moment, then hunts its target. Can be struck down. */
public class WatchingEye extends Projectile {
    private static final int HOVER_TICKS = 14;
    private static final int HOMING_TICKS = 70;
    private static final int MAX_LIFE = 130;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.55F, 0.04F, 0.1F), 0.9F);

    @Nullable
    private UUID targetId;
    @Nullable
    private Entity target;
    private int life;
    private float speed = 0.7F;

    public WatchingEye(EntityType<? extends WatchingEye> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public WatchingEye(Level level, LivingEntity owner, @Nullable LivingEntity target, float speed) {
        this(ModEntities.WATCHING_EYE.get(), level);
        this.setOwner(owner);
        this.target = target;
        this.targetId = target == null ? null : target.getUUID();
        this.speed = speed;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            if (++this.life > MAX_LIFE) {
                this.discard();
                return;
            }
            this.steer();
        } else {
            this.level.addParticle(TRAIL, this.getX(), this.getY() + 0.2D, this.getZ(), 0.0D, 0.0D, 0.0D);
            if (this.random.nextInt(3) == 0) {
                this.level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.2D, this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }

        HitResult hit = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            this.onHit(hit);
        }
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.3F);
    }

    private void steer() {
        if (this.life < HOVER_TICKS) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.85D));
            return;
        }
        Entity prey = this.resolveTarget();
        Vec3 motion = this.getDeltaMovement();
        if (prey != null && prey.isAlive() && this.life < HOMING_TICKS) {
            Vec3 want = prey.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(this.speed);
            double turn = this.life == HOVER_TICKS ? 1.0D : 0.16D;
            motion = motion.lerp(want, turn);
        } else if (motion.lengthSqr() < 0.01D) {
            motion = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        }
        this.setDeltaMovement(motion.normalize().scale(this.speed));
    }

    @Nullable
    private Entity resolveTarget() {
        if (this.target == null && this.targetId != null && this.level instanceof ServerLevel server) {
            this.target = server.getEntity(this.targetId);
        }
        return this.target;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof GoddessOfDeath) && !(entity instanceof WatchingEye);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level.isClientSide) {
            return;
        }
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        if (result.getEntity() instanceof LivingEntity victim && DeathMagic.strike(owner, this, victim, 7.0F)
                && victim.isAlive()) {
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), this.getEffectSource());
        }
        this.burst();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level.isClientSide) {
            this.burst();
        }
    }

    private void burst() {
        if (this.level instanceof ServerLevel server) {
            server.sendParticles(TRAIL, this.getX(), this.getY() + 0.2D, this.getZ(), 14, 0.2D, 0.2D, 0.2D, 0.02D);
            server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.2D, this.getZ(), 8, 0.15D, 0.15D, 0.15D, 0.02D);
            DeathMagic.blightDisc(server, this.getOwner(), DeathMagic.ground(server, this.position()), 1.6D);
        }
        this.playSound(ModSounds.EYE_HIT.get(), 0.9F, 0.85F + this.random.nextFloat() * 0.3F);
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.getEntity() instanceof GoddessOfDeath) {
            return false;
        }
        if (!this.level.isClientSide) {
            this.burst();
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }
        tag.putInt("Life", this.life);
        tag.putFloat("Speed", this.speed);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Target")) {
            this.targetId = tag.getUUID("Target");
        }
        this.life = tag.getInt("Life");
        if (tag.contains("Speed")) {
            this.speed = tag.getFloat("Speed");
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
