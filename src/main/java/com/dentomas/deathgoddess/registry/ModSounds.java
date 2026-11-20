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
    public static final RegistryObject<SoundEvent> CAST_SIGIL = register("cast_sigil");
    public static final RegistryObject<SoundEvent> CAST_BLADE = register("cast_blade");
    public static final RegistryObject<SoundEvent> SIGIL_ERUPT = register("sigil_erupt");
    public static final RegistryObject<SoundEvent> BLADE_WAVE = register("blade_wave");
    public static final RegistryObject<SoundEvent> KNELL = register("knell");
    public static final RegistryObject<SoundEvent> RAIN_FALL = register("rain_fall");
    public static final RegistryObject<SoundEvent> LANCE_FORM = register("lance_form");
    public static final RegistryObject<SoundEvent> LANCE_FIRE = register("lance_fire");
    public static final RegistryObject<SoundEvent> DANSE = register("danse");
    public static final RegistryObject<SoundEvent> REQUIEM_CHARGE = register("requiem_charge");
    public static final RegistryObject<SoundEvent> REQUIEM_RELEASE = register("requiem_release");
    public static final RegistryObject<SoundEvent> REAP = register("reap");
    public static final RegistryObject<SoundEvent> WITHER = register("wither");

    private static RegistryObject<SoundEvent> register(String name) {
        String path = "entity.goddess_of_death." + name;
        return SOUNDS.register(path, () -> new SoundEvent(DeathGoddessMod.id(path)));
    }

    private ModSounds() {
    }
}
