package com.dentomas.deathgoddess.entity;

import com.dentomas.deathgoddess.registry.ModSounds;
import com.mojang.math.Vector3f;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
 */
public class GoddessOfDeath extends Monster {
    public static final int NONE = 0;
    public static final int SPAWN = 1;
    public static final int SWEEP = 2;
    public static final int EYES = 3;
    public static final int GAZE = 4;
    public static final int STRINGS = 5;
    public static final int VEIL_STEP = 6;
    public static final int PHASE = 7;
    /** Ticks per action; must equal the matching animation length in GoddessAnimations. */
    private static final int[] DURATION = {0, 70, 34, 44, 80, 40, 36, 70};
    public static final int DEATH_TICKS = 100;

    private static final int SWEEP_HIT = 14;
    private static final int SWEEP_BACKSWING = 24;
    private static final int GAZE_RELEASE = 56;
    private static final int STRINGS_PULL = 14;
    private static final int VEIL_STEP_JUMP = 16;
    private static final int PHASE_BURST = 30;

    private static final UUID RAGE_SPEED_ID =
            UUID.nameUUIDFromBytes("deathgoddess:rage_speed".getBytes(StandardCharsets.UTF_8));
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.44F, 0.05F, 0.12F), 1.3F);
    private static final DustParticleOptions THREAD = new DustParticleOptions(new Vector3f(0.05F, 0.03F, 0.04F), 0.6F);

    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(GoddessOfDeath.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENRAGED =
            SynchedEntityData.defineId(GoddessOfDeath.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState idleAnim = new AnimationState();
    public final AnimationState spawnAnim = new AnimationState();
    public final AnimationState sweepAnim = new AnimationState();
    public final AnimationState eyesAnim = new AnimationState();
    public final AnimationState gazeAnim = new AnimationState();
    public final AnimationState stringsAnim = new AnimationState();
    public final AnimationState veilStepAnim = new AnimationState();
    public final AnimationState phaseAnim = new AnimationState();
    public final AnimationState deathAnim = new AnimationState();

    /** Runs on both sides; replaces deathTime so the renderer does not tint or topple her. */
    public int dyingTicks;
    /** Client-side tick counter of the current action, for particles. */
    private int clientActionTicks;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);
    private int actionTicks;
    private int cooldown = 40;
    private int gazeCooldown = 200;
    private int stringsCooldown = 100;
    private int veilStepCooldown;

    public GoddessOfDeath(EntityType<? extends GoddessOfDeath> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.maxUpStep = 1.25F;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 350.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTION, NONE);
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

    public int getAction() {
        return this.entityData.get(ACTION);
    }

    public boolean isEnraged() {
        return this.entityData.get(ENRAGED);
    }

    // ------------------------------------------------------------------ fight

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.gazeCooldown > 0) {
            this.gazeCooldown--;
        }
        if (this.stringsCooldown > 0) {
            this.stringsCooldown--;
        }
        if (this.veilStepCooldown > 0) {
            this.veilStepCooldown--;
        }

        int action = this.getAction();
        LivingEntity target = this.getTarget();
        if (action != NONE) {
            this.actionTicks++;
            this.getNavigation().stop();
            if (target != null && action != SPAWN && action != PHASE) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            this.tickAction(action, this.actionTicks, target);
            if (this.actionTicks >= DURATION[action]) {
                this.endAction(action);
            }
            return;
        }

        if (!this.isEnraged() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.startAction(PHASE);
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        } else if (target != null && target.isAlive()) {
            this.chooseAction(target);
        }
    }

    private void startAction(int action) {
        this.actionTicks = 0;
        this.entityData.set(ACTION, action);
        switch (action) {
            case SPAWN -> this.playSound(ModSounds.SPAWN.get(), 3.0F, 1.0F);
            case GAZE -> this.playSound(ModSounds.GAZE_CHARGE.get(), 3.0F, 1.0F);
            case PHASE -> this.playSound(ModSounds.PHASE.get(), 4.0F, 1.0F);
            case VEIL_STEP -> this.playSound(ModSounds.VEIL_STEP.get(), 1.5F, 1.0F);
            default -> {
            }
        }
    }

    private void endAction(int action) {
        this.entityData.set(ACTION, NONE);
        this.actionTicks = 0;
        boolean chained = action == STRINGS || action == VEIL_STEP;
        this.cooldown = chained ? 6 : (this.isEnraged() ? 16 : 30) + this.random.nextInt(18);
    }

    private void chooseAction(LivingEntity target) {
        double dist = this.distanceTo(target);
        boolean sees = this.hasLineOfSight(target);
        int sweep = dist < 4.6D ? 6 : 0;
        int eyes = sees && dist > 3.5D && dist < 30.0D ? 3 : 0;
        int strings = sees && dist > 6.0D && dist < 24.0D && this.stringsCooldown == 0 ? 3 : 0;
        int gaze = dist < 20.0D && this.gazeCooldown == 0 ? 3 : 0;
        int step = this.isEnraged() && dist > 5.0D && this.veilStepCooldown == 0 ? (sees ? 2 : 6) : 0;
        int total = sweep + eyes + strings + gaze + step;
        if (total == 0) {
            return;
        }
        int roll = this.random.nextInt(total);
        if ((roll -= sweep) < 0) {
            this.startAction(SWEEP);
        } else if ((roll -= eyes) < 0) {
            this.startAction(EYES);
        } else if ((roll -= strings) < 0) {
            this.stringsCooldown = 160;
            this.startAction(STRINGS);
        } else if ((roll -= gaze) < 0) {
            this.gazeCooldown = this.isEnraged() ? 260 : 400;
            this.startAction(GAZE);
        } else {
            this.veilStepCooldown = 140;
            this.startAction(VEIL_STEP);
        }
    }

    private void tickAction(int action, int tick, @Nullable LivingEntity target) {
        switch (action) {
            case SPAWN -> this.particles(ParticleTypes.LARGE_SMOKE, this.position().add(0.0D, 0.3D, 0.0D), 3, 0.5D, 0.2D, 0.01D);
            case SWEEP -> {
                if (tick == 8 || (tick == 19 && this.isEnraged())) {
                    this.playSound(ModSounds.SWEEP.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
                }
                if (tick == SWEEP_HIT) {
                    this.sweepHit(1.0F);
                } else if (tick == SWEEP_BACKSWING && this.isEnraged()) {
                    this.sweepHit(0.7F);
                }
            }
            case EYES -> {
                int last = this.isEnraged() ? 36 : 24;
                if (tick >= 16 && tick <= last && tick % 4 == 0 && target != null) {
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
            case PHASE -> {
                if (tick < PHASE_BURST) {
                    this.particles(CRIMSON, this.position().add(0.0D, 2.2D, 0.0D), 4, 0.5D, 1.0D, 0.0D);
                } else if (tick == PHASE_BURST) {
                    this.phaseBurst();
                }
            }
            default -> {
            }
        }
    }

    private void sweepHit(float scale) {
        Vec3 facing = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale;
        AABB area = this.getBoundingBox().inflate(5.2D, 1.5D, 5.2D);
        for (LivingEntity victim : this.level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            Vec3 to = victim.position().subtract(this.position());
            double flat = to.horizontalDistance();
            // Six arms cover almost a half circle in front of her.
            boolean inArc = flat < 1.2D || to.multiply(1.0D, 0.0D, 1.0D).normalize().dot(facing) > 0.1D;
            if (flat <= 5.2D && inArc && victim.hurt(DamageSource.mobAttack(this), damage)) {
                victim.knockback(1.1D, -to.x, -to.z);
                this.doEnchantDamageEffects(this, victim);
            }
        }
        this.particles(ParticleTypes.SWEEP_ATTACK, this.position().add(facing.scale(2.2D)).add(0.0D, 1.6D, 0.0D), 5, 1.2D, 0.6D, 0.0D);
    }

    private Vec3 haloCenter() {
        Vec3 back = Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(-0.4D);
        return this.position().add(back.x, 3.55D, back.z);
    }

    private void launchEye(LivingEntity target) {
        Vec3 right = Vec3.directionFromRotation(0.0F, this.yBodyRot + 90.0F);
        double side = (this.random.nextDouble() - 0.5D) * 2.8D;
        Vec3 from = this.haloCenter().add(right.scale(side)).add(0.0D, this.random.nextDouble() * 1.3D - 0.2D, 0.0D);
        WatchingEye eye = new WatchingEye(this.level, this, target, this.isEnraged() ? 0.9F : 0.7F);
        eye.setPos(from.x, from.y, from.z);
        eye.setDeltaMovement(right.scale(side * 0.08D).add(0.0D, 0.1D, 0.0D));
        this.level.addFreshEntity(eye);
        this.playSound(ModSounds.EYE_CAST.get(), 1.2F, 0.85F + this.random.nextFloat() * 0.3F);
    }

    /** Everything the halo can see is punished; breaking line of sight is the only defence. */
    private void gazeRelease() {
        this.playSound(ModSounds.GAZE_RELEASE.get(), 3.0F, 1.0F);
        Vec3 halo = this.haloCenter();
        this.particles(CRIMSON, halo, 60, 1.4D, 1.0D, 0.0D);
        float damage = this.isEnraged() ? 18.0F : 14.0F;
        LivingEntity target = this.getTarget();
        AABB area = this.getBoundingBox().inflate(22.0D);
        for (LivingEntity victim : this.level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && (e instanceof Player || e == target))) {
            if (victim instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            if (!this.hasLineOfSight(victim)) {
                continue;
            }
            victim.hurt(DamageSource.indirectMagic(this, this), damage);
            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200), this);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), this);
            this.particleLine(CRIMSON, halo, victim.getEyePosition(), 0.5D);
        }
    }

    private void pull(LivingEntity target) {
        if (!this.hasLineOfSight(target) || this.distanceTo(target) > 28.0F) {
            return;
        }
        Vec3 to = this.position().subtract(target.position());
        double flat = to.horizontalDistance();
        Vec3 dir = to.multiply(1.0D, 0.0D, 1.0D).normalize();
        double strength = Math.min(2.4D, 0.4D + flat * 0.12D);
        target.hurt(DamageSource.indirectMagic(this, this), 4.0F);
        target.setDeltaMovement(dir.x * strength, 0.45D, dir.z * strength);
        target.hurtMarked = true;
        this.particleLine(THREAD, this.position().add(0.0D, 2.6D, 0.0D), target.position().add(0.0D, 1.0D, 0.0D), 0.25D);
    }

    private void veilStep(LivingEntity target) {
        Vec3 behind = Vec3.directionFromRotation(0.0F, target.getYRot()).scale(-3.0D);
        for (int attempt = 0; attempt < 10; attempt++) {
            double spread = attempt == 0 ? 0.0D : 5.0D;
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

    private void phaseBurst() {
        this.entityData.set(ENRAGED, true);
        this.applyRage();
        this.particles(ParticleTypes.LARGE_SMOKE, this.position().add(0.0D, 2.0D, 0.0D), 80, 1.5D, 1.5D, 0.15D);
        this.particles(CRIMSON, this.position().add(0.0D, 2.0D, 0.0D), 120, 3.0D, 1.5D, 0.0D);
        for (LivingEntity victim : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(8.0D),
                e -> e != this && e.isAlive())) {
            Vec3 away = victim.position().subtract(this.position());
            if (victim.hurt(DamageSource.indirectMagic(this, this), 6.0F)) {
                victim.knockback(2.0D, -away.x, -away.z);
                victim.hurtMarked = true;
            }
        }
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
            if (this.getAction() != NONE) {
                this.clientActionTicks++;
            }
            this.clientEffects();
        } else if (this.isAlive() && this.tickCount % 34 == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 0.001D) {
            this.playSound(ModSounds.GLIDE.get(), 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void clientEffects() {
        Vec3 back = Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(-0.45D);
        Vec3 right = Vec3.directionFromRotation(0.0F, this.yBodyRot + 90.0F);
        double hx = this.getX() + back.x;
        double hy = this.getY() + 3.55D;
        double hz = this.getZ() + back.z;
        if (this.isAlive() && this.tickCount % (this.isEnraged() ? 2 : 4) == 0) {
            // Smoke curling between the eyes.
            double side = (this.random.nextDouble() - 0.5D) * 3.0D;
            this.level.addParticle(ParticleTypes.SMOKE, hx + right.x * side, hy + this.random.nextDouble() * 1.4D - 0.4D,
                    hz + right.z * side, 0.0D, 0.01D, 0.0D);
        }
        if (this.getAction() == GAZE && this.clientActionTicks < GAZE_RELEASE) {
            // Light being drawn into the halo while the gaze builds.
            for (int i = 0; i < 2; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double radius = 2.5D + this.random.nextDouble() * 1.5D;
                double px = hx + right.x * Math.cos(angle) * radius;
                double py = hy + Math.sin(angle) * radius;
                double pz = hz + right.z * Math.cos(angle) * radius;
                this.level.addParticle(CRIMSON, px, py, pz, 0.0D, 0.0D, 0.0D);
            }
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
            AnimationState next = switch (this.getAction()) {
                case SPAWN -> this.spawnAnim;
                case SWEEP -> this.sweepAnim;
                case EYES -> this.eyesAnim;
                case GAZE -> this.gazeAnim;
                case STRINGS -> this.stringsAnim;
                case VEIL_STEP -> this.veilStepAnim;
                case PHASE -> this.phaseAnim;
                default -> null;
            };
            if (next != null) {
                next.start(this.tickCount);
            }
        }
    }

    private void stopActionAnimations() {
        this.spawnAnim.stop();
        this.sweepAnim.stop();
        this.eyesAnim.stop();
        this.gazeAnim.stop();
        this.stringsAnim.stop();
        this.veilStepAnim.stop();
        this.phaseAnim.stop();
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
            this.entityData.set(ACTION, NONE);
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ boss plumbing

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.startAction(SPAWN);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (!source.isBypassInvul()) {
            int action = this.getAction();
            boolean vanished = action == VEIL_STEP && this.actionTicks >= 8 && this.actionTicks <= 24;
            if (action == SPAWN || action == PHASE || vanished) {
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
        // Never saved; lets /data merge trigger an action, e.g. {Action:4}, to preview or script her.
        if (tag.contains("Action") && !this.level.isClientSide && this.isAlive()) {
            this.startAction(Mth.clamp(tag.getInt("Action"), NONE, PHASE));
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
        return this.getAction() == NONE ? ModSounds.AMBIENT.get() : null;
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

    /** Drifts toward the target and stops just inside sweeping reach. */
    private static class ApproachGoal extends Goal {
        private final GoddessOfDeath goddess;
        private int repath;

        ApproachGoal(GoddessOfDeath goddess) {
            this.goddess = goddess;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.goddess.getTarget();
            return target != null && target.isAlive() && this.goddess.getAction() == NONE;
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
            if (this.goddess.distanceToSqr(target) < 3.2D * 3.2D) {
                this.goddess.getNavigation().stop();
            } else if (--this.repath <= 0) {
                this.repath = 8 + this.goddess.getRandom().nextInt(6);
                this.goddess.getNavigation().moveTo(target, 1.0D);
            }
        }
    }
}
