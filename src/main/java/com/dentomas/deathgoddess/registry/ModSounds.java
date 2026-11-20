package com.dentomas.deathgoddess.registry;

import com.dentomas.deathgoddess.DeathGoddessMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DeathGoddessMod.MODID);

    public static final RegistryObject<SoundEvent> AMBIENT = register("ambient");
    public static final RegistryObject<SoundEvent> HURT = register("hurt");
    public static final RegistryObject<SoundEvent> DEATH = register("death");
    public static final RegistryObject<SoundEvent> SPAWN = register("spawn");
    public static final RegistryObject<SoundEvent> GLIDE = register("glide");
    public static final RegistryObject<SoundEvent> SWEEP = register("sweep");
    public static final RegistryObject<SoundEvent> EYE_CAST = register("eye_cast");
    public static final RegistryObject<SoundEvent> EYE_HIT = register("eye_hit");
    public static final RegistryObject<SoundEvent> GAZE_CHARGE = register("gaze_charge");
    public static final RegistryObject<SoundEvent> GAZE_RELEASE = register("gaze_release");
    public static final RegistryObject<SoundEvent> STRINGS = register("strings");
    public static final RegistryObject<SoundEvent> VEIL_STEP = register("veil_step");
    public static final RegistryObject<SoundEvent> PHASE = register("phase");

    private static RegistryObject<SoundEvent> register(String name) {
        String path = "entity.goddess_of_death." + name;
        return SOUNDS.register(path, () -> new SoundEvent(DeathGoddessMod.id(path)));
    }

    private ModSounds() {
    }
}
