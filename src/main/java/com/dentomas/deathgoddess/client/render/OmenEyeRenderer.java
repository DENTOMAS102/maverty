package com.dentomas.deathgoddess.client.render;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.entity.OmenEye;
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

/** A three-block-wide eye that always faces the viewer: the lids part, and the pupil never quite holds still. */
public class OmenEyeRenderer extends EntityRenderer<OmenEye> {
    private static final ResourceLocation EYE = DeathGoddessMod.id("textures/entity/omen_eye.png");
    private static final RenderType EYE_TYPE = RenderType.entityCutoutNoCull(EYE);
    private static final RenderType PUPIL_TYPE =
            RenderType.entityTranslucentEmissive(DeathGoddessMod.id("textures/entity/omen_pupil.png"));
    private static final float HALF_WIDTH = 1.6F;
    private static final float HALF_HEIGHT = 1.0F;

    public OmenEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(OmenEye eye, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float open = eye.openness(partialTick);
        if (open <= 0.01F) {
            return;
        }
        float age = eye.tickCount + partialTick;
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Vector3f.YP.rotationDegrees(180.0F));
        // Lids open like a wound: a slit first, then wide.
        float lid = open * open * (3.0F - 2.0F * open);
        pose.pushPose();
        pose.scale(1.0F, Math.max(0.03F, lid), 1.0F);
        quad(buffers.getBuffer(EYE_TYPE), pose, HALF_WIDTH, HALF_HEIGHT, 0.0F, 1.0F);
        pose.popPose();

        // The pupil holds on its target with small, sudden corrections, and slowly widens.
        int step = Mth.floor(age / 7.0F);
        float snap = Mth.clamp((age / 7.0F - step) * 5.0F, 0.0F, 1.0F);
        float dx = Mth.lerp(snap, jitter(step - 1, 0), jitter(step, 0)) * 0.1F;
        float dy = Mth.lerp(snap, jitter(step - 1, 1), jitter(step, 1)) * 0.05F;
        float size = (0.42F + 0.12F * Mth.clamp(age / 120.0F, 0.0F, 1.0F)) * Math.min(1.0F, lid * 1.6F);
        pose.translate(dx, dy, -0.02F);
        quad(buffers.getBuffer(PUPIL_TYPE), pose, size, size, 0.0F, Mth.clamp(lid * 2.0F, 0.0F, 1.0F));
        pose.popPose();
        super.render(eye, yaw, partialTick, pose, buffers, light);
    }

    private static float jitter(int step, int axis) {
        return Mth.frac(Mth.sin(step * 127.1F + axis * 311.7F) * 43758.547F) * 2.0F - 1.0F;
    }

    private static void quad(VertexConsumer consumer, PoseStack pose, float halfWidth, float halfHeight, float z, float alpha) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        vertex(consumer, matrix, normal, -halfWidth, -halfHeight, z, 0.0F, 1.0F, alpha);
        vertex(consumer, matrix, normal, halfWidth, -halfHeight, z, 1.0F, 1.0F, alpha);
        vertex(consumer, matrix, normal, halfWidth, halfHeight, z, 1.0F, 0.0F, alpha);
        vertex(consumer, matrix, normal, -halfWidth, halfHeight, z, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z, float u,
                               float v, float alpha) {
        consumer.vertex(matrix, x, y, z).color(1.0F, 1.0F, 1.0F, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(OmenEye eye) {
        return EYE;
    }
}
