package com.dentomas.deathgoddess.client;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.entity.OmenEye;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What it is like to be watched: while an OmenEye is fixed on the local player the view closes in
 * from every side, narrows and trembles. Nothing here touches the game state.
 */
@Mod.EventBusSubscriber(modid = DeathGoddessMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WatchedEffects {
    /** Share of each screen edge swallowed when the gaze is strongest. */
    private static final float INSET = 0.14F;
    private static final int SOFT_STEPS = 6;
    private static float strength;
    private static float lastStrength;
    private static int ticks;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        lastStrength = strength;
        float wanted = 0.0F;
        if (minecraft.level != null && minecraft.player != null) {
            if (minecraft.isPaused()) {
                return;
            }
            int self = minecraft.player.getId();
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof OmenEye eye && eye.getTargetId() == self) {
                    wanted = Math.max(wanted, eye.gaze());
                }
            }
        }
        strength += (wanted - strength) * 0.3F;
        if (strength < 0.003F) {
            strength = 0.0F;
        }
        ticks++;
    }

    private static float strength(double partialTick) {
        return Mth.lerp((float) partialTick, lastStrength, strength);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        float s = strength(event.getPartialTick());
        if (s > 0.0F) {
            // The world draws in, pulsing slightly like a held breath.
            float time = ticks + (float) event.getPartialTick();
            event.setFOV(event.getFOV() * (1.0D - 0.17D * s + 0.012D * s * Mth.sin(time * 0.9F)));
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float s = strength(event.getPartialTick());
        if (s > 0.0F) {
            float time = ticks + (float) event.getPartialTick();
            event.setRoll(event.getRoll() + (Mth.sin(time * 1.9F) * 1.5F + Mth.sin(time * 7.3F) * 0.6F) * s);
            event.setPitch(event.getPitch() + (Mth.sin(time * 5.1F + 1.0F) * 0.45F + Mth.sin(time * 11.7F) * 0.2F) * s);
            event.setYaw(event.getYaw() + (Mth.sin(time * 4.3F) * 0.45F + Mth.sin(time * 13.1F + 2.0F) * 0.2F) * s);
        }
    }

    /** Drawn above the whole HUD: black closing in from all four sides, with a soft, shuddering edge. */
    public static void renderOverlay(ForgeGui gui, PoseStack pose, float partialTick, int width, int height) {
        float s = strength(partialTick);
        if (s <= 0.0F) {
            return;
        }
        float time = ticks + partialTick;
        float shakeX = (Mth.sin(time * 1.7F) * 0.012F + Mth.sin(time * 6.1F) * 0.007F) * width * s;
        float shakeY = (Mth.sin(time * 2.3F + 1.0F) * 0.012F + Mth.sin(time * 7.7F) * 0.007F) * height * s;
        float breathe = 1.0F + 0.06F * Mth.sin(time * 0.9F);
        float insetX = width * INSET * s * breathe;
        float insetY = height * INSET * s * breathe;
        float softX = width * 0.05F * s;
        float softY = height * 0.05F * s;
        // A faint blood tint over what is left of the view.
        GuiComponent.fill(pose, 0, 0, width, height, ((int) (26.0F * s) << 24) | 0x3B0713);
        for (int step = 0; step <= SOFT_STEPS; step++) {
            // Solid bars first, then progressively fainter strips reaching inward.
            float reach = (float) step / SOFT_STEPS;
            int alpha = step == 0 ? 255 : (int) (150.0F * (1.0F - reach) * (1.0F - reach));
            int color = (alpha << 24) | 0x080608;
            int left = Math.round(insetX + softX * reach + shakeX);
            int right = Math.round(width - insetX - softX * reach + shakeX);
            int top = Math.round(insetY + softY * reach + shakeY);
            int bottom = Math.round(height - insetY - softY * reach + shakeY);
            GuiComponent.fill(pose, 0, 0, Math.max(0, left), height, color);
            GuiComponent.fill(pose, Math.min(width, right), 0, width, height, color);
            GuiComponent.fill(pose, Math.max(0, left), 0, Math.min(width, right), Math.max(0, top), color);
            GuiComponent.fill(pose, Math.max(0, left), Math.min(height, bottom), Math.min(width, right), height, color);
        }
    }

    private WatchedEffects() {
    }
}
