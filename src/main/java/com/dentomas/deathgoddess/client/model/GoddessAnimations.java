package com.dentomas.deathgoddess.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Keyframe animations. Every channel is a flat list of {@code time, x, y, z} quadruples; rotations
 * are degrees added to the rest pose. Lengths must match the action durations in GoddessOfDeath
 * (20 ticks per second), and the comments mark the ticks on which the server lands the effect.
 */
public final class GoddessAnimations {
    /** Slow sway broken by sudden, stiff head tilts and hand twitches. */
    public static final AnimationDefinition IDLE = new Anim(8.0F).loop()
            .rot("torso", 0, 0, 0, 0, 2, 1.5F, 2.5F, 0.8F, 4, 0, 0, 0, 6, 1.5F, -2.5F, -0.8F, 8, 0, 0, 0)
            .snap("head", 0, 0, 0, 0, 1.8F, 0, 0, 0, 1.95F, 0, 0, 9, 4.6F, 0, 0, 9, 4.8F, -4, 0, -7, 7.2F, -4, 0, -7,
                    7.45F, 0, 0, 0, 8, 0, 0, 0)
            .rotBoth("arm_top", 0, 0, 0, 0, 2.5F, 0, 0, 5, 5, -3, 0, -2, 8, 0, 0, 0)
            .rotBoth("forearm_top", 0, 0, 0, 0, 3, 0, 0, 9, 6, -4, 0, -3, 8, 0, 0, 0)
            .rot("arm_mid_r", 0, 0, 0, 0, 2, -5, 0, 3, 4.5F, 2, 0, -3, 8, 0, 0, 0)
            .rot("arm_mid_l", 0, 0, 0, 0, 3, 3, 0, 3, 6, -6, 0, -2, 8, 0, 0, 0)
            .rotBoth("forearm_mid", 0, 0, 0, 0, 2, -8, 0, 0, 5, 4, 0, 5, 8, 0, 0, 0)
            .rotBoth("arm_low", 0, 0, 0, 0, 4, 2, 0, 4, 8, 0, 0, 0)
            .rotBoth("forearm_low", 0, 0, 0, 0, 3.5F, -9, 0, -4, 8, 0, 0, 0)
            .snap("hand_top_r", 0, 0, 0, 0, 3, 0, 0, 0, 3.12F, 0, 25, 0, 5.5F, 0, 25, 0, 5.7F, 0, 0, 0, 8, 0, 0, 0)
            .snap("hand_mid_l", 0, 0, 0, 0, 5, 0, 0, 0, 5.1F, -20, 0, 10, 6.4F, -20, 0, 10, 6.6F, 0, 0, 0, 8, 0, 0, 0)
            .snap("hand_low_r", 0, 0, 0, 0, 1, 0, 0, 0, 1.1F, 15, -20, 0, 2.2F, 15, -20, 0, 2.4F, 0, 0, 0, 8, 0, 0, 0)
            .build();

    /** Rises out of the ground folded and bowed, then unfurls pair by pair as the halo opens. */
    public static final AnimationDefinition SPAWN = new Anim(3.5F)
            .pos("all", 0, 0, -46, 0, 1.2F, 0, -18, 0, 2.2F, 0, 0, 0, 3.5F, 0, 0, 0)
            .rot("torso", 0, 25, 0, 0, 2.3F, 25, 0, 0, 2.8F, -5, 0, 0, 3.3F, 0, 0, 0)
            .snap("head", 0, 40, 0, 0, 2.4F, 40, 0, 0, 2.6F, -12, 0, 6, 3.1F, 0, 0, 0, 3.5F, 0, 0, 0)
            .rotBoth("arm_low", 0, -15, 0, -25, 2.0F, -15, 0, -25, 2.4F, 0, 0, 0)
            .rotBoth("forearm_low", 0, -40, 0, 10, 2.0F, -40, 0, 10, 2.4F, 0, 0, 0)
            .rotBoth("arm_mid", 0, -20, 0, -75, 2.3F, -20, 0, -75, 2.7F, 0, 0, 6, 3.1F, 0, 0, 0)
            .rotBoth("forearm_mid", 0, -30, 0, 20, 2.3F, -30, 0, 20, 2.7F, 0, 0, 0)
            .rotBoth("arm_top", 0, -10, 0, -100, 2.6F, -10, 0, -100, 3.0F, 0, 0, 8, 3.4F, 0, 0, 0)
            .rotBoth("forearm_top", 0, -40, 0, -38, 2.6F, -40, 0, -38, 3.0F, 0, 0, 0)
            .scale("halo", 0, 0.02F, 0.02F, 1, 2.0F, 0.02F, 0.02F, 1, 2.9F, 1.2F, 1.2F, 1, 3.3F, 1, 1, 1)
            .build();

