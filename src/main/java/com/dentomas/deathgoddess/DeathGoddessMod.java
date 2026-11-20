package com.dentomas.deathgoddess;

import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import com.dentomas.deathgoddess.registry.ModEntities;
import com.dentomas.deathgoddess.registry.ModItems;
import com.dentomas.deathgoddess.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DeathGoddessMod.MODID)
public class DeathGoddessMod {
    public static final String MODID = "deathgoddess";

    public DeathGoddessMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::registerAttributes);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GODDESS.get(), GoddessOfDeath.createAttributes().build());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
