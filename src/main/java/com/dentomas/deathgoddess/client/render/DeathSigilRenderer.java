package com.dentomas.deathgoddess.client.render;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.entity.DeathSigil;
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

/**
 * Draws a sigil in three layers: the seal on the ground (visible at full size from the first tick),
 * a fill that grows until the moment it strikes, and a low curtain along the edge that shoots up
 * into the burst. Everything is fullbright so it reads the same at noon and at midnight.
 */
public class DeathSigilRenderer extends EntityRenderer<DeathSigil> {
    private static final ResourceLocation DISC = DeathGoddessMod.id("textures/entity/sigil_disc.png");
    private static final RenderType DISC_TYPE = type("sigil_disc");
    private static final RenderType MARK_TYPE = type("sigil_mark");
    private static final RenderType SAFE_TYPE = type("sigil_safe");
    private static final RenderType RING_TYPE = type("sigil_ring");
    private static final RenderType LINE_TYPE = type("sigil_line");
    private static final RenderType FILL_TYPE = type("sigil_fill");
    private static final RenderType PILLAR_TYPE = type("death_pillar");
    /** Blocks of edge per repeat of the curtain texture. */
    private static final float CURTAIN_TILE = 2.5F;

    private Matrix4f matrix;
    private Matrix3f normal;
    private VertexConsumer consumer;
    private float red;
    private float green;
    private float blue;
    private float alpha;

    public DeathSigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static RenderType type(String texture) {
        return RenderType.entityTranslucentEmissive(DeathGoddessMod.id("textures/entity/" + texture + ".png"));
    }

    @Override
    public void render(DeathSigil sigil, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = sigil.tickCount + partialTick;
        float warmup = Math.max(1, sigil.getWarmup());
        float progress = Mth.clamp(age / warmup, 0.0F, 1.0F);
        float after = age - warmup;
        boolean struck = after >= 0.0F;
        float fade = struck ? 1.0F - Mth.clamp(after / DeathSigil.FADE_TICKS, 0.0F, 1.0F) : 1.0F;
        float appear = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        // The seal throbs faster as the strike approaches.
        float throb = 0.78F + 0.22F * Mth.sin(age * (0.3F + progress * 0.9F));
        int shape = sigil.getShape();
        int style = sigil.getStyle();
        boolean safe = style == DeathSigil.SAFE;
        float a = sigil.getSizeA();
        float b = sigil.getSizeB();

        pose.pushPose();
        pose.translate(0.0D, 0.03D, 0.0D);
        if (shape == DeathSigil.LINE) {
            pose.mulPose(Vector3f.YP.rotationDegrees(-sigil.getYaw()));
        }
        this.matrix = pose.last().pose();
        this.normal = pose.last().normal();

        // Fill: how close it is to going off.
        this.begin(buffers, FILL_TYPE);
        if (safe) {
            this.color(1.0F, 0.96F, 0.86F, 0.16F * appear * fade);
        } else if (struck) {
            this.color(1.0F, 0.35F, 0.3F, 0.55F * fade * fade);
        } else {
            this.color(0.62F, 0.03F, 0.1F, 0.34F);
        }
        float grown = safe || struck ? 1.0F : progress;
        switch (shape) {
            case DeathSigil.RING -> this.annulus(b, b + (a - b) * grown, 0.0F, false);
            case DeathSigil.LINE -> this.strip(a * grown, b, 0.0F, false);
            default -> this.annulus(0.0F, a * grown, 0.0F, false);
        }

        // Seal.
        float sealAlpha = appear * (struck ? fade : 1.0F);
        float bright = safe ? 1.0F : struck ? 1.0F : throb;
        this.color(bright, bright, bright, sealAlpha);
        switch (shape) {
            case DeathSigil.RING -> {
                this.begin(buffers, RING_TYPE);
                this.annulus(b, a, 0.012F, true);
            }
            case DeathSigil.LINE -> {
                this.begin(buffers, LINE_TYPE);
                this.strip(a, b, 0.012F, true);
            }
            default -> {
                this.begin(buffers, safe ? SAFE_TYPE : style == DeathSigil.MARK ? MARK_TYPE : DISC_TYPE);
                // Seals turn slowly; the refuge stays still.
                float turn = safe ? 0.0F : age * (style == DeathSigil.MARK ? 4.0F : 1.2F) * Mth.DEG_TO_RAD;
                this.seal(a, turn);
            }
        }

        // Curtain along the edge, then the burst.
        this.begin(buffers, PILLAR_TYPE);
        float scroll = age * 0.06F;
        if (!struck) {
            this.color(safe ? 1.0F : throb, safe ? 0.95F : throb, safe ? 0.85F : throb, (safe ? 0.5F : 0.75F) * appear);
            this.walls(shape, a, b, 1.0F, 0.0F, safe ? 0.9F : 0.55F, scroll);
            if (style == DeathSigil.RAIN && warmup - age < 7.0F) {
                // Something falls out of the sky for the last third of a second.
                float fall = (warmup - age) / 7.0F;
                this.color(1.0F, 1.0F, 1.0F, 0.9F);
                this.cylinder(a * 0.45F, fall * 16.0F, fall * 16.0F + 5.0F, scroll);
            }
        } else if (!safe) {
            float height = burstHeight(style) * Math.min(1.0F, 0.25F + after / 3.0F);
            this.color(1.0F, 1.0F, 1.0F, fade);
            this.walls(shape, a, b, 1.0F, 0.0F, height, scroll * 3.0F);
            this.walls(shape, a, b, 0.6F, 0.0F, height * 1.25F, scroll * 2.0F + 0.37F);
            if (shape != DeathSigil.RING && style != DeathSigil.REQUIEM) {
                this.walls(shape, a, b, 0.25F, 0.0F, height * 1.45F, scroll * 2.5F + 0.71F);
            }
        }
        pose.popPose();
        super.render(sigil, yaw, partialTick, pose, buffers, light);
    }