    /** Right arms rake across on tick 14; the left arms answer on tick 24 once she is enraged. */
    public static final AnimationDefinition SWEEP = new Anim(1.7F)
            .rot("torso", 0, 0, 0, 0, 0.45F, -4, 32, 0, 0.7F, 14, -38, 0, 0.95F, 10, -20, 0, 1.2F, 12, 30, 0,
                    1.45F, 4, 8, 0, 1.7F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.45F, 0, -20, 0, 0.7F, 6, 20, 0, 1.2F, 6, -15, 0, 1.7F, 0, 0, 0)
            .rot("arm_top_r", 0, 0, 0, 0, 0.45F, 35, 0, 10, 0.68F, -85, 0, -35, 0.95F, -60, 0, -45, 1.3F, -10, 0, -10,
                    1.7F, 0, 0, 0)
            .rot("forearm_top_r", 0, 0, 0, 0, 0.45F, -20, 0, 20, 0.68F, 5, 0, -30, 1.3F, 0, 0, 0)
            .rot("arm_mid_r", 0, 0, 0, 0, 0.45F, 40, 0, 5, 0.7F, -80, 0, -10, 0.95F, -55, 0, -25, 1.3F, -10, 0, 0,
                    1.7F, 0, 0, 0)
            .rot("forearm_mid_r", 0, 0, 0, 0, 0.45F, -25, 0, 0, 0.7F, 25, 0, 20, 1.3F, 0, 0, 0)
            .rot("arm_low_r", 0, 0, 0, 0, 0.45F, 30, 0, 15, 0.72F, -95, 0, 35, 0.95F, -70, 0, 20, 1.3F, -10, 0, 0,
                    1.7F, 0, 0, 0)
            .rot("forearm_low_r", 0, 0, 0, 0, 0.45F, -10, 0, -10, 0.72F, 30, 0, 25, 1.3F, 0, 0, 0)
            .rot("arm_top_l", 0, 0, 0, 0, 0.7F, 30, 0, -10, 0.95F, 35, 0, -12, 1.2F, -85, 0, 35, 1.45F, -40, 0, 15,
                    1.7F, 0, 0, 0)
            .rot("forearm_top_l", 0, 0, 0, 0, 0.95F, -20, 0, -20, 1.2F, 5, 0, 30, 1.6F, 0, 0, 0)
            .rot("arm_mid_l", 0, 0, 0, 0, 0.7F, 35, 0, -5, 0.95F, 40, 0, -5, 1.2F, -80, 0, 10, 1.45F, -35, 0, 0,
                    1.7F, 0, 0, 0)
            .rot("forearm_mid_l", 0, 0, 0, 0, 0.95F, -25, 0, 0, 1.2F, 25, 0, -20, 1.6F, 0, 0, 0)
            .rot("arm_low_l", 0, 0, 0, 0, 0.7F, 25, 0, -15, 0.95F, 30, 0, -15, 1.2F, -95, 0, -35, 1.45F, -40, 0, -10,
                    1.7F, 0, 0, 0)
            .rot("forearm_low_l", 0, 0, 0, 0, 0.95F, -10, 0, 10, 1.2F, 30, 0, -25, 1.6F, 0, 0, 0)
            .build();

