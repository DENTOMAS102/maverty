package com.dentomas.deathgoddess.client.render;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.client.model.GoddessModel;
import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A thread rises from each wrist and fades into the dark above her. Each one goes slack and drops
 * away at the moment the death animation cuts that arm loose.
 */
public class MarionetteStringsLayer extends RenderLayer<GoddessOfDeath, GoddessModel> {
    private static final ResourceLocation TEXTURE = DeathGoddessMod.id("textures/entity/marionette_string.png");
    private static final RenderType TYPE = RenderType.entityTranslucent(TEXTURE);
    private static final float LENGTH = 3.4F;
    private static final float HALF_WIDTH = 0.014F;
    /** Seconds into the death animation at which each hand chain (top r/l, mid r/l, low r/l) is cut. */
    private static final float[] CUT_AT = {0.5F, 1.5F, 1.8F, 0.9F, 1.2F, 2.1F};

    public MarionetteStringsLayer(RenderLayerParent<GoddessOfDeath, GoddessModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, GoddessOfDeath entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        float deathSeconds = entity.dyingTicks > 0 ? (entity.dyingTicks + partialTick) / 20.0F : -1.0F;
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        ModelPart[][] chains = this.getParentModel().handChains();
        for (int i = 0; i < chains.length; i++) {
            float alpha = 1.0F;
            if (deathSeconds >= 0.0F) {
                alpha = 1.0F - Mth.clamp((deathSeconds - CUT_AT[i]) / 0.25F, 0.0F, 1.0F);
                if (alpha <= 0.0F) {
                    continue;
                }
            }
            // Walk the part chain to find the wrist in model space (y points down there).
            PoseStack local = new PoseStack();
            for (ModelPart part : chains[i]) {
                part.translateAndRotate(local);
            }
            Vector4f wrist = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
            wrist.transform(local.last().pose());
            float sway = Mth.sin(age * 0.05F + i * 1.7F) * 0.06F;
            float topX = wrist.x() + sway;
            float topY = wrist.y() - LENGTH;
            float topZ = wrist.z() + Mth.cos(age * 0.04F + i) * 0.04F;
            int a = (int) (alpha * 255.0F);
            quad(consumer, matrix, normal, light, a, wrist.x(), wrist.y(), wrist.z(), topX, topY, topZ, HALF_WIDTH, 0.0F);
            quad(consumer, matrix, normal, light, a, wrist.x(), wrist.y(), wrist.z(), topX, topY, topZ, 0.0F, HALF_WIDTH);
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light, int alpha, float x0,
                             float y0, float z0, float x1, float y1, float z1, float dx, float dz) {
        vertex(consumer, matrix, normal, light, alpha, x0 - dx, y0, z0 - dz, 0.0F, 1.0F);
        vertex(consumer, matrix, normal, light, alpha, x0 + dx, y0, z0 + dz, 1.0F, 1.0F);
        vertex(consumer, matrix, normal, light, alpha, x1 + dx, y1, z1 + dz, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, light, alpha, x1 - dx, y1, z1 - dz, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light, int alpha, float x,
                               float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z).color(255, 255, 255, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(normal, 0.0F, -1.0F, 0.0F).endVertex();
    }
}
