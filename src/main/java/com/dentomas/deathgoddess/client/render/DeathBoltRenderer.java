package com.dentomas.deathgoddess.client.render;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.entity.DeathBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Fullbright flat shapes: an upright arc for the crescent, a billboard for the orb, crossed blades for the lance. */
public class DeathBoltRenderer extends EntityRenderer<DeathBolt> {
    private static final ResourceLocation CRESCENT = DeathGoddessMod.id("textures/entity/death_crescent.png");
    private static final ResourceLocation ORB = DeathGoddessMod.id("textures/entity/death_orb.png");
    private static final ResourceLocation LANCE = DeathGoddessMod.id("textures/entity/soul_lance.png");
    private static final RenderType CRESCENT_TYPE = RenderType.entityTranslucentEmissive(CRESCENT);
    private static final RenderType ORB_TYPE = RenderType.entityTranslucentEmissive(ORB);
    private static final RenderType LANCE_TYPE = RenderType.entityTranslucentEmissive(LANCE);

    public DeathBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DeathBolt bolt, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = bolt.tickCount + partialTick;
        // Everything swells into being rather than popping in.
        float grow = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        float facing = Mth.rotLerp(partialTick, bolt.yRotO, bolt.getYRot());
        float pitch = Mth.lerp(partialTick, bolt.xRotO, bolt.getXRot());
        pose.pushPose();
        switch (bolt.kind()) {
            case CRESCENT -> {
                pose.translate(0.0D, 0.3D, 0.0D);
                pose.mulPose(Vector3f.YP.rotationDegrees(facing));
                pose.scale(grow, grow, grow);
                VertexConsumer consumer = buffers.getBuffer(CRESCENT_TYPE);
                // An upright arc facing the way it travels, leaning into it, and its wake along the ground.
                pose.pushPose();
                pose.mulPose(Vector3f.XP.rotationDegrees(-18.0F));
                quad(consumer, pose, -1.5F, -0.3F, 0.0F, 1.5F, -0.3F, 0.0F, 1.5F, 1.9F, 0.0F, -1.5F, 1.9F, 0.0F, 1.0F);
                pose.popPose();
                quad(consumer, pose, -1.5F, -0.2F, -2.0F, 1.5F, -0.2F, -2.0F, 1.5F, -0.2F, 0.3F, -1.5F, -0.2F, 0.3F, 0.7F);
            }
            case LANCE -> {
                pose.translate(0.0D, 0.22D, 0.0D);
                pose.mulPose(Vector3f.YP.rotationDegrees(facing - 90.0F));
                pose.mulPose(Vector3f.ZP.rotationDegrees(pitch));
                pose.scale(grow, grow, grow);
                VertexConsumer consumer = buffers.getBuffer(LANCE_TYPE);
                float shimmer = 0.85F + 0.15F * Mth.sin(age * 0.9F);
                quad(consumer, pose, -2.0F, -0.3F, 0.0F, 1.0F, -0.3F, 0.0F, 1.0F, 0.3F, 0.0F, -2.0F, 0.3F, 0.0F, shimmer);
                quad(consumer, pose, -2.0F, 0.0F, -0.3F, 1.0F, 0.0F, -0.3F, 1.0F, 0.0F, 0.3F, -2.0F, 0.0F, 0.3F, shimmer);
            }
            default -> {
                pose.translate(0.0D, 0.3D, 0.0D);
                float pulse = (0.8F + 0.1F * Mth.sin(age * 0.7F)) * grow;
                pose.scale(pulse, pulse, pulse);
                pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
                pose.mulPose(Vector3f.YP.rotationDegrees(180.0F));
                quad(buffers.getBuffer(ORB_TYPE), pose, -0.5F, -0.5F, 0.0F, 0.5F, -0.5F, 0.0F, 0.5F, 0.5F, 0.0F,
                        -0.5F, 0.5F, 0.0F, 1.0F);
            }
        }
        pose.popPose();
        super.render(bolt, yaw, partialTick, pose, buffers, light);
    }

    /** Corners in order: bottom-left, bottom-right, top-right, top-left of the texture. */
    private static void quad(VertexConsumer consumer, PoseStack pose, float x0, float y0, float z0, float x1, float y1,
                             float z1, float x2, float y2, float z2, float x3, float y3, float z3, float alpha) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        vertex(consumer, matrix, normal, x0, y0, z0, 0.0F, 1.0F, alpha);
        vertex(consumer, matrix, normal, x1, y1, z1, 1.0F, 1.0F, alpha);
        vertex(consumer, matrix, normal, x2, y2, z2, 1.0F, 0.0F, alpha);
        vertex(consumer, matrix, normal, x3, y3, z3, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z, float u,
                               float v, float alpha) {
        consumer.vertex(matrix, x, y, z).color(1.0F, 1.0F, 1.0F, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DeathBolt bolt) {
        return switch (bolt.kind()) {
            case CRESCENT -> CRESCENT;
            case LANCE -> LANCE;
            default -> ORB;
        };
    }
}