    /** Upper arms lift the halo while it pulses once per eye thrown (every 4 ticks from tick 16). */
    public static final AnimationDefinition EYES = new Anim(2.2F)
            .rot("torso", 0, 0, 0, 0, 0.5F, -7, 0, 0, 1.6F, -7, 0, 0, 2.2F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.5F, -16, 0, 0, 1.6F, -16, 0, 0, 2.2F, 0, 0, 0)
            .rotBoth("arm_top", 0, 0, 0, 0, 0.5F, -10, 0, 28, 1.6F, -14, 0, 32, 2.2F, 0, 0, 0)
            .rotBoth("forearm_top", 0, 0, 0, 0, 0.5F, 0, 0, -25, 1.6F, 0, 0, -28, 2.2F, 0, 0, 0)
            .rotBoth("arm_mid", 0, 0, 0, 0, 0.5F, -55, 0, 0, 0.8F, -64, 0, 0, 1.0F, -52, 0, 0, 1.2F, -64, 0, 0,
                    1.4F, -52, 0, 0, 1.6F, -64, 0, 0, 2.2F, 0, 0, 0)
            .rotBoth("forearm_mid", 0, 0, 0, 0, 0.5F, 20, 0, 24, 1.6F, 20, 0, 24, 2.2F, 0, 0, 0)
            .snap("halo", Target.SCALE, 0, 1, 1, 1, 0.7F, 1.05F, 1.05F, 1, 0.8F, 1.3F, 1.3F, 1, 0.95F, 1.05F, 1.05F, 1,
                    1.0F, 1.3F, 1.3F, 1, 1.15F, 1.05F, 1.05F, 1, 1.2F, 1.3F, 1.3F, 1, 1.35F, 1.05F, 1.05F, 1,
                    1.4F, 1.25F, 1.25F, 1, 1.55F, 1.05F, 1.05F, 1, 1.6F, 1.25F, 1.25F, 1, 1.8F, 1.05F, 1.05F, 1,
                    2.2F, 1, 1, 1)
            .build();

    /** A long, levitating build-up with every arm spread; the halo flares on tick 56. */
    public static final AnimationDefinition GAZE = new Anim(4.0F)
            .pos("all", 0, 0, 0, 0, 2.6F, 0, 7, 0, 2.8F, 0, 8, 0, 3.3F, 0, 0, 0, 4, 0, 0, 0)
            .rot("torso", 0, 0, 0, 0, 2.6F, -10, 0, 0, 2.9F, 12, 0, 0, 4, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 2.6F, -28, 0, 0, 2.8F, -30, 0, 0, 2.9F, 22, 0, 0, 3.5F, 10, 0, 0, 4, 0, 0, 0)
            .rotBoth("arm_top", 0, 0, 0, 0, 2.6F, -5, 0, 34, 2.85F, -25, 0, 10, 3.5F, 0, 0, 0)
            .rotBoth("forearm_top", 0, 0, 0, 0, 2.6F, 10, 0, -30, 2.85F, -10, 0, 10, 3.5F, 0, 0, 0)
            .rotBoth("arm_mid", 0, 0, 0, 0, 2.6F, 8, 0, 14, 2.85F, -35, 0, -5, 3.5F, 0, 0, 0)
            .rotBoth("forearm_mid", 0, 0, 0, 0, 2.6F, 25, 0, 26, 2.85F, 0, 0, 0, 3.5F, 0, 0, 0)
            .rotBoth("arm_low", 0, 0, 0, 0, 2.6F, 0, 0, 48, 2.85F, -30, 0, 20, 3.5F, 0, 0, 0)
            .rotBoth("forearm_low", 0, 0, 0, 0, 2.6F, 28, 0, 26, 2.85F, 0, 0, 0, 3.5F, 0, 0, 0)
            .rot("veil_front", 0, 0, 0, 0, 2.8F, 0, 0, 0, 2.92F, -38, 0, 0, 3.6F, 0, 0, 0)
            .scale("halo", 0, 1, 1, 1, 2.6F, 1.45F, 1.45F, 1, 2.78F, 1.5F, 1.5F, 1, 2.88F, 1.9F, 1.9F, 1,
                    3.4F, 1, 1, 1, 4, 1, 1, 1)
            .build();

