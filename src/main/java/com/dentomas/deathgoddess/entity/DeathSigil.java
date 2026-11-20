package com.dentomas.deathgoddess.entity;

import com.dentomas.deathgoddess.registry.ModEntities;
import com.dentomas.deathgoddess.registry.ModSounds;
import com.mojang.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * A mark of death on the ground. It shows exactly where the magic will land, fills while it warms
 * up, then strikes everything inside it once and fades. All her area attacks are built from these.
 */
public class DeathSigil extends Entity {
    public static final int DISC = 0;
    public static final int RING = 1;
    public static final int LINE = 2;

    public static final int PILLAR = 0;
    public static final int MARK = 1;
    /** Harmless: a refuge that the Requiem cannot reach. */
    public static final int SAFE = 2;
    public static final int RAIN = 3;
    public static final int BLADE = 4;
    public static final int TOLL = 5;
    public static final int REQUIEM = 6;

    /** How long the burst stays visible after the strike. */
    public static final int FADE_TICKS = 12;

    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.55F, 0.05F, 0.12F), 1.4F);
    private static final EntityDataAccessor<Integer> SHAPE = SynchedEntityData.defineId(DeathSigil.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(DeathSigil.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WARMUP = SynchedEntityData.defineId(DeathSigil.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE_A = SynchedEntityData.defineId(DeathSigil.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SIZE_B = SynchedEntityData.defineId(DeathSigil.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(DeathSigil.class, EntityDataSerializers.FLOAT);

    @Nullable
    private LivingEntity owner;
    @Nullable
    private LivingEntity tracked;
    private int lockTicks;
    private float damage;
    private boolean quiet;
    private final List<Vec3> safeSpots = new ArrayList<>();
    private float safeRadius;

    public DeathSigil(EntityType<? extends DeathSigil> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    private static DeathSigil spawn(Level level, @Nullable LivingEntity owner, Vec3 at, int shape, float a, float b, float yaw,
                                    int warmup, float damage, int style) {
        DeathSigil sigil = new DeathSigil(ModEntities.DEATH_SIGIL.get(), level);
        Vec3 ground = DeathMagic.ground(level, at);
        sigil.setPos(ground.x, ground.y, ground.z);
        sigil.owner = owner;
        sigil.damage = damage;
        sigil.entityData.set(SHAPE, shape);
        sigil.entityData.set(STYLE, style);
        sigil.entityData.set(WARMUP, warmup);
        sigil.entityData.set(SIZE_A, a);
        sigil.entityData.set(SIZE_B, b);
        sigil.entityData.set(YAW, yaw);
        level.addFreshEntity(sigil);
        return sigil;
    }

    public static DeathSigil disc(Level level, @Nullable LivingEntity owner, Vec3 at, float radius, int warmup, float damage,
                                  int style) {
        return spawn(level, owner, at, DISC, radius, 0.0F, 0.0F, warmup, damage, style);
    }

    public static DeathSigil ring(Level level, @Nullable LivingEntity owner, Vec3 at, float inner, float outer, int warmup,
                                  float damage, int style) {
        return spawn(level, owner, at, RING, outer, inner, 0.0F, warmup, damage, style);
    }

    /** A strip starting at {@code from} and running {@code length} blocks along {@code yaw}. */
    public static DeathSigil line(Level level, @Nullable LivingEntity owner, Vec3 from, float yaw, float length, float width,
                                  int warmup, float damage, int style) {
        return spawn(level, owner, from, LINE, length, width, yaw, warmup, damage, style);
    }

    /** Follows the entity until {@code lockTicks} before it strikes. */
    public DeathSigil track(LivingEntity entity, int lockTicks) {
        this.tracked = entity;
        this.lockTicks = lockTicks;
        return this;
    }

    /** No strike or wither sound; used for all but one sigil of a batch that lands together. */
    public DeathSigil quiet() {
        this.quiet = true;
        return this;
    }

    public DeathSigil spare(List<Vec3> spots, float radius) {
        this.safeSpots.addAll(spots);
        this.safeRadius = radius;
        return this;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SHAPE, DISC);
        this.entityData.define(STYLE, PILLAR);
        this.entityData.define(WARMUP, 30);
        this.entityData.define(SIZE_A, 3.0F);
        this.entityData.define(SIZE_B, 0.0F);
        this.entityData.define(YAW, 0.0F);
    }

    public int getShape() {
        return this.entityData.get(SHAPE);
    }

    public int getStyle() {
        return this.entityData.get(STYLE);
    }

    public int getWarmup() {
        return this.entityData.get(WARMUP);
    }

    /** Disc radius, ring outer radius or line length. */
    public float getSizeA() {
        return this.entityData.get(SIZE_A);
    }

    /** Ring inner radius or line width. */
    public float getSizeB() {
        return this.entityData.get(SIZE_B);
    }

    public float getYaw() {
        return this.entityData.get(YAW);
    }

    public boolean contains(double x, double z) {
        double dx = x - this.getX();
        double dz = z - this.getZ();
        float a = this.getSizeA();
        float b = this.getSizeB();
        switch (this.getShape()) {
            case RING -> {
                double d = dx * dx + dz * dz;
                return d <= a * a && d >= b * b;
            }
            case LINE -> {
                float yaw = this.getYaw() * Mth.DEG_TO_RAD;
                double dirX = -Mth.sin(yaw);
                double dirZ = Mth.cos(yaw);
                double along = dx * dirX + dz * dirZ;
                double across = dx * dirZ - dz * dirX;
                return along >= 0.0D && along <= a && Math.abs(across) <= b * 0.5D;
            }
            default -> {
                return dx * dx + dz * dz <= a * a;
            }
        }
    }

    private boolean spared(double x, double z) {
        for (Vec3 spot : this.safeSpots) {
            if ((x - spot.x) * (x - spot.x) + (z - spot.z) * (z - spot.z) <= this.safeRadius * this.safeRadius) {
                return true;
            }
        }
        return false;
    }

    public AABB shapeBounds() {
        float a = this.getSizeA();
        if (this.getShape() != LINE) {
            return new AABB(this.getX() - a, this.getY(), this.getZ() - a, this.getX() + a, this.getY(), this.getZ() + a);
        }
        float yaw = this.getYaw() * Mth.DEG_TO_RAD;
        double endX = this.getX() - Mth.sin(yaw) * a;
        double endZ = this.getZ() + Mth.cos(yaw) * a;
        double half = this.getSizeB() * 0.5D;
        return new AABB(this.getX(), this.getY(), this.getZ(), endX, this.getY(), endZ).inflate(half, 0.0D, half);
    }

    @Override
    public void tick() {
        super.tick();
        int age = this.tickCount;
        int warmup = this.getWarmup();
        if (this.level.isClientSide) {
            this.clientTick(age, warmup);
            return;
        }
        if (age < warmup) {
            if (this.tracked != null && this.tracked.isAlive() && age < warmup - this.lockTicks) {
                Vec3 goal = DeathMagic.ground(this.level, this.tracked.position());
                Vec3 next = this.position().lerp(goal, 0.3D);
                this.setPos(next.x, goal.y, next.z);
            }
        } else if (age == warmup) {
            this.strike();
        } else if (age >= warmup + FADE_TICKS) {
            this.discard();
        }
    }

    private void strike() {
        int style = this.getStyle();
        if (style == SAFE) {
            return;
        }
        DeathMagic.Area area = (x, z) -> this.contains(x, z) && !this.spared(x, z);
        AABB bounds = this.shapeBounds();
        DeathMagic.strikeArea(this.level, this.owner, this, bounds, this.getY(), area, this.damage);
        int withered = DeathMagic.blight(this.level, this.owner, bounds, this.getY(), area);
        if (this.quiet) {
            return;
        }
        Vec3 center = bounds.getCenter();
        SoundEvent sound = switch (style) {
            case RAIN -> ModSounds.RAIN_FALL.get();
            case BLADE -> ModSounds.BLADE_WAVE.get();
            case TOLL -> ModSounds.KNELL.get();
            case REQUIEM -> ModSounds.REQUIEM_RELEASE.get();
            default -> ModSounds.SIGIL_ERUPT.get();
        };
        float volume = style == REQUIEM ? 5.0F : style == RAIN ? 1.2F : 2.5F;
        this.level.playSound(null, center.x, this.getY(), center.z, sound, SoundSource.HOSTILE, volume,
                0.9F + this.random.nextFloat() * 0.2F);
        if (withered > 6) {
            this.level.playSound(null, center.x, this.getY(), center.z, ModSounds.WITHER.get(), SoundSource.HOSTILE, 1.0F,
                    0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private Vec3 randomPoint(boolean rim) {
        float a = this.getSizeA();
        float b = this.getSizeB();
        if (this.getShape() == LINE) {
            float yaw = this.getYaw() * Mth.DEG_TO_RAD;
            double along = this.random.nextDouble() * a;
            double across = rim ? (this.random.nextBoolean() ? 0.5D : -0.5D) * b : (this.random.nextDouble() - 0.5D) * b;
            return new Vec3(this.getX() - Mth.sin(yaw) * along + Mth.cos(yaw) * across, this.getY(),
                    this.getZ() + Mth.cos(yaw) * along + Mth.sin(yaw) * across);
        }
        double inner = this.getShape() == RING ? b : 0.0D;
        double radius = rim ? a : Math.sqrt(Mth.lerp(this.random.nextDouble(), inner * inner, a * a));
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(this.getX() + Math.cos(angle) * radius, this.getY(), this.getZ() + Math.sin(angle) * radius);
    }

    private double area() {
        float a = this.getSizeA();
        float b = this.getSizeB();
        return this.getShape() == LINE ? a * b : Math.PI * (a * a - (this.getShape() == RING ? b * b : 0.0F));
    }

    private void clientTick(int age, int warmup) {
        int style = this.getStyle();
        if (age < warmup) {
            // Ash lifting off the outline while it charges.
            if (this.random.nextInt(style == SAFE ? 2 : 3) == 0) {
                Vec3 p = this.randomPoint(true);
                this.level.addParticle(style == SAFE ? ParticleTypes.WHITE_ASH : ParticleTypes.SMOKE, p.x, p.y + 0.1D, p.z,
                        0.0D, 0.03D, 0.0D);
            }
        } else if (age == warmup && style != SAFE) {
            int count = (int) Mth.clamp(this.area() * 1.2D, 10.0D, 220.0D);
            for (int i = 0; i < count; i++) {
                Vec3 p = this.randomPoint(false);
                if (this.spared(p.x, p.z)) {
                    continue;
                }
                double rise = 0.05D + this.random.nextDouble() * (style == TOLL ? 0.06D : 0.22D);
                switch (i % 4) {
                    case 0 -> this.level.addParticle(ParticleTypes.SMOKE, p.x, p.y + 0.2D, p.z, 0.0D, rise, 0.0D);
                    case 1 -> this.level.addParticle(CRIMSON, p.x, p.y + 0.3D + this.random.nextDouble() * 2.0D, p.z, 0.0D,
                            0.0D, 0.0D);
                    default -> this.level.addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.1D, p.z, 0.0D, rise, 0.0D);
                }
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.shapeBounds().expandTowards(0.0D, 16.0D, 0.0D).inflate(0.5D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0D * 160.0D;
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
