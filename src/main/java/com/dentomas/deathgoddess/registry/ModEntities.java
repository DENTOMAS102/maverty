package com.dentomas.deathgoddess.registry;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.entity.DeathBolt;
import com.dentomas.deathgoddess.entity.DeathSigil;
import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import com.dentomas.deathgoddess.entity.OmenEye;
import com.dentomas.deathgoddess.entity.WatchingEye;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DeathGoddessMod.MODID);

    public static final RegistryObject<EntityType<GoddessOfDeath>> GODDESS = ENTITIES.register("goddess_of_death",
            () -> EntityType.Builder.of(GoddessOfDeath::new, MobCategory.MONSTER)
                    .sized(1.0F, 3.9F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("goddess_of_death"));

    public static final RegistryObject<EntityType<WatchingEye>> WATCHING_EYE = ENTITIES.register("watching_eye",
            () -> EntityType.Builder.<WatchingEye>of(WatchingEye::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("watching_eye"));

    // Spell effects only live for a few seconds, so none of them are written to the world save.
    public static final RegistryObject<EntityType<DeathSigil>> DEATH_SIGIL = ENTITIES.register("death_sigil",
            () -> EntityType.Builder.<DeathSigil>of(DeathSigil::new, MobCategory.MISC)
                    .sized(0.5F, 0.1F)
                    .fireImmune()
                    .noSave()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("death_sigil"));

    public static final RegistryObject<EntityType<DeathBolt>> DEATH_CRESCENT = ENTITIES.register("death_crescent",
            () -> bolt(2.2F, 0.7F, 1).build("death_crescent"));

    public static final RegistryObject<EntityType<DeathBolt>> DEATH_ORB = ENTITIES.register("death_orb",
            () -> bolt(0.6F, 0.6F, 3).build("death_orb"));

    public static final RegistryObject<EntityType<DeathBolt>> SOUL_LANCE = ENTITIES.register("soul_lance",
            () -> bolt(0.45F, 0.45F, 1).build("soul_lance"));

    public static final RegistryObject<EntityType<OmenEye>> OMEN_EYE = ENTITIES.register("omen_eye",
            () -> EntityType.Builder.<OmenEye>of(OmenEye::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("omen_eye"));

    private static EntityType.Builder<DeathBolt> bolt(float width, float height, int updateInterval) {
        return EntityType.Builder.<DeathBolt>of(DeathBolt::new, MobCategory.MISC)
                .sized(width, height)
                .fireImmune()
                .noSave()
                .clientTrackingRange(8)
                .updateInterval(updateInterval)
                .setShouldReceiveVelocityUpdates(true);
    }

    private ModEntities() {
    }
}
