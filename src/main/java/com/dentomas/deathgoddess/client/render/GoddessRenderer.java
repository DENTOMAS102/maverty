package com.dentomas.deathgoddess.client.render;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.client.ClientSetup;
import com.dentomas.deathgoddess.client.model.GoddessModel;
import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GoddessRenderer extends MobRenderer<GoddessOfDeath, GoddessModel> {
    private static final ResourceLocation TEXTURE = DeathGoddessMod.id("textures/entity/goddess_of_death.png");

    public GoddessRenderer(EntityRendererProvider.Context context) {
        super(context, new GoddessModel(context.bakeLayer(ClientSetup.GODDESS_LAYER)), 0.9F);
        this.addLayer(new GoddessGlowLayer(this));
        this.addLayer(new MarionetteStringsLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(GoddessOfDeath entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(GoddessOfDeath entity) {
        // The death animation does its own collapsing.
        return 0.0F;
    }
}
