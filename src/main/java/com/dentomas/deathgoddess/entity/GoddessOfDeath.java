package com.dentomas.deathgoddess.entity;

import com.dentomas.deathgoddess.registry.ModSounds;
import com.mojang.math.Vector3f;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The boss. Her fight is a small state machine: one action at a time, each with a fixed length that
 * matches a keyframe animation on the client, and effects that land on specific ticks of it.
 *
 * <p>Every attack gives at least a second of warning before it can hurt: ground magic is drawn as a
 * {@link DeathSigil} first, thrown magic is slow or hangs in the air before it flies.
 */
public class GoddessOfDeath extends Monster {
    public static final int DEATH_TICKS = 100;

    private static final int SWEEP_HIT = 20;
    private static final int SWEEP_BACKSWING = 27;
    public static final int GAZE_RELEASE = 40;
    private static final int STRINGS_PULL = 20;
    private static final int VEIL_STEP_JUMP = 14;
    private static final int PHASE_BURST = 30;
    /** How many of her last attacks she will not repeat. */
    private static final int MEMORY = 4;

    private static final UUID RAGE_SPEED_ID =
            UUID.nameUUIDFromBytes("deathgoddess:rage_speed".getBytes(StandardCharsets.UTF_8));
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.44F, 0.05F, 0.12F), 1.3F);
    private static final DustParticleOptions THREAD = new DustParticleOptions(new Vector3f(0.05F, 0.03F, 0.04F), 0.6F);

    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(GoddessOfDeath.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENRAGED =
            SynchedEntityData.defineId(GoddessOfDeath.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState idleAnim = new AnimationState();
    public final AnimationState deathAnim = new AnimationState();
    /** One per action, indexed by ordinal. */
    public final AnimationState[] actionAnims = new AnimationState[GoddessAction.VALUES.length];

    /** Runs on both sides; replaces deathTime so the renderer does not tint or topple her. */
    public int dyingTicks;
    /** Client-side tick counter of the current action, for particles. */
    private int clientActionTicks;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);
    private final int[] actionCooldowns = new int[GoddessAction.VALUES.length];
    private final ArrayDeque<GoddessAction> recent = new ArrayDeque<>();
    private int actionTicks;
    private int cooldown = 30;
    private float castYaw;

    public GoddessOfDeath(EntityType<? extends GoddessOfDeath> type, Level level) {
        super(type, level);
        for (int i = 0; i < this.actionAnims.length; i++) {
            this.actionAnims[i] = new AnimationState();
        }
        this.xpReward = 150;
        this.maxUpStep = 1.25F;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTION, 0);
        this.entityData.define(ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ApproachGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // No line-of-sight requirement: the eyes are always watching.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public GoddessAction getAction() {
        return GoddessAction.byId(this.entityData.get(ACTION));
    }

    public boolean isEnraged() {
        return this.entityData.get(ENRAGED);
    }

    // ------------------------------------------------------------------ fight

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        for (int i = 0; i < this.actionCooldowns.length; i++) {
            if (this.actionCooldowns[i] > 0) {
                this.actionCooldowns[i]--;
            }
        }
        if (this.tickCount % 5 == 0) {
            // Nothing lives where she passes.
            DeathMagic.blightDisc(this.level, this, this.position(), 2.6D);
            DeathMagic.reapNear(this, 2.5D);
        }

        GoddessAction action = this.getAction();
        LivingEntity target = this.getTarget();
        if (action != GoddessAction.NONE) {
            this.actionTicks++;
            this.getNavigation().stop();
            if (target != null && action != GoddessAction.SPAWN && action != GoddessAction.PHASE
                    && action != GoddessAction.DANSE_MACABRE) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            this.tickAction(action, this.actionTicks, target);
            if (this.actionTicks >= action.duration) {
                this.endAction();
            }
            return;
        }

        if (!this.isEnraged() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.startAction(GoddessAction.PHASE);
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        } else if (target != null && target.isAlive()) {
            this.chooseAction(target);
        }
    }

    private void startAction(GoddessAction action) {
        this.actionTicks = 0;
        this.castYaw = this.aimYaw();
        this.entityData.set(ACTION, action.ordinal());
        switch (action) {
            case SPAWN -> this.playSound(ModSounds.SPAWN.get(), 3.0F, 1.0F);
            case PHASE -> this.playSound(ModSounds.PHASE.get(), 4.0F, 1.0F);
            case GAZE -> this.playSound(ModSounds.GAZE_CHARGE.get(), 3.0F, 1.0F);
            case VEIL_STEP -> this.playSound(ModSounds.VEIL_STEP.get(), 1.5F, 1.0F);
            case GRAVE_BLOOM, CROSS, MARK, LATTICE, BLACK_RAIN -> this.playSound(ModSounds.CAST_SIGIL.get(), 2.5F, 1.0F);
            case DEATH_KNELL -> this.playSound(ModSounds.CAST_SIGIL.get(), 2.5F, 0.75F);
            case SEVERANCE, CRESCENTS -> this.playSound(ModSounds.CAST_BLADE.get(), 2.5F, 1.0F);
            case DANSE_MACABRE -> this.playSound(ModSounds.DANSE.get(), 3.0F, 1.0F);
            case REQUIEM -> this.playSound(ModSounds.REQUIEM_CHARGE.get(), 5.0F, 1.0F);
            default -> {
            }
        }
    }

    private void endAction() {
        this.entityData.set(ACTION, GoddessAction.NONE.ordinal());
        this.actionTicks = 0;
        // Just long enough to read as a breath between two phrases of the same dance.
        this.cooldown = (this.isEnraged() ? 4 : 10) + this.random.nextInt(10);
    }

    private void chooseAction(LivingEntity target) {
        double dist = this.distanceTo(target);
        boolean sees = this.hasLineOfSight(target);
        boolean rage = this.isEnraged();
        Map<GoddessAction, Integer> weights = new EnumMap<>(GoddessAction.class);
        weights.put(GoddessAction.SWEEP, dist < 5.5D ? 6 : 0);
        weights.put(GoddessAction.EYES, sees ? 2 : 0);
        weights.put(GoddessAction.GAZE, dist < 22.0D ? 2 : 0);
        weights.put(GoddessAction.STRINGS, sees && dist > 9.0D && dist < 28.0D ? 2 : 0);
        weights.put(GoddessAction.GRAVE_BLOOM, 3);
        weights.put(GoddessAction.SEVERANCE, dist < 26.0D ? 3 : 0);
        weights.put(GoddessAction.DEATH_KNELL, dist < 20.0D ? 2 : 0);
        weights.put(GoddessAction.BLACK_RAIN, 3);
        weights.put(GoddessAction.CROSS, 3);
        weights.put(GoddessAction.MARK, 3);
        weights.put(GoddessAction.CRESCENTS, sees && dist < 34.0D ? 3 : 0);
        weights.put(GoddessAction.SOUL_LANCES, sees ? 3 : 0);
        // Her answer to a target that runs or hides: step through the veil to casting range behind them.
        weights.put(GoddessAction.VEIL_STEP, dist > 20.0D || !sees ? 9 : rage ? 1 : 0);
        if (rage) {
            weights.put(GoddessAction.LATTICE, 3);
            weights.put(GoddessAction.DANSE_MACABRE, dist < 26.0D ? 2 : 0);
            weights.put(GoddessAction.REQUIEM, dist < 20.0D ? 4 : 0);
        }
        weights.replaceAll((action, weight) -> this.actionCooldowns[action.ordinal()] > 0 ? 0 : weight);
        GoddessAction picked = pick(weights, this.recent, this.random.nextInt(1 << 16));
        if (picked == GoddessAction.NONE) {
            return;
        }
        this.recent.addLast(picked);
        if (this.recent.size() > MEMORY) {
            this.recent.removeFirst();
        }
        this.actionCooldowns[picked.ordinal()] = switch (picked) {
            case GAZE -> 240;
            case STRINGS, BLACK_RAIN -> 160;
            case DEATH_KNELL, LATTICE -> 200;
            case DANSE_MACABRE -> 300;
            case REQUIEM -> 500;
            case VEIL_STEP -> 120;
            default -> 0;
        };
        this.startAction(picked);
    }

    /** Weighted pick that never returns one of the recent actions unless nothing else is possible. */
    private static GoddessAction pick(Map<GoddessAction, Integer> weights, ArrayDeque<GoddessAction> recent, int roll) {
        for (int pass = 0; pass < 2; pass++) {
            int total = 0;
            for (Map.Entry<GoddessAction, Integer> entry : weights.entrySet()) {
                if (pass == 1 || !recent.contains(entry.getKey())) {
                    total += entry.getValue();
                }
            }
            if (total == 0) {
                continue;
            }
            int left = roll % total;
            for (Map.Entry<GoddessAction, Integer> entry : weights.entrySet()) {
                if (pass == 1 || !recent.contains(entry.getKey())) {
                    left -= entry.getValue();
                    if (left < 0) {
                        return entry.getKey();
                    }
                }
            }
        }
        return GoddessAction.NONE;
    }

    private void tickAction(GoddessAction action, int tick, @Nullable LivingEntity target) {
        boolean rage = this.isEnraged();
        switch (action) {
            case SPAWN -> {
                this.particles(ParticleTypes.LARGE_SMOKE, this.position().add(0.0D, 0.3D, 0.0D), 3, 0.5D, 0.2D, 0.01D);
                if (tick == 44) {
                    this.witherAround(7.0D);
                }
            }
            case PHASE -> {
                if (tick < PHASE_BURST) {
                    this.particles(CRIMSON, this.position().add(0.0D, 2.2D, 0.0D), 4, 0.5D, 1.0D, 0.0D);
                } else if (tick == PHASE_BURST) {
                    this.phaseBurst();
                }
            }
            case SWEEP -> {
                if (tick >= 5 && tick < SWEEP_HIT - 2 && target != null) {
                    // Glide into reach while the arms draw back.
                    Vec3 to = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (to.length() > 3.0D) {
                        Vec3 glide = to.normalize().scale(0.42D);
                        this.setDeltaMovement(glide.x, this.getDeltaMovement().y, glide.z);
                    }
                }
                if (tick == SWEEP_HIT - 6 || (tick == SWEEP_BACKSWING - 5 && rage)) {
                    this.playSound(ModSounds.SWEEP.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
                }
                if (tick == SWEEP_HIT) {
                    this.sweepHit(1.0F);
                } else if (tick == SWEEP_BACKSWING && rage) {
                    this.sweepHit(0.7F);
                }
            }
            case EYES -> {
                if (tick >= 10 && tick <= (rage ? 34 : 22) && tick % 4 == 2 && target != null) {
                    this.launchEye(target);
                }
            }
            case GAZE -> {
                if (tick == GAZE_RELEASE) {
                    this.gazeRelease();
                }
            }
            case STRINGS -> {
                if (tick == 4) {
                    this.playSound(ModSounds.STRINGS.get(), 1.6F, 1.0F);
                }
                if (target != null && tick >= 4 && tick < STRINGS_PULL && tick % 4 == 0) {
                    // The threads are visible on their way out, before they tighten.
                    this.particleLine(THREAD, this.position().add(0.0D, 2.6D, 0.0D), target.position().add(0.0D, 1.0D, 0.0D), 0.4D);
                }
                if (tick == STRINGS_PULL && target != null) {
                    this.pull(target);
                }
            }
            case VEIL_STEP -> {
                if (tick < VEIL_STEP_JUMP + 6) {
                    this.particles(ParticleTypes.LARGE_SMOKE, this.position().add(0.0D, 1.8D, 0.0D), 6, 0.3D, 1.2D, 0.01D);
                }
                if (tick == VEIL_STEP_JUMP && target != null) {
                    this.veilStep(target);
                }
            }
            case GRAVE_BLOOM -> {
                if (tick == 8) {
                    this.graveBloom(rage ? 8 : 5);
                }
            }
            case SEVERANCE -> {
                if (tick == 6) {
                    this.sever(rage ? new float[]{0.0F, -24.0F, 24.0F} : new float[]{0.0F});
                } else if (tick == 16) {
                    this.sever(rage ? new float[]{-12.0F, 12.0F, -36.0F, 36.0F} : new float[]{0.0F});
                }
            }
            case DEATH_KNELL -> {
                int wave = (tick - 6) / 8;
                if (tick >= 6 && (tick - 6) % 8 == 0 && wave < (rage ? 5 : 4)) {
                    float inner = wave * 5.0F;
                    if (wave == 0) {
                        DeathSigil.disc(this.level, this, this.position(), 5.0F, 24, this.power(10.0F), DeathSigil.TOLL);
                    } else {
                        DeathSigil.ring(this.level, this, this.position(), inner, inner + 5.0F, 24, this.power(10.0F),
                                DeathSigil.TOLL);
                    }
                }
            }
            case BLACK_RAIN -> {
                int every = rage ? 2 : 3;
                if (tick >= 8 && tick <= 52 && (tick - 8) % every == 0) {
                    this.rainDrop((tick - 8) / every, target);
                }
            }
            case CROSS -> {
                if (tick == 6) {
                    this.cross(0.0F);
                } else if (tick == 18 && rage) {
                    this.cross(45.0F);
                }
            }
            case MARK -> {
                if (tick == 6 || (tick == 18 && rage)) {
                    this.markTargets(target);
                }
            }
            case LATTICE -> {
                if (tick == 6) {
                    this.lattice(0.0F);
                } else if (tick == 22) {
                    this.lattice(3.0F);
                }
            }
            case CRESCENTS -> {
                if (tick == 20) {
                    this.crescents(rage ? new float[]{0.0F, -14.0F, 14.0F, -28.0F, 28.0F} : new float[]{0.0F, -16.0F, 16.0F});
                } else if (tick == 32) {
                    this.crescents(rage ? new float[]{-7.0F, 7.0F, -21.0F, 21.0F} : new float[]{-8.0F, 8.0F});
                }
            }
            case SOUL_LANCES -> {
                int count = rage ? 8 : 5;
                int index = (tick - 6) / 4;
                if (tick >= 6 && (tick - 6) % 4 == 0 && index < count) {
                    this.formLance(index, count, target);
                }
            }
            case DANSE_MACABRE -> {
                if (tick >= 20 && tick <= 70 && tick % 2 == 0) {
                    this.danseStep(tick - 20, rage ? 4 : 3);
                }
            }
            case REQUIEM -> {
                if (tick == 10) {
                    this.requiem(target);
                }
            }
            default -> {
            }
        }
    }

    private float power(float damage) {
        return this.isEnraged() ? damage * 1.2F : damage;
    }

    /** Where her magic is aimed: the target, or straight ahead when she has none (previews). */
    private Vec3 aimPoint() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            return target.position();
        }
        return this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(9.0D));
    }

    private float aimYaw() {
        Vec3 to = this.aimPoint().subtract(this.position());
        return (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);
    }

    private static Vec3 direction(float yaw) {
        return Vec3.directionFromRotation(0.0F, yaw);
    }

    private void witherAround(double radius) {
        if (DeathMagic.blightDisc(this.level, this, this.position(), radius) > 6) {
            this.playSound(ModSounds.WITHER.get(), 1.5F, 1.0F);
        }
        DeathMagic.reapNear(this, radius);
    }

    // ---- close range

    private void sweepHit(float scale) {
        Vec3 facing = direction(this.yBodyRot);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale;
        AABB area = this.getBoundingBox().inflate(5.4D, 1.5D, 5.4D);
        for (LivingEntity victim : this.level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            Vec3 to = victim.position().subtract(this.position());
            double flat = to.horizontalDistance();
            // Six arms cover almost a half circle in front of her.
            boolean inArc = flat < 1.2D || to.multiply(1.0D, 0.0D, 1.0D).normalize().dot(facing) > 0.1D;
            if (flat <= 5.4D && inArc && DeathMagic.strike(this, this, victim, damage) && victim.isAlive()) {
                victim.knockback(1.1D, -to.x, -to.z);
            }
        }
        this.particles(ParticleTypes.SWEEP_ATTACK, this.position().add(facing.scale(2.4D)).add(0.0D, 1.6D, 0.0D), 6, 1.4D, 0.6D, 0.0D);
        DeathMagic.blightDisc(this.level, this, this.position().add(facing.scale(2.5D)), 4.0D);
    }

    private void pull(LivingEntity target) {
        if (!this.hasLineOfSight(target) || this.distanceTo(target) > 30.0F) {
            return;
        }
        Vec3 to = this.position().subtract(target.position());
        double flat = to.horizontalDistance();
        Vec3 dir = to.multiply(1.0D, 0.0D, 1.0D).normalize();
        double strength = Math.min(2.6D, 0.4D + flat * 0.12D);
        if (DeathMagic.strike(this, this, target, 4.0F) && target.isAlive()) {
            target.setDeltaMovement(dir.x * strength, 0.45D, dir.z * strength);
            target.hurtMarked = true;
        }
        this.particleLine(THREAD, this.position().add(0.0D, 2.6D, 0.0D), target.position().add(0.0D, 1.0D, 0.0D), 0.25D);
    }

    private void veilStep(LivingEntity target) {
        // Reappear at casting distance, behind the target's back.
        Vec3 behind = direction(target.getYRot()).scale(-9.0D);
        for (int attempt = 0; attempt < 10; attempt++) {
            double spread = attempt == 0 ? 0.0D : 6.0D;
            double x = target.getX() + behind.x + (this.random.nextDouble() - 0.5D) * spread;
            double z = target.getZ() + behind.z + (this.random.nextDouble() - 0.5D) * spread;
            if (this.randomTeleport(x, target.getY(), z, false)) {
                break;
            }
        }
        this.lookAt(target, 180.0F, 30.0F);
        this.yBodyRot = this.getYRot();
        this.playSound(ModSounds.VEIL_STEP.get(), 1.5F, 1.25F);
    }

    // ---- the eyes

    private Vec3 haloCenter() {
        Vec3 back = direction(this.yBodyRot).scale(-0.4D);
        return this.position().add(back.x, 3.55D, back.z);
    }

    private void launchEye(LivingEntity target) {
        Vec3 right = direction(this.yBodyRot + 90.0F);
        double side = (this.random.nextDouble() - 0.5D) * 2.8D;
        Vec3 from = this.haloCenter().add(right.scale(side)).add(0.0D, this.random.nextDouble() * 1.3D - 0.2D, 0.0D);
        WatchingEye eye = new WatchingEye(this.level, this, target, this.isEnraged() ? 0.95F : 0.8F);
        eye.setPos(from.x, from.y, from.z);
        eye.setDeltaMovement(right.scale(side * 0.1D).add(0.0D, 0.12D, 0.0D));
        this.level.addFreshEntity(eye);
        this.playSound(ModSounds.EYE_CAST.get(), 1.2F, 0.85F + this.random.nextFloat() * 0.3F);
    }

    /** Everything the halo can see is punished; breaking line of sight is the only defence. */
    private void gazeRelease() {
        this.playSound(ModSounds.GAZE_RELEASE.get(), 3.0F, 1.0F);
        Vec3 halo = this.haloCenter();
        this.particles(CRIMSON, halo, 60, 1.4D, 1.0D, 0.0D);
        float damage = this.power(14.0F);
        AABB area = this.getBoundingBox().inflate(24.0D);
        for (LivingEntity victim : this.level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            if (!this.hasLineOfSight(victim)) {
                continue;
            }
            boolean player = victim instanceof Player;
            if (DeathMagic.strike(this, this, victim, damage) && player) {
                victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160), this);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
                this.particleLine(CRIMSON, halo, victim.getEyePosition(), 0.5D);
            }
        }
        this.witherAround(10.0D);
    }

    // ---- ground magic

    private void graveBloom(int satellites) {
        Vec3 center = this.aimPoint();
        DeathSigil.disc(this.level, this, center, 3.2F, 28, this.power(10.0F), DeathSigil.PILLAR);
        for (int i = 0; i < satellites; i++) {
            double angle = (i + this.random.nextDouble()) / satellites * Math.PI * 2.0D;
            double distance = 4.5D + this.random.nextDouble() * 5.5D;
            Vec3 at = center.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
            DeathSigil sigil = DeathSigil.disc(this.level, this, at, 2.4F, 30 + i * 2, this.power(10.0F), DeathSigil.PILLAR);
            if (i % 3 != 0) {
                sigil.quiet();
            }
        }
    }

    private void sever(float[] offsets) {
        float yaw = this.aimYaw();
        for (int i = 0; i < offsets.length; i++) {
            DeathSigil sigil = DeathSigil.line(this.level, this, this.position(), yaw + offsets[i], 28.0F, 2.6F, 26,
                    this.power(12.0F), DeathSigil.BLADE);
            if (i > 0) {
                sigil.quiet();
            }
        }
    }

    private void rainDrop(int index, @Nullable LivingEntity target) {
        Vec3 at;
        if (target != null && index % 4 == 0) {
            // Lead the target slightly so standing still or running straight are both punished.
            at = target.position().add(target.getDeltaMovement().multiply(8.0D, 0.0D, 8.0D));
        } else {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(this.random.nextDouble()) * 10.0D;
            at = this.aimPoint().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
        }
        DeathSigil sigil = DeathSigil.disc(this.level, this, at, 2.2F, 24, this.power(7.0F), DeathSigil.RAIN);
        if (index % 2 != 0) {
            sigil.quiet();
        }
    }

    private void cross(float turn) {
        Vec3 center = this.aimPoint();
        float yaw = this.castYaw + turn;
        for (int i = 0; i < 2; i++) {
            float lineYaw = yaw + i * 90.0F;
            Vec3 from = center.subtract(direction(lineYaw).scale(16.0D));
            DeathSigil sigil = DeathSigil.line(this.level, this, new Vec3(from.x, center.y, from.z), lineYaw, 32.0F, 3.0F, 28,
                    this.power(11.0F), DeathSigil.PILLAR);
            if (i > 0) {
                sigil.quiet();
            }
        }
    }

    private void markTargets(@Nullable LivingEntity target) {
        List<LivingEntity> marked = new ArrayList<>();
        for (Player player : this.level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0D),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            marked.add(player);
        }
        if (target != null && target.isAlive() && !marked.contains(target)) {
            marked.add(target);
        }
        if (marked.isEmpty()) {
            DeathSigil.disc(this.level, this, this.aimPoint(), 2.6F, 40, this.power(12.0F), DeathSigil.MARK);
        }
        for (LivingEntity victim : marked) {
            // Follows for a second, then stays put for the last one.
            DeathSigil.disc(this.level, this, victim.position(), 2.6F, 40, this.power(12.0F), DeathSigil.MARK).track(victim, 20);
        }
    }

    /** Parallel walls with gaps exactly as wide as the walls; the second set fills the gaps. */
    private void lattice(float shift) {
        Vec3 center = this.aimPoint();
        Vec3 along = direction(this.castYaw);
        Vec3 across = direction(this.castYaw + 90.0F);
        for (int k = -3; k <= 3; k++) {
            Vec3 from = center.add(across.scale(k * 6.0D + shift)).subtract(along.scale(20.0D));
            DeathSigil sigil = DeathSigil.line(this.level, this, new Vec3(from.x, center.y, from.z), this.castYaw, 40.0F, 3.0F,
                    28, this.power(10.0F), DeathSigil.PILLAR);
            if (k != 0) {
                sigil.quiet();
            }
        }
    }

    private void requiem(@Nullable LivingEntity target) {
        List<Vec3> refuges = new ArrayList<>();
        // One refuge within a short run of the target, one elsewhere.
        Vec3 near = target != null ? target.position() : this.aimPoint();
        for (int i = 0; i < 2; i++) {
            Vec3 spot = this.position();
            for (int attempt = 0; attempt < 12; attempt++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double distance = i == 0 ? 3.5D + this.random.nextDouble() * 3.0D : 8.0D + this.random.nextDouble() * 8.0D;
                Vec3 origin = i == 0 ? near : this.position();
                spot = origin.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
                double fromHer = spot.subtract(this.position()).horizontalDistance();
                if (fromHer > 5.0D && fromHer < 17.0D && (refuges.isEmpty() || refuges.get(0).distanceTo(spot) > 8.0D)) {
                    break;
                }
            }
            refuges.add(DeathMagic.ground(this.level, spot));
        }
        for (Vec3 refuge : refuges) {
            DeathSigil.disc(this.level, this, refuge, 3.2F, 40, 0.0F, DeathSigil.SAFE);
        }
        DeathSigil.disc(this.level, this, this.position(), 22.0F, 40, this.power(22.0F), DeathSigil.REQUIEM).spare(refuges, 3.2F);
    }

    // ---- thrown magic

    private void crescents(float[] offsets) {
        float yaw = this.aimYaw();
        Vec3 from = this.position().add(0.0D, 0.4D, 0.0D);
        for (float offset : offsets) {
            Vec3 dir = direction(yaw + offset);
            DeathBolt.crescent(this.level, this, from.add(dir.scale(1.4D)), dir, 0.85F, this.power(9.0F));
        }
        this.playSound(ModSounds.BLADE_WAVE.get(), 2.0F, 1.1F);
    }

    private void formLance(int index, int count, @Nullable LivingEntity target) {
        Vec3 right = direction(this.yBodyRot + 90.0F);
        double side = index - (count - 1) / 2.0D;
        Vec3 at = this.haloCenter().add(right.scale(side * 1.15D)).add(0.0D, 1.5D - Math.abs(side) * 0.3D, 0.0D);
        Vec3 fallback = this.aimPoint().add(0.0D, 1.0D, 0.0D).subtract(at);
        DeathBolt.lance(this.level, this, at, target, fallback, 26, 2.2F, this.power(7.0F));
        this.playSound(ModSounds.LANCE_FORM.get(), 1.2F, 0.8F + index * 0.06F);
    }

    private void danseStep(int step, int arms) {
        Vec3 from = this.position().add(0.0D, 1.3D, 0.0D);
        for (int arm = 0; arm < arms; arm++) {
            Vec3 dir = direction(this.castYaw + step * 7.0F + arm * 360.0F / arms);
            DeathBolt.orb(this.level, this, from.add(dir.scale(1.2D)), dir, 0.4F, this.power(6.0F));
        }
    }

    // ---- phase change

    private void phaseBurst() {
        this.entityData.set(ENRAGED, true);
        this.applyRage();
        this.particles(ParticleTypes.LARGE_SMOKE, this.position().add(0.0D, 2.0D, 0.0D), 80, 1.5D, 1.5D, 0.15D);
        this.particles(CRIMSON, this.position().add(0.0D, 2.0D, 0.0D), 120, 3.0D, 1.5D, 0.0D);
        for (LivingEntity victim : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(8.0D),
                e -> e != this && e.isAlive())) {
            Vec3 away = victim.position().subtract(this.position());
            if (DeathMagic.strike(this, this, victim, 6.0F) && victim.isAlive()) {
                victim.knockback(2.0D, -away.x, -away.z);
                victim.hurtMarked = true;
            }
        }
        this.witherAround(12.0D);
    }

    private void applyRage() {
        this.bossEvent.setCreateWorldFog(true);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(RAGE_SPEED_ID) == null) {
            speed.addPermanentModifier(new AttributeModifier(RAGE_SPEED_ID, "Goddess rage", 0.3D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private void particles(ParticleOptions type, Vec3 at, int count, double spreadXZ, double spreadY, double speed) {
        if (this.level instanceof ServerLevel server) {
            server.sendParticles(type, at.x, at.y, at.z, count, spreadXZ, spreadY, spreadXZ, speed);
        }
    }

    private void particleLine(ParticleOptions type, Vec3 from, Vec3 to, double step) {
        if (this.level instanceof ServerLevel server) {
            Vec3 delta = to.subtract(from);
            int count = Math.min(120, (int) (delta.length() / step));
            for (int i = 0; i <= count; i++) {
                Vec3 p = from.add(delta.scale(i / (double) Math.max(1, count)));
                server.sendParticles(type, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            if (this.isAlive()) {
                this.idleAnim.startIfStopped(this.tickCount);
            }
            if (this.getAction() != GoddessAction.NONE) {
                this.clientActionTicks++;
            }
            this.clientEffects();
        } else if (this.isAlive() && this.tickCount % 34 == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 0.001D) {
            this.playSound(ModSounds.GLIDE.get(), 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void clientEffects() {
        Vec3 back = direction(this.yBodyRot).scale(-0.45D);
        Vec3 right = direction(this.yBodyRot + 90.0F);
        double hx = this.getX() + back.x;
        double hy = this.getY() + 3.55D;
        double hz = this.getZ() + back.z;
        GoddessAction action = this.getAction();
        if (this.isAlive() && this.tickCount % (this.isEnraged() ? 2 : 4) == 0) {
            // Smoke curling between the eyes.
            double side = (this.random.nextDouble() - 0.5D) * 3.0D;
            this.level.addParticle(ParticleTypes.SMOKE, hx + right.x * side, hy + this.random.nextDouble() * 1.4D - 0.4D,
                    hz + right.z * side, 0.0D, 0.01D, 0.0D);
        }
        boolean gazing = action == GoddessAction.GAZE && this.clientActionTicks < GAZE_RELEASE;
        boolean requiem = action == GoddessAction.REQUIEM && this.clientActionTicks < 50;
        if (gazing || requiem) {
            // Light being drawn into the halo while the spell builds.
            for (int i = 0; i < 2; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double radius = 2.5D + this.random.nextDouble() * 1.5D;
                double px = hx + right.x * Math.cos(angle) * radius;
                double py = hy + Math.sin(angle) * radius;
                double pz = hz + right.z * Math.cos(angle) * radius;
                this.level.addParticle(CRIMSON, px, py, pz, 0.0D, 0.0D, 0.0D);
            }
        } else if (action != GoddessAction.NONE && action != GoddessAction.SWEEP && action != GoddessAction.SPAWN) {
            // Ash and embers lifting off the ground around her while she casts.
            boolean ember = this.random.nextBoolean();
            this.level.addParticle(ember ? CRIMSON : ParticleTypes.SMOKE, this.getRandomX(1.8D),
                    this.getY() + this.random.nextDouble() * (ember ? 2.5D : 0.6D), this.getRandomZ(1.8D), 0.0D, 0.05D, 0.0D);
        }
        if (this.dyingTicks > 0 && this.dyingTicks % 2 == 0) {
            this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.8D), this.getY() + this.random.nextDouble() * 2.5D,
                    this.getRandomZ(0.8D), 0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ACTION.equals(key) && this.level.isClientSide) {
            this.clientActionTicks = 0;
            this.stopActionAnimations();
            GoddessAction action = this.getAction();
            if (action != GoddessAction.NONE) {
                this.actionAnims[action.ordinal()].start(this.tickCount);
            }
        }
    }

    private void stopActionAnimations() {
        for (AnimationState state : this.actionAnims) {
            state.stop();
        }
    }

    @Override
    protected void tickDeath() {
        this.dyingTicks++;
        if (this.level.isClientSide) {
            if (this.dyingTicks == 1) {
                this.stopActionAnimations();
                this.idleAnim.stop();
                this.deathAnim.start(this.tickCount);
            }
        } else if (this.dyingTicks >= DEATH_TICKS && !this.isRemoved()) {
            this.level.broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level.isClientSide) {
            this.entityData.set(ACTION, GoddessAction.NONE.ordinal());
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ boss plumbing

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.startAction(GoddessAction.SPAWN);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (!source.isBypassInvul()) {
            GoddessAction action = this.getAction();
            boolean vanished = action == GoddessAction.VEIL_STEP && this.actionTicks >= 6 && this.actionTicks <= 20;
            if (action == GoddessAction.SPAWN || action == GoddessAction.PHASE || vanished) {
                return true;
            }
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.WITHER && effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.isEnraged());
        // Read-only: what she is doing right now, for /data get and datapacks.
        tag.putString("CurrentAction", this.getAction().id());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Enraged")) {
            this.entityData.set(ENRAGED, true);
            this.applyRage();
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        // Never saved; lets /data merge trigger an action by name, e.g. {Action:"requiem"}, to preview or script her.
        if (tag.contains("Action") && !this.level.isClientSide && this.isAlive()) {
            GoddessAction action = tag.contains("Action", Tag.TAG_STRING) ? GoddessAction.byName(tag.getString("Action"))
                    : GoddessAction.byId(tag.getInt("Action"));
            this.startAction(action);
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getAction() == GoddessAction.NONE ? ModSounds.AMBIENT.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 3.45F;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // The halo, arms and train reach well past the hitbox.
        return this.getBoundingBox().inflate(2.6D, 1.0D, 2.6D);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** She is a caster now: drifts to mid range with a clear view, and holds there. */
    private static class ApproachGoal extends Goal {
        private static final double HOLD_RANGE = 12.0D;
        private final GoddessOfDeath goddess;
        private int repath;

        ApproachGoal(GoddessOfDeath goddess) {
            this.goddess = goddess;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.goddess.getTarget();
            return target != null && target.isAlive() && this.goddess.getAction() == GoddessAction.NONE;
        }

        @Override
        public void stop() {
            this.goddess.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.goddess.getTarget();
            if (target == null) {
                return;
            }
            this.goddess.getLookControl().setLookAt(target, 30.0F, 30.0F);
            boolean inPlace = this.goddess.distanceToSqr(target) < HOLD_RANGE * HOLD_RANGE && this.goddess.hasLineOfSight(target);
            if (inPlace) {
                this.goddess.getNavigation().stop();
            } else if (--this.repath <= 0) {
                this.repath = 8 + this.goddess.getRandom().nextInt(6);
                this.goddess.getNavigation().moveTo(target, 1.0D);
            }
        }
    }
}
