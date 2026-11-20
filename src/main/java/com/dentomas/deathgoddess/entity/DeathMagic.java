package com.dentomas.deathgoddess.entity;

import com.dentomas.deathgoddess.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * The rules of her magic, shared by every attack: players and other bosses take damage, every other
 * living thing simply dies, and whatever grows where the magic lands withers.
 */
public final class DeathMagic {
    /** How far above and below the struck ground vegetation is withered (trees lose their crowns). */
    private static final int BLIGHT_UP = 10;
    private static final int BLIGHT_DOWN = 3;

    @FunctionalInterface
    public interface Area {
        boolean contains(double x, double z);
    }

    public static DamageSource source(Entity direct, @Nullable Entity owner) {
        return new IndirectEntityDamageSource("deathgoddess.death_magic", direct, owner).bypassArmor().setMagic();
    }

    /** Ordinary creatures: anything alive that is not a player, a boss or the Goddess herself. */
    public static boolean isMortal(LivingEntity victim) {
        return victim.isAlive() && !(victim instanceof Player) && !(victim instanceof GoddessOfDeath)
                && !(victim instanceof ArmorStand) && !victim.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** @return true if the victim was hurt or killed */
    public static boolean strike(@Nullable LivingEntity owner, Entity direct, LivingEntity victim, float damage) {
        if (victim == owner || victim instanceof GoddessOfDeath || victim instanceof ArmorStand || !victim.isAlive()) {
            return false;
        }
        if (victim instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && player.hurt(source(direct, owner), damage);
        }
        if (!isMortal(victim)) {
            return victim.hurt(source(direct, owner), damage);
        }
        return reap(owner, direct, victim);
    }

    /** Her mere presence: ordinary creatures this close to her die. */
    public static void reapNear(LivingEntity owner, double radius) {
        AABB box = owner.getBoundingBox().inflate(radius, 1.0D, radius);
        for (LivingEntity victim : owner.level.getEntitiesOfClass(LivingEntity.class, box, DeathMagic::isMortal)) {
            if (victim.distanceToSqr(owner) <= (radius + 1.0D) * (radius + 1.0D)) {
                reap(owner, owner, victim);
            }
        }
    }

    /** Takes the life of an ordinary creature outright. */
    public static boolean reap(@Nullable LivingEntity owner, Entity direct, LivingEntity victim) {
        if (victim.isInvulnerable() || !(victim.level instanceof ServerLevel level)) {
            return false;
        }
        double height = victim.getBbHeight();
        level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + height * 0.6D, victim.getZ(), 6,
                victim.getBbWidth() * 0.3D, height * 0.3D, victim.getBbWidth() * 0.3D, 0.03D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + height * 0.4D, victim.getZ(), 5,
                victim.getBbWidth() * 0.3D, height * 0.3D, victim.getBbWidth() * 0.3D, 0.01D);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.REAP.get(), SoundSource.HOSTILE, 0.7F,
                0.8F + level.random.nextFloat() * 0.4F);
        victim.invulnerableTime = 0;
        victim.hurt(source(direct, owner), Float.MAX_VALUE);
        if (victim.isAlive()) {
            victim.kill();
        }
        return true;
    }

    /** Strikes everything alive inside the area, within a few blocks of the given height. */
    public static void strikeArea(Level level, @Nullable LivingEntity owner, Entity direct, AABB bounds, double groundY,
                                  Area area, float damage) {
        AABB box = new AABB(bounds.minX, groundY - 3.0D, bounds.minZ, bounds.maxX, groundY + 6.0D, bounds.maxZ);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (area.contains(victim.getX(), victim.getZ())) {
                strike(owner, direct, victim, damage);
            }
        }
    }

    public static void strikeDisc(Level level, @Nullable LivingEntity owner, Entity direct, Vec3 center, double radius,
                                  float damage) {
        strikeArea(level, owner, direct, discBounds(center, radius), center.y, disc(center, radius), damage);
    }

    public static Area disc(Vec3 center, double radius) {
        return (x, z) -> (x - center.x) * (x - center.x) + (z - center.z) * (z - center.z) <= radius * radius;
    }

    public static AABB discBounds(Vec3 center, double radius) {
        return new AABB(center.x - radius, center.y, center.z - radius, center.x + radius, center.y, center.z + radius);
    }

    public static int blightDisc(Level level, @Nullable Entity owner, Vec3 center, double radius) {
        return blight(level, owner, discBounds(center, radius), center.y, disc(center, radius));
    }

    /**
     * Withers the vegetation in every block column whose centre is inside the area: grass turns to
     * bare earth, plants die back and leaves fall away. Respects the mobGriefing game rule.
     *
     * @return how many blocks changed
     */
    public static int blight(Level level, @Nullable Entity owner, AABB bounds, double groundY, Area area) {
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        boolean allowed = owner != null ? ForgeEventFactory.getMobGriefingEvent(level, owner)
                : level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        if (!allowed) {
            return 0;
        }
        int top = Mth.floor(groundY) + BLIGHT_UP;
        int bottom = Math.max(level.getMinBuildHeight(), Mth.floor(groundY) - BLIGHT_DOWN);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int changed = 0;
        for (int x = Mth.floor(bounds.minX); x <= Mth.floor(bounds.maxX); x++) {
            for (int z = Mth.floor(bounds.minZ); z <= Mth.floor(bounds.maxZ); z++) {
                if (!area.contains(x + 0.5D, z + 0.5D)) {
                    continue;
                }
                for (int y = top; y >= bottom; y--) {
                    if (wither(server, pos.set(x, y, z))) {
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    private static boolean wither(ServerLevel level, BlockPos.MutableBlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (isLivingSoil(state)) {
            level.setBlock(pos, deadSoil(pos), Block.UPDATE_CLIENTS);
            return true;
        }
        Material material = state.getMaterial();
        boolean plant = material == Material.PLANT || material == Material.REPLACEABLE_PLANT || material == Material.BAMBOO
                || material == Material.BAMBOO_SAPLING || material == Material.CACTUS;
        if (!plant && !state.is(BlockTags.LEAVES)) {
            return false;
        }
        if (state.is(Blocks.DEAD_BUSH) || state.is(Blocks.WITHER_ROSE)) {
            return false;
        }
        boolean flower = state.is(BlockTags.SMALL_FLOWERS);
        // Known-shape flag: each half of a tall plant is cleared on its own, with no drops.
        BlockState left = state.getFluidState().createLegacyBlock();
        level.setBlock(pos, left, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (plant && left.isAir()) {
            BlockPos below = pos.below();
            if (isLivingSoil(level.getBlockState(below))) {
                level.setBlock(below, deadSoil(below), Block.UPDATE_CLIENTS);
            }
            RandomSource random = level.random;
            BlockState husk = flower && random.nextInt(4) == 0 ? Blocks.WITHER_ROSE.defaultBlockState()
                    : random.nextInt(3) == 0 ? Blocks.DEAD_BUSH.defaultBlockState() : null;
            if (husk != null && husk.canSurvive(level, pos)) {
                level.setBlock(pos, husk, Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }

    private static boolean isLivingSoil(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.FARMLAND) || state.is(Blocks.ROOTED_DIRT);
    }

    private static BlockState deadSoil(BlockPos pos) {
        return (Math.floorMod(Mth.getSeed(pos), 5L) < 2L ? Blocks.COARSE_DIRT : Blocks.DIRT).defaultBlockState();
    }

    /** The top of the first solid block at or below the point, so sigils lie on the ground. */
    public static Vec3 ground(Level level, Vec3 at) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int start = Mth.floor(at.y + 1.5D);
        for (int y = start; y >= Math.max(level.getMinBuildHeight(), start - 14); y--) {
            pos.set(Mth.floor(at.x), y, Mth.floor(at.z));
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return new Vec3(at.x, y + shape.max(net.minecraft.core.Direction.Axis.Y), at.z);
            }
        }
        return at;
    }

    private DeathMagic() {
    }
}
