package com.dentomas.deathgoddess.client.render;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.entity.WatchingEye;
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

/** A fullbright billboard: the eye always stares straight at the camera. */
public class WatchingEyeRenderer extends EntityRenderer<WatchingEye> {
    private static final ResourceLocation TEXTURE = DeathGoddessMod.id("textures/entity/watching_eye.png");
    private static final RenderType TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    public WatchingEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WatchingEye eye, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0D, 0.22D, 0.0D);
        float pulse = 0.85F + Mth.sin((eye.tickCount + partialTick) * 0.6F) * 0.08F;
        pose.scale(pulse, pulse, pulse);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Vector3f.YP.rotationDegrees(180.0F));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        vertex(consumer, matrix, normal, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(consumer, matrix, normal, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(consumer, matrix, normal, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, -0.5F, 0.5F, 0.0F, 0.0F);
        pose.popPose();
        super.render(eye, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v) {
        consumer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(WatchingEye eye) {
        return TEXTURE;
    }
}
