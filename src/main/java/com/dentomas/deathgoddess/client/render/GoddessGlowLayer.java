package com.dentomas.deathgoddess.client.render;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.client.model.GoddessModel;
import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Fullbright pupils, plus pulsing wounds of light across cloth and bone once she is enraged. */
public class GoddessGlowLayer extends RenderLayer<GoddessOfDeath, GoddessModel> {
    private static final RenderType EYES = RenderType.eyes(DeathGoddessMod.id("textures/entity/goddess_of_death_glow.png"));
    private static final RenderType ENRAGED =
            RenderType.eyes(DeathGoddessMod.id("textures/entity/goddess_of_death_enraged.png"));

    public GoddessGlowLayer(RenderLayerParent<GoddessOfDeath, GoddessModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, GoddessOfDeath entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        float fade = 1.0F - Mth.clamp((entity.dyingTicks + partialTick) / (GoddessOfDeath.DEATH_TICKS * 0.7F), 0.0F, 1.0F);
        if (entity.dyingTicks == 0) {
            fade = 1.0F;
        }
        if (fade <= 0.0F) {
            return;
        }
        boolean enraged = entity.isEnraged();
        float brightness = fade;
        if (enraged) {
            brightness *= 0.6F + 0.4F * Mth.sin(age * 0.18F);
        }
        this.getParentModel().renderToBuffer(pose, buffers.getBuffer(enraged ? ENRAGED : EYES), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, brightness, brightness, brightness, 1.0F);
        if (enraged) {
            // Keep the pupils steady while the wounds pulse.
            this.getParentModel().renderToBuffer(pose, buffers.getBuffer(EYES), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, fade, fade, fade, 1.0F);
        }
    }
}