    /** Two hands cast the threads forward, then wrench them back on tick 14. */
    public static final AnimationDefinition STRINGS = new Anim(2.0F)
            .rot("torso", 0, 0, 0, 0, 0.45F, 10, 0, 0, 0.85F, -14, 0, 0, 1.4F, -8, 0, 0, 2, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.45F, 5, 0, 0, 0.85F, -10, 0, 8, 2, 0, 0, 0)
            .rot("arm_top_r", 0, 0, 0, 0, 0.45F, -88, 0, -20, 0.7F, -70, 0, -22, 0.85F, -20, 0, 25, 1.4F, -15, 0, 20,
                    2, 0, 0, 0)
            .rot("forearm_top_r", 0, 0, 0, 0, 0.45F, 10, 0, -34, 0.85F, -40, 0, 10, 1.4F, -30, 0, 5, 2, 0, 0, 0)
            .snap("hand_top_r", 0, 0, 0, 0, 0.45F, -30, 0, 0, 0.7F, -30, 0, 0, 0.8F, 45, 0, 0, 1.4F, 45, 0, 0, 2, 0, 0, 0)
            .rot("arm_mid_l", 0, 0, 0, 0, 0.45F, -72, 0, 6, 0.7F, -60, 0, 8, 0.85F, -15, 0, -18, 1.4F, -10, 0, -14,
                    2, 0, 0, 0)
            .rot("forearm_mid_l", 0, 0, 0, 0, 0.45F, 26, 0, -22, 0.85F, -30, 0, 0, 1.4F, -20, 0, 0, 2, 0, 0, 0)
            .snap("hand_mid_l", 0, 0, 0, 0, 0.45F, -30, 0, 0, 0.7F, -30, 0, 0, 0.8F, 45, 0, 0, 1.4F, 45, 0, 0, 2, 0, 0, 0)
            .build();

    /** Folds in and thins to a thread; the server moves her on tick 16 while nothing is visible. */
    public static final AnimationDefinition VEIL_STEP = new Anim(1.8F)
            .scale("all", 0, 1, 1, 1, 0.55F, 0.5F, 1.12F, 0.5F, 0.7F, 0.03F, 1.3F, 0.03F, 0.9F, 0.03F, 1.3F, 0.03F,
                    1.1F, 1.12F, 0.94F, 1.12F, 1.35F, 1, 1, 1, 1.8F, 1, 1, 1)
            .rot("head", 0, 0, 0, 0, 0.5F, 30, 0, 0, 1.0F, 30, 0, 0, 1.4F, -8, 0, 0, 1.8F, 0, 0, 0)
            .rotBoth("arm_top", 0, 0, 0, 0, 0.5F, -45, 0, -85, 1.0F, -45, 0, -85, 1.4F, 0, 0, 10, 1.8F, 0, 0, 0)
            .rotBoth("forearm_top", 0, 0, 0, 0, 0.5F, -30, 0, -30, 1.0F, -30, 0, -30, 1.4F, 0, 0, 0)
            .rotBoth("arm_mid", 0, 0, 0, 0, 0.5F, -50, 0, -55, 1.0F, -50, 0, -55, 1.4F, 0, 0, 8, 1.8F, 0, 0, 0)
            .rotBoth("arm_low", 0, 0, 0, 0, 0.5F, -30, 0, -20, 1.0F, -30, 0, -20, 1.4F, 0, 0, 0)
            .build();