    private static float burstHeight(int style) {
        return switch (style) {
            case DeathSigil.RAIN -> 9.0F;
            case DeathSigil.BLADE -> 3.4F;
            case DeathSigil.TOLL -> 2.2F;
            case DeathSigil.MARK -> 7.5F;
            case DeathSigil.REQUIEM -> 10.0F;
            default -> 5.5F;
        };
    }

    private void begin(MultiBufferSource buffers, RenderType type) {
        this.consumer = buffers.getBuffer(type);
    }

    private void color(float red, float green, float blue, float alpha) {
        this.red = Mth.clamp(red, 0.0F, 1.0F);
        this.green = Mth.clamp(green, 0.0F, 1.0F);
        this.blue = Mth.clamp(blue, 0.0F, 1.0F);
        this.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
    }

    private void vertex(float x, float y, float z, float u, float v) {
        this.consumer.vertex(this.matrix, x, y, z).color(this.red, this.green, this.blue, this.alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(this.normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static int segments(float radius) {
        return Mth.clamp((int) (radius * 5.0F), 20, 96);
    }

    /** A square carrying one whole seal texture, turned about its centre. */
    private void seal(float radius, float turn) {
        float cos = Mth.cos(turn) * radius;
        float sin = Mth.sin(turn) * radius;
        this.vertex(-cos + sin, 0.012F, -sin - cos, 0.0F, 0.0F);
        this.vertex(-cos - sin, 0.012F, -sin + cos, 0.0F, 1.0F);
        this.vertex(cos - sin, 0.012F, sin + cos, 1.0F, 1.0F);
        this.vertex(cos + sin, 0.012F, sin - cos, 1.0F, 0.0F);
    }

    /** Flat ring between two radii (a disc when the inner one is zero). */
    private void annulus(float inner, float outer, float y, boolean tiled) {
        if (outer <= inner) {
            return;
        }
        int count = segments(outer);
        for (int i = 0; i < count; i++) {
            float a0 = i * Mth.TWO_PI / count;
            float a1 = (i + 1) * Mth.TWO_PI / count;
            // Four segments per texture tile, so UVs never need to wrap.
            float u0 = tiled ? (i % 4) / 4.0F : 0.0F;
            float u1 = tiled ? u0 + 0.25F : 1.0F;
            this.vertex(Mth.cos(a0) * inner, y, Mth.sin(a0) * inner, u0, 0.0F);
            this.vertex(Mth.cos(a0) * outer, y, Mth.sin(a0) * outer, u0, 1.0F);
            this.vertex(Mth.cos(a1) * outer, y, Mth.sin(a1) * outer, u1, 1.0F);
            this.vertex(Mth.cos(a1) * inner, y, Mth.sin(a1) * inner, u1, 0.0F);
        }
    }

    /** Flat strip running along +z from the origin. */
    private void strip(float length, float width, float y, boolean tiled) {
        float half = width * 0.5F;
        float tile = tiled ? width : length;
        for (float z = 0.0F; z < length - 0.001F; z += tile) {
            float end = Math.min(length, z + tile);
            float v = tiled ? (end - z) / tile : 1.0F;
            this.vertex(-half, y, z, 0.0F, 0.0F);
            this.vertex(-half, y, end, 0.0F, v);
            this.vertex(half, y, end, 1.0F, v);
            this.vertex(half, y, z, 1.0F, 0.0F);
        }
    }

    /** Upright walls following the outline of the shape, scaled toward its middle. */
    private void walls(int shape, float a, float b, float scale, float bottom, float top, float scroll) {
        switch (shape) {
            case DeathSigil.RING -> {
                float mid = (a + b) * 0.5F;
                this.cylinder(mid + (a - mid) * scale, bottom, top, scroll);
                this.cylinder(mid + (b - mid) * scale, bottom, top, scroll + 0.5F);
            }
            case DeathSigil.LINE -> {
                float half = b * 0.5F * scale;
                this.wall(-half, 0.0F, -half, a, bottom, top, scroll);
                this.wall(half, 0.0F, half, a, bottom, top, scroll + 0.5F);
                if (scale == 1.0F) {
                    this.wall(-half, 0.0F, half, 0.0F, bottom, top, scroll);
                    this.wall(-half, a, half, a, bottom, top, scroll);
                }
            }
            default -> this.cylinder(a * scale, bottom, top, scroll);
        }
    }

    private void cylinder(float radius, float bottom, float top, float scroll) {
        int count = segments(radius);
        float step = Mth.TWO_PI * radius / count;
        for (int i = 0; i < count; i++) {
            float a0 = i * Mth.TWO_PI / count;
            float a1 = (i + 1) * Mth.TWO_PI / count;
            this.panel(Mth.cos(a0) * radius, Mth.sin(a0) * radius, Mth.cos(a1) * radius, Mth.sin(a1) * radius, bottom, top,
                    i * step / CURTAIN_TILE + scroll, step / CURTAIN_TILE);
        }
    }

    private void wall(float x0, float z0, float x1, float z1, float bottom, float top, float scroll) {
        float length = Mth.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));
        int count = Math.max(1, Mth.ceil(length / CURTAIN_TILE));
        for (int i = 0; i < count; i++) {
            float t0 = (float) i / count;
            float t1 = (float) (i + 1) / count;
            this.panel(Mth.lerp(t0, x0, x1), Mth.lerp(t0, z0, z1), Mth.lerp(t1, x0, x1), Mth.lerp(t1, z0, z1), bottom, top,
                    i * length / count / CURTAIN_TILE + scroll, length / count / CURTAIN_TILE);
        }
    }

    /** One upright quad; the texture is opaque at its base and torn away toward the top. */
    private void panel(float x0, float z0, float x1, float z1, float bottom, float top, float u, float width) {
        float u0 = Mth.frac(u);
        float u1 = u0 + Math.min(width, 1.0F - u0);
        this.vertex(x0, bottom, z0, u0, 1.0F);
        this.vertex(x1, bottom, z1, u1, 1.0F);
        this.vertex(x1, top, z1, u1, 0.0F);
        this.vertex(x0, top, z0, u0, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeathSigil sigil) {
        return DISC;
    }
}
