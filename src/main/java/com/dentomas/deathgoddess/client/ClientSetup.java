package com.dentomas.deathgoddess.client;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.client.model.GoddessModelLayer;
import com.dentomas.deathgoddess.client.render.DeathBoltRenderer;
import com.dentomas.deathgoddess.client.render.DeathSigilRenderer;
import com.dentomas.deathgoddess.client.render.GoddessRenderer;
import com.dentomas.deathgoddess.client.render.OmenEyeRenderer;
import com.dentomas.deathgoddess.client.render.WatchingEyeRenderer;
import com.dentomas.deathgoddess.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathGoddessMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final ModelLayerLocation GODDESS_LAYER =
            new ModelLayerLocation(DeathGoddessMod.id("goddess_of_death"), "main");

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GODDESS_LAYER, GoddessModelLayer::create);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GODDESS.get(), GoddessRenderer::new);
        event.registerEntityRenderer(ModEntities.WATCHING_EYE.get(), WatchingEyeRenderer::new);
        event.registerEntityRenderer(ModEntities.DEATH_SIGIL.get(), DeathSigilRenderer::new);
        event.registerEntityRenderer(ModEntities.DEATH_CRESCENT.get(), DeathBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.DEATH_ORB.get(), DeathBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.SOUL_LANCE.get(), DeathBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.OMEN_EYE.get(), OmenEyeRenderer::new);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("watched", WatchedEffects::renderOverlay);
    }

    private ClientSetup() {
    }
}