    /** Hunches and shudders, then snaps open as the second phase bursts out on tick 30. */
    public static final AnimationDefinition PHASE = new Anim(3.5F)
            .pos("all", 0, 0, 0, 0, 1.35F, 0, -3, 0, 1.5F, 0, 9, 0, 2.6F, 0, 7, 0, 3.5F, 0, 0, 0)
            .rot("torso", 0, 0, 0, 0, 0.6F, 30, 0, 0, 1.35F, 38, 0, 0, 1.5F, -22, 0, 0, 2.6F, -18, 0, 0, 3.5F, 0, 0, 0)
            .snap("chest", 0, 0, 0, 0, 0.3F, 0, 0, 3, 0.4F, 0, 0, -3, 0.5F, 0, 0, 3, 0.6F, 0, 0, -4, 0.7F, 0, 0, 4,
                    0.8F, 0, 0, -4, 0.9F, 0, 0, 5, 1.0F, 0, 0, -5, 1.1F, 0, 0, 5, 1.2F, 0, 0, -6, 1.3F, 0, 0, 6,
                    1.4F, 0, 0, -6, 1.5F, 0, 0, 0, 3.5F, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 0.6F, 32, 0, 0, 1.35F, 36, 0, 10, 1.5F, -38, 0, 0, 2.6F, -30, 0, -6, 3.5F, 0, 0, 0)
            .rotBoth("arm_top", 0, 0, 0, 0, 0.6F, -55, 0, -70, 1.35F, -60, 0, -75, 1.5F, 10, 0, 38, 2.6F, 6, 0, 32,
                    3.5F, 0, 0, 0)
            .rotBoth("forearm_top", 0, 0, 0, 0, 0.6F, -40, 0, -20, 1.35F, -45, 0, -20, 1.5F, 10, 0, -32, 2.6F, 8, 0, -28,
                    3.5F, 0, 0, 0)
            .rotBoth("arm_mid", 0, 0, 0, 0, 0.6F, -60, 0, -40, 1.35F, -65, 0, -45, 1.5F, 12, 0, 22, 2.6F, 8, 0, 18,
                    3.5F, 0, 0, 0)
            .rotBoth("forearm_mid", 0, 0, 0, 0, 0.6F, -35, 0, 10, 1.35F, -40, 0, 10, 1.5F, 25, 0, 26, 2.6F, 22, 0, 24,
                    3.5F, 0, 0, 0)
            .rotBoth("arm_low", 0, 0, 0, 0, 0.6F, -45, 0, -15, 1.35F, -50, 0, -18, 1.5F, 5, 0, 55, 2.6F, 4, 0, 48,
                    3.5F, 0, 0, 0)
            .rotBoth("forearm_low", 0, 0, 0, 0, 0.6F, -30, 0, 10, 1.35F, -30, 0, 10, 1.5F, 28, 0, 28, 2.6F, 25, 0, 25,
                    3.5F, 0, 0, 0)
            .scale("halo", 0, 1, 1, 1, 1.35F, 0.6F, 0.6F, 1, 1.5F, 1.9F, 1.9F, 1, 2.6F, 1.5F, 1.5F, 1, 3.5F, 1, 1, 1)
            .build();

    /** Her strings are cut one arm at a time; then the head drops and the body folds and sinks. */
    public static final AnimationDefinition DEATH = new Anim(5.0F)
            .snap("arm_top_r", 0, 0, 0, 0, 0.25F, 0, 0, 15, 0.5F, 0, 0, 15, 0.65F, 15, 0, -102, 5, 15, 0, -102)
            .snap("forearm_top_r", 0.5F, 0, 0, 0, 0.7F, -10, 0, -36, 5, -10, 0, -36)
            .snap("arm_mid_l", 0, 0, 0, 0, 0.25F, 0, 0, -8, 0.9F, 0, 0, -8, 1.05F, 10, 0, 74, 5, 10, 0, 74)
            .snap("forearm_mid_l", 0.9F, 0, 0, 0, 1.1F, 20, 0, -20, 5, 20, 0, -20)
            .snap("arm_low_r", 0, 0, 0, 0, 0.25F, 0, 0, 12, 1.2F, 0, 0, 12, 1.35F, 5, 0, -26, 5, 5, 0, -26)
            .snap("arm_top_l", 0, 0, 0, 0, 0.25F, 0, 0, -15, 1.5F, 0, 0, -15, 1.65F, 15, 0, 102, 5, 15, 0, 102)
            .snap("forearm_top_l", 1.5F, 0, 0, 0, 1.7F, -10, 0, 36, 5, -10, 0, 36)
            .snap("arm_mid_r", 0, 0, 0, 0, 0.25F, 0, 0, 8, 1.8F, 0, 0, 8, 1.95F, 10, 0, -74, 5, 10, 0, -74)
            .snap("forearm_mid_r", 1.8F, 0, 0, 0, 2.0F, 20, 0, 20, 5, 20, 0, 20)
            .snap("arm_low_l", 0, 0, 0, 0, 0.25F, 0, 0, -12, 2.1F, 0, 0, -12, 2.25F, 5, 0, 26, 5, 5, 0, 26)
            .snap("head", 0, 0, 0, 0, 0.25F, -15, 0, 0, 2.3F, -15, 0, 5, 2.5F, 55, 0, 18, 5, 55, 0, 18)
            .rot("torso", 0, 0, 0, 0, 0.25F, -8, 0, 0, 2.5F, -4, 0, 3, 3.3F, 62, 0, 6, 5, 70, 0, 6)
            .rot("chest", 2.5F, 0, 0, 0, 3.4F, 25, 0, 0, 5, 25, 0, 0)
            .rot("skirt", 2.5F, 0, 0, 0, 3.5F, -8, 0, 0, 5, -8, 0, 0)
            .pos("all", 0, 0, 0, 0, 2.5F, 0, 0, 0, 3.5F, 0, -10, 0, 5, 0, -22, 0)
            .scale("all", 3.6F, 1, 1, 1, 5, 0.85F, 0.55F, 0.85F)
            .pos("halo", 0, 0, 0, 0, 2.5F, 0, 4, 0, 5, 0, 36, 0)
            .scale("halo", 0, 1, 1, 1, 3.5F, 1.1F, 1.1F, 1, 5, 0.5F, 0.5F, 1)
            .build();

