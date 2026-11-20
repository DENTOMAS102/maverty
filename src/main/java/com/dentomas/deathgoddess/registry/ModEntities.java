package com.dentomas.deathgoddess.registry;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.entity.GoddessOfDeath;
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

    private ModEntities() {
    }
}
