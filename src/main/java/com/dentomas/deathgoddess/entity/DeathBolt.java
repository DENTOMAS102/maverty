package com.dentomas.deathgoddess.entity;

import com.dentomas.deathgoddess.registry.ModEntities;
import com.dentomas.deathgoddess.registry.ModSounds;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

/**
 * Her thrown magic, in three forms that share one class and differ by entity type:
 * a crescent that skims the ground and cuts through everything in its path, a slow orb,
 * and a lance that hangs in the air taking aim before it flies.
 */
public class DeathBolt extends Projectile {
    public enum Kind {
        CRESCENT, ORB, LANCE
    }

    /** A lance stops correcting its aim this many ticks before it flies. */
    private static final int LANCE_LOCK = 6;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.5F, 0.04F, 0.1F), 1.0F);

    private final IntSet struck = new IntOpenHashSet();
    @Nullable
    private LivingEntity target;
    private Vec3 aim = new Vec3(0.0D, 0.0D, 1.0D);
    private float damage = 6.0F;
    private float speed = 0.5F;
    private int hover;
    private int maxLife = 80;
    private int life;

    public DeathBolt(EntityType<? extends DeathBolt> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    private static DeathBolt create(EntityType<DeathBolt> type, Level level, LivingEntity owner, Vec3 from, Vec3 direction,
                                    float speed, float damage, int maxLife) {
        DeathBolt bolt = new DeathBolt(type, level);
        bolt.setOwner(owner);
        bolt.setPos(from.x, from.y, from.z);
        bolt.aim = direction.normalize();
        bolt.speed = speed;
        bolt.damage = damage;
        bolt.maxLife = maxLife;
        bolt.face(bolt.aim);
        bolt.xRotO = bolt.getXRot();
        bolt.yRotO = bolt.getYRot();
        return bolt;
    }

    public static DeathBolt crescent(Level level, LivingEntity owner, Vec3 from, Vec3 direction, float speed, float damage) {
        DeathBolt bolt = create(ModEntities.DEATH_CRESCENT.get(), level, owner, from, direction.multiply(1.0D, 0.0D, 1.0D),
                speed, damage, (int) (34.0F / speed));
        bolt.setDeltaMovement(bolt.aim.scale(speed));
        level.addFreshEntity(bolt);
        return bolt;
    }

    public static DeathBolt orb(Level level, LivingEntity owner, Vec3 from, Vec3 direction, float speed, float damage) {
        DeathBolt bolt = create(ModEntities.DEATH_ORB.get(), level, owner, from, direction, speed, damage,
                (int) (30.0F / speed));
        bolt.setDeltaMovement(bolt.aim.scale(speed));
        level.addFreshEntity(bolt);
        return bolt;
    }

    /** Hangs where it is made for {@code hover} ticks, turning to follow the target, then flies straight. */
    public static DeathBolt lance(Level level, LivingEntity owner, Vec3 from, @Nullable LivingEntity target, Vec3 direction,
                                  int hover, float speed, float damage) {
        DeathBolt bolt = create(ModEntities.SOUL_LANCE.get(), level, owner, from, direction, speed, damage,
                hover + (int) (48.0F / speed));
        bolt.target = target;
        bolt.hover = hover;
        level.addFreshEntity(bolt);
        return bolt;
    }

    public Kind kind() {
        EntityType<?> type = this.getType();
        return type == ModEntities.DEATH_CRESCENT.get() ? Kind.CRESCENT : type == ModEntities.SOUL_LANCE.get() ? Kind.LANCE
                : Kind.ORB;
    }

    @Override
    protected void defineSynchedData() {
    }

    /** Arrow convention: yaw from atan2(x, z), pitch positive upward. */
    private void face(Vec3 direction) {
        this.setYRot((float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    @Override
    public void tick() {
        super.tick();
        Kind kind = this.kind();
        if (!this.level.isClientSide) {
            if (++this.life > this.maxLife) {
                this.discard();
                return;
            }
            if (kind == Kind.LANCE && this.life <= this.hover) {
                if (this.target != null && this.target.isAlive() && this.life < this.hover - LANCE_LOCK) {
                    this.aim = this.target.getBoundingBox().getCenter().subtract(this.position()).normalize();
                }
                this.face(this.aim);
                if (this.life < this.hover) {
                    return;
                }
                this.setDeltaMovement(this.aim.scale(this.speed));
                this.hasImpulse = true;
                this.playSound(ModSounds.LANCE_FIRE.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.25F);
            }
        } else {
            this.clientTick(kind);
        }

        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        if (!this.level.isClientSide) {
            if (kind == Kind.CRESCENT) {
                motion = this.skimGround(motion);
                if (motion == null) {
                    return;
                }
                this.cut(motion);
            } else {
                HitResult hit = ProjectileUtil.getHitResult(this, this::canHitEntity);
                if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                    this.onHit(hit);
                }
                if (this.isRemoved()) {
                    return;
                }
            }
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        this.face(motion);
    }

    /** Keeps a crescent just above the ground, climbing single steps and ending against walls. */
    @Nullable
    private Vec3 skimGround(Vec3 motion) {
        Vec3 ahead = new Vec3(this.getX() + motion.x, this.getY(), this.getZ() + motion.z);
        Vec3 ground = DeathMagic.ground(this.level, ahead);
        double step = ground.y + 0.3D - this.getY();
        if (step > 1.3D) {
            this.burst(1.6D);
            return null;
        }
        Vec3 adjusted = new Vec3(motion.x, Mth.clamp(step, -0.7D, 1.3D), motion.z);
        this.setDeltaMovement(adjusted);
        return adjusted;
    }

    private void cut(Vec3 motion) {
        AABB reach = this.getBoundingBox().expandTowards(motion).inflate(0.15D, 0.7D, 0.15D);
        for (LivingEntity victim : this.level.getEntitiesOfClass(LivingEntity.class, reach, LivingEntity::isAlive)) {
            if (this.struck.add(victim.getId())) {
                DeathMagic.strike(this.owner(), this, victim, this.damage);
            }
        }
        DeathMagic.blightDisc(this.level, this.owner(), this.position(), 1.5D);
    }

    @Nullable
    private LivingEntity owner() {
        return this.getOwner() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof GoddessOfDeath) && !(entity instanceof Projectile);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level.isClientSide && result.getEntity() instanceof LivingEntity victim) {
            DeathMagic.strike(this.owner(), this, victim, this.damage);
            this.burst(0.0D);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level.isClientSide) {
            this.burst(this.kind() == Kind.LANCE ? 2.2D : 1.6D);
        }
    }

    private void burst(double blightRadius) {
        if (this.level instanceof ServerLevel server) {
            server.sendParticles(TRAIL, this.getX(), this.getY() + 0.2D, this.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.0D);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.2D, this.getZ(), 6, 0.3D, 0.2D, 0.3D,
                    0.02D);
            if (blightRadius > 0.0D) {
                DeathMagic.blightDisc(server, this.owner(), DeathMagic.ground(server, this.position()), blightRadius);
            }
        }
        this.playSound(ModSounds.EYE_HIT.get(), 0.7F, 0.6F + this.random.nextFloat() * 0.3F);
        this.discard();
    }

    private void clientTick(Kind kind) {
        if (kind == Kind.ORB && this.tickCount % 2 != 0) {
            return;
        }
        double spread = kind == Kind.CRESCENT ? 1.0D : 0.15D;
        this.level.addParticle(TRAIL,
                this.getX() + (this.random.nextDouble() - 0.5D) * spread * 2.0D, this.getY() + this.getBbHeight() * 0.5D,
                this.getZ() + (this.random.nextDouble() - 0.5D) * spread * 2.0D, 0.0D, 0.0D, 0.0D);
        if (kind == Kind.CRESCENT) {
            this.level.addParticle(ParticleTypes.SMOKE, this.getRandomX(0.5D), this.getY() + 0.1D, this.getRandomZ(0.5D), 0.0D,
                    0.04D, 0.0D);
        }
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