    enum Target {
        ROTATION, POSITION, SCALE
    }

    private static final class Anim {
        private final AnimationDefinition.Builder builder;

        Anim(float seconds) {
            this.builder = AnimationDefinition.Builder.withLength(seconds);
        }

        Anim loop() {
            this.builder.looping();
            return this;
        }

        /** Smooth (Catmull-Rom) rotation. */
        Anim rot(String part, float... keys) {
            return this.add(part, Target.ROTATION, AnimationChannel.Interpolations.CATMULLROM, false, keys);
        }

        /** Linear rotation: used for the abrupt, puppet-like moves. */
        Anim snap(String part, float... keys) {
            return this.add(part, Target.ROTATION, AnimationChannel.Interpolations.LINEAR, false, keys);
        }

        Anim snap(String part, Target target, float... keys) {
            return this.add(part, target, AnimationChannel.Interpolations.LINEAR, false, keys);
        }

        /** Same motion on both sides; values are written for the right arm and mirrored for the left. */
        Anim rotBoth(String part, float... keys) {
            this.add(part + "_r", Target.ROTATION, AnimationChannel.Interpolations.CATMULLROM, false, keys);
            return this.add(part + "_l", Target.ROTATION, AnimationChannel.Interpolations.CATMULLROM, true, keys);
        }

        /** Position in model pixels, +y up. */
        Anim pos(String part, float... keys) {
            return this.add(part, Target.POSITION, AnimationChannel.Interpolations.CATMULLROM, false, keys);
        }

        Anim scale(String part, float... keys) {
            return this.add(part, Target.SCALE, AnimationChannel.Interpolations.CATMULLROM, false, keys);
        }

        private Anim add(String part, Target target, AnimationChannel.Interpolation interpolation, boolean mirror,
                         float... keys) {
            if (keys.length % 4 != 0) {
                throw new IllegalArgumentException("Keyframes for " + part + " must be time, x, y, z");
            }
            Keyframe[] frames = new Keyframe[keys.length / 4];
            float side = mirror ? -1.0F : 1.0F;
            for (int i = 0; i < frames.length; i++) {
                float x = keys[i * 4 + 1];
                float y = keys[i * 4 + 2];
                float z = keys[i * 4 + 3];
                frames[i] = new Keyframe(keys[i * 4], switch (target) {
                    case ROTATION -> KeyframeAnimations.degreeVec(x, y * side, z * side);
                    case POSITION -> KeyframeAnimations.posVec(x, y, z);
                    case SCALE -> KeyframeAnimations.scaleVec(x, y, z);
                }, interpolation);
            }
            AnimationChannel.Target channelTarget = switch (target) {
                case ROTATION -> AnimationChannel.Targets.ROTATION;
                case POSITION -> AnimationChannel.Targets.POSITION;
                case SCALE -> AnimationChannel.Targets.SCALE;
            };
            this.builder.addAnimation(part, new AnimationChannel(channelTarget, frames));
            return this;
        }

        AnimationDefinition build() {
            return this.builder.build();
        }
    }

    private GoddessAnimations() {
    }
}
