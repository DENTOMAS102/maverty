package com.dentomas.deathgoddess.client.model;

import com.dentomas.deathgoddess.entity.GoddessAction;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Keyframe animations. Every channel is a flat list of {@code time, x, y, z} quadruples, and an
 * action's animation is exactly as long as the action (see {@link GoddessAction}); the comments mark
 * the seconds at which the server lands each effect.
 *
 * <p>Arms can be posed absolutely, which is far easier to choreograph than offsets from the rest
 * pose. For an upper arm {@code (x, 0, z)}: {@code z} swings it sideways from hanging (0) through
 * horizontal (90) to overhead (180), and negative {@code x} brings it forward, so {@code (-90, 0, z)}
 * points straight ahead. A forearm at {@code (0, 0, 0)} continues its arm in a straight line. Both
 * sides are written as the right arm; the left is mirrored. {@link #R} returns a joint to rest.
 */
public final class GoddessAnimations {
    /** Marks a keyframe that returns the joint to its rest pose. */
    private static final float R = Float.NaN;
    private static final String[] PAIRS = {"top", "mid", "low"};
    private static final Map<GoddessAction, AnimationDefinition> ACTIONS = new EnumMap<>(GoddessAction.class);

    /** A slow wave that travels down the three pairs of arms, like breath. */
    public static final AnimationDefinition IDLE = new Anim(8.0F).loop()
            .rot("torso", 0, 0, 0, 0, 2, 1.5F, 3, 1, 4, 0, 0, 0, 6, 1.5F, -3, -1, 8, 0, 0, 0)
            .rot("head", 0, 0, 0, 0, 2, -2, 4, 5, 4, 0, 0, 0, 6, -2, -4, -5, 8, 0, 0, 0)
            .rotBoth("arm_top", 0, 0, 0, 0, 2, -3, 0, 7, 4, 0, 0, 0, 6, 3, 0, -5, 8, 0, 0, 0)
            .rotBoth("arm_mid", 0, 0, 0, -4, 2, 0, 0, 0, 4, -3, 0, 6, 6, 0, 0, 0, 8, 0, 0, -4)
            .rotBoth("arm_low", 0, 2, 0, -3, 2, 0, 0, -5, 4, 0, 0, 0, 6, -3, 0, 5, 8, 2, 0, -3)
            .rotBoth("forearm_top", 0, 0, 0, 0, 2.5F, -4, 0, 8, 5, 0, 0, -3, 8, 0, 0, 0)
            .rotBoth("forearm_mid", 0, 0, 0, 0, 3, -8, 0, 5, 6, 2, 0, 0, 8, 0, 0, 0)
            .rotBoth("forearm_low", 0, 0, 0, 0, 3.5F, -9, 0, -4, 8, 0, 0, 0)
            .rotBoth("hand_top", 0, 0, 0, 0, 2, 10, 15, 0, 5, -8, -10, 0, 8, 0, 0, 0)
            .rotBoth("hand_mid", 0, 0, 0, 0, 3, -12, 0, 8, 6, 8, 0, -5, 8, 0, 0, 0)
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

    static {
        // Rises out of the ground folded and bowed, then unfurls pair by pair as the halo opens.
        ACTIONS.put(GoddessAction.SPAWN, new Anim(GoddessAction.SPAWN)
                .pos("all", 0, 0, -46, 0, 1.2F, 0, -18, 0, 2.2F, 0, 0, 0, 3.5F, 0, 0, 0)
                .rot("torso", 0, 25, 0, 0, 2.3F, 25, 0, 0, 2.8F, -5, 0, 0, 3.3F, 0, 0, 0)
                .rot("head", 0, 40, 0, 0, 2.4F, 40, 0, 0, 2.7F, -12, 0, 6, 3.2F, 0, 0, 0, 3.5F, 0, 0, 0)
                .rotBoth("arm_low", 0, -15, 0, -25, 2.0F, -15, 0, -25, 2.4F, 0, 0, 0)
                .rotBoth("forearm_low", 0, -40, 0, 10, 2.0F, -40, 0, 10, 2.4F, 0, 0, 0)
                .rotBoth("arm_mid", 0, -20, 0, -75, 2.3F, -20, 0, -75, 2.7F, 0, 0, 6, 3.1F, 0, 0, 0)
                .rotBoth("forearm_mid", 0, -30, 0, 20, 2.3F, -30, 0, 20, 2.7F, 0, 0, 0)
                .rotBoth("arm_top", 0, -10, 0, -100, 2.6F, -10, 0, -100, 3.0F, 0, 0, 8, 3.4F, 0, 0, 0)
                .rotBoth("forearm_top", 0, -40, 0, -38, 2.6F, -40, 0, -38, 3.0F, 0, 0, 0)
                .scale("halo", 0, 0.02F, 0.02F, 1, 2.0F, 0.02F, 0.02F, 1, 2.9F, 1.2F, 1.2F, 1, 3.3F, 1, 1, 1)
                .build());

        // Hunches and shudders, then snaps open as the second phase bursts out at 1.5 s.
        ACTIONS.put(GoddessAction.PHASE, new Anim(GoddessAction.PHASE)
                .pos("all", 0, 0, 0, 0, 1.35F, 0, -3, 0, 1.5F, 0, 9, 0, 2.6F, 0, 7, 0, 3.5F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 0.6F, 30, 0, 0, 1.35F, 38, 0, 0, 1.5F, -22, 0, 0, 2.6F, -18, 0, 0, 3.5F, 0, 0, 0)
                .snap("chest", 0, 0, 0, 0, 0.3F, 0, 0, 3, 0.4F, 0, 0, -3, 0.5F, 0, 0, 3, 0.6F, 0, 0, -4, 0.7F, 0, 0, 4,
                        0.8F, 0, 0, -4, 0.9F, 0, 0, 5, 1.0F, 0, 0, -5, 1.1F, 0, 0, 5, 1.2F, 0, 0, -6, 1.3F, 0, 0, 6,
                        1.4F, 0, 0, -6, 1.5F, 0, 0, 0, 3.5F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.6F, 32, 0, 0, 1.35F, 36, 0, 10, 1.5F, -38, 0, 0, 2.6F, -30, 0, -6, 3.5F, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 0.6F, -62, 0, -28, 1.35F, -66, 0, -30, 1.5F, 8, 0, 160, 2.6F, 4, 0, 150, 3.5F, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 0.6F, -60, 0, -20, 1.35F, -64, 0, -20, 1.5F, 0, 0, 5, 2.6F, 0, 0, 8, 3.5F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.6F, -58, 0, -24, 1.35F, -62, 0, -26, 1.5F, 10, 0, 100, 2.6F, 6, 0, 96, 3.5F, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 0.6F, -60, 0, -15, 1.35F, -64, 0, -15, 1.5F, 0, 0, 8, 2.6F, 0, 0, 8, 3.5F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.6F, -50, 0, -18, 1.35F, -54, 0, -20, 1.5F, 6, 0, 62, 2.6F, 4, 0, 56, 3.5F, R, 0, 0)
                .both("forearm_low", 0, R, 0, 0, 0.6F, -50, 0, -10, 1.35F, -52, 0, -10, 1.5F, -5, 0, 5, 2.6F, -5, 0, 5, 3.5F, R, 0, 0)
                .scale("halo", 0, 1, 1, 1, 1.35F, 0.6F, 0.6F, 1, 1.5F, 1.9F, 1.9F, 1, 2.6F, 1.5F, 1.5F, 1, 3.5F, 1, 1, 1)
                .build());

        // She turns into the wind-up and lets the right arms fan across at 1.0 s; the left arms answer at 1.35 s.
        ACTIONS.put(GoddessAction.SWEEP, new Anim(GoddessAction.SWEEP)
                .rot("torso", 0, 0, 0, 0, 0.65F, -5, 34, -4, 1.0F, 12, -36, 4, 1.35F, 10, 30, -3, 1.7F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.65F, 0, -22, 0, 1.0F, 6, 20, 0, 1.35F, 4, -16, 0, 1.7F, 0, 0, 0)
                .rot("skirt_low", 0, 0, 0, 0, 0.8F, 0, -14, 0, 1.2F, 0, 16, 0, 1.7F, 0, 0, 0)
                .fanRight("arm", 0.04F, 0, 40, 0, R, 0, 0, 0.65F, 25, 0, 105, 1.0F, -75, 0, 90, 1.18F, -60, 0, 30, 1.7F, R, 0, 0)
                .fanRight("forearm", 0.04F, 0, 0, 0, R, 0, 0, 0.65F, -35, 0, 20, 1.0F, 0, 0, 5, 1.7F, R, 0, 0)
                .fanLeft("arm", 0.04F, 0, 40, 0, R, 0, 0, 1.0F, 25, 0, 105, 1.35F, -75, 0, 90, 1.5F, -60, 0, 30, 1.7F, R, 0, 0)
                .fanLeft("forearm", 0.04F, 0, 0, 0, R, 0, 0, 1.0F, -35, 0, 20, 1.35F, 0, 0, 5, 1.7F, R, 0, 0)
                .build());

        // The upper arms frame the halo while the middle pair offers each eye forward (0.5 s onward).
        ACTIONS.put(GoddessAction.EYES, new Anim(GoddessAction.EYES)
                .rot("torso", 0, 0, 0, 0, 0.5F, -8, 0, 0, 1.5F, -6, 0, 0, 2, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.5F, -16, 0, 0, 1.5F, -14, 0, 4, 2, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 0.5F, -12, 0, 152, 1.0F, -16, 0, 146, 1.5F, -12, 0, 152, 2, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 0.5F, -10, 0, -30, 1.5F, -14, 0, -36, 2, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.55F, -62, 0, 70, 1.05F, -68, 0, 80, 1.55F, -62, 0, 70, 2, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 0.55F, -20, 0, 10, 1.55F, -20, 0, 10, 2, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.6F, -10, 0, 48, 1.6F, -10, 0, 48, 2, R, 0, 0)
                .scale("halo", 0, 1, 1, 1, 0.5F, 1.12F, 1.12F, 1, 0.75F, 1.25F, 1.25F, 1, 1.0F, 1.12F, 1.12F, 1,
                        1.25F, 1.25F, 1.25F, 1, 1.5F, 1.12F, 1.12F, 1, 2, 1, 1, 1)
                .build());

        // A slow ascension with every arm opening like wings; the halo flares at 2.0 s.
        ACTIONS.put(GoddessAction.GAZE, new Anim(GoddessAction.GAZE)
                .pos("all", 0, 0, 0, 0, 1.9F, 0, 8, 0, 2.05F, 0, 6, 0, 2.5F, 0, 0, 0, 3, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 1.9F, -10, 0, 0, 2.08F, 14, 0, 0, 3, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 1.9F, -30, 0, 0, 2.06F, 24, 0, 0, 2.6F, 8, 0, 0, 3, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 1.9F, 10, 0, 158, 2.08F, -50, 0, 120, 3, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 1.9F, 0, 0, 5, 2.08F, -10, 0, 10, 3, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 1.9F, 12, 0, 96, 2.08F, -55, 0, 75, 3, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 1.9F, 0, 0, 8, 3, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 1.9F, 8, 0, 58, 2.08F, -45, 0, 40, 3, R, 0, 0)
                .both("forearm_low", 0, R, 0, 0, 1.9F, -5, 0, 5, 3, R, 0, 0)
                .rot("veil_front", 0, 0, 0, 0, 1.95F, 0, 0, 0, 2.08F, -38, 0, 0, 2.7F, 0, 0, 0, 3, 0, 0, 0)
                .scale("halo", 0, 1, 1, 1, 1.85F, 1.45F, 1.45F, 1, 1.97F, 1.5F, 1.5F, 1, 2.05F, 1.95F, 1.95F, 1,
                        2.6F, 1, 1, 1, 3, 1, 1, 1)
                .build());

        // Four hands reach out along the threads, then draw them in to her chest at 1.0 s.
        ACTIONS.put(GoddessAction.STRINGS, new Anim(GoddessAction.STRINGS)
                .rot("torso", 0, 0, 0, 0, 0.6F, 10, 0, 0, 1.05F, -14, 0, 0, 1.5F, -6, 0, 0, 1.9F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.6F, 6, 0, -6, 1.05F, -10, 0, 6, 1.9F, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 0.6F, -82, 0, 120, 0.9F, -80, 0, 120, 1.05F, -25, 0, 95, 1.5F, -30, 0, 100,
                        1.9F, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 0.6F, 0, 0, 0, 0.9F, 0, 0, 0, 1.05F, -70, 0, -20, 1.5F, -55, 0, -10,
                        1.9F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.7F, -70, 0, 60, 0.95F, -70, 0, 60, 1.1F, -20, 0, 70, 1.9F, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 0.7F, -10, 0, 0, 0.95F, -10, 0, 0, 1.1F, -75, 0, -10, 1.9F, R, 0, 0)
                .rotBoth("hand_top", 0, 0, 0, 0, 0.6F, -30, 0, 0, 0.9F, -30, 0, 0, 1.02F, 45, 0, 0, 1.5F, 30, 0, 0, 1.9F, 0, 0, 0)
                .build());

        // Folds her arms in and thins to a thread; the server moves her at 0.7 s while nothing is visible.
        ACTIONS.put(GoddessAction.VEIL_STEP, new Anim(GoddessAction.VEIL_STEP)
                .snap("all", Target.SCALE, 0, 1, 1, 1, 0.45F, 0.55F, 1.1F, 0.55F, 0.6F, 0.03F, 1.3F, 0.03F,
                        0.8F, 0.03F, 1.3F, 0.03F, 0.95F, 1.1F, 0.95F, 1.1F, 1.15F, 1, 1, 1, 1.5F, 1, 1, 1)
                .rot("head", 0, 0, 0, 0, 0.45F, 28, 0, 0, 0.85F, 28, 0, 0, 1.2F, -8, 0, 0, 1.5F, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 0.45F, -60, 0, -30, 0.85F, -60, 0, -30, 1.2F, 0, 0, 125, 1.5F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.45F, -55, 0, -25, 0.85F, -55, 0, -25, 1.2F, 0, 0, 95, 1.5F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.45F, -40, 0, -15, 0.85F, -40, 0, -15, 1.2F, 0, 0, 40, 1.5F, R, 0, 0)
                .build());

        // Bows to the earth, then lifts what is buried there; the graves open at 1.8 s.
        ACTIONS.put(GoddessAction.GRAVE_BLOOM, new Anim(GoddessAction.GRAVE_BLOOM)
                .pos("all", 0, 0, 0, 0, 0.5F, 0, -3, 0, 1.7F, 0, 2, 0, 1.85F, 0, 4, 0, 2.3F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 0.5F, 26, 0, 0, 1.7F, 2, 0, 0, 1.85F, -10, 0, 0, 2.3F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.5F, 18, 0, 0, 1.7F, -6, 0, 0, 1.85F, -20, 0, 0, 2.3F, 0, 0, 0)
                .fanBoth("arm", 0.05F, 0, 25, 0, R, 0, 0, 0.5F, -55, 0, 25, 1.7F, -45, 0, 95, 1.85F, -10, 0, 150, 2.3F, R, 0, 0)
                .fanBoth("forearm", 0.05F, 0, 0, 0, R, 0, 0, 0.5F, -10, 0, 0, 1.7F, -50, 0, 10, 1.85F, -5, 0, 0, 2.3F, R, 0, 0)
                .rotBoth("hand_top", 0, 0, 0, 0, 0.5F, 30, 0, 0, 1.7F, -40, 0, 0, 2.3F, 0, 0, 0)
                .build());

        // Two long diagonal cuts, right hand then left; they land at 1.6 s and 2.1 s.
        ACTIONS.put(GoddessAction.SEVERANCE, new Anim(GoddessAction.SEVERANCE)
                .rot("torso", 0, 0, 0, 0, 0.7F, -6, 28, 5, 1.45F, -8, 30, 5, 1.62F, 14, -24, -4, 1.95F, -4, -26, -5,
                        2.12F, 14, 22, 4, 2.5F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.7F, -8, -16, 0, 1.62F, 10, 14, 0, 2.12F, 10, -12, 0, 2.5F, 0, 0, 0)
                .rot("skirt_low", 0, 0, 0, 0, 1.0F, 0, 10, 0, 1.7F, 0, -14, 0, 2.2F, 0, 12, 0, 2.5F, 0, 0, 0)
                .right("arm_top", 0, R, 0, 0, 0.7F, 15, 0, 165, 1.45F, 20, 0, 168, 1.62F, -70, 0, -15, 2.0F, -40, 0, 10,
                        2.5F, R, 0, 0)
                .right("forearm_top", 0, R, 0, 0, 0.7F, -20, 0, 15, 1.45F, -25, 0, 15, 1.62F, 0, 0, 0, 2.5F, R, 0, 0)
                .left("arm_top", 0, R, 0, 0, 1.2F, R, 0, 0, 1.75F, 15, 0, 165, 1.95F, 20, 0, 168, 2.12F, -70, 0, -15,
                        2.5F, R, 0, 0)
                .left("forearm_top", 0, R, 0, 0, 1.2F, R, 0, 0, 1.95F, -25, 0, 15, 2.12F, 0, 0, 0, 2.5F, R, 0, 0)
                .right("arm_mid", 0, R, 0, 0, 0.7F, 10, 0, 110, 1.45F, 10, 0, 112, 1.68F, -55, 0, 10, 2.5F, R, 0, 0)
                .left("arm_mid", 0, R, 0, 0, 1.3F, R, 0, 0, 1.95F, 10, 0, 112, 2.18F, -55, 0, 10, 2.5F, R, 0, 0)
                .build());

        // Lifts all six arms like a bell-ringer and beats them down on each toll: 1.5, 1.9, 2.3, 2.7 and 3.1 s.
        ACTIONS.put(GoddessAction.DEATH_KNELL, new Anim(GoddessAction.DEATH_KNELL)
                .pos("all", 0, 0, 0, 0, 1.2F, 0, 5, 0, 1.5F, 0, -1, 0, 1.7F, 0, 3, 0, 1.9F, 0, -1, 0, 2.1F, 0, 3, 0,
                        2.3F, 0, -1, 0, 2.5F, 0, 3, 0, 2.7F, 0, -1, 0, 2.9F, 0, 2, 0, 3.1F, 0, -1, 0, 3.5F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 1.2F, -8, 0, 0, 1.5F, 10, 0, 0, 1.7F, -4, 0, 0, 1.9F, 10, 0, 0, 2.1F, -4, 0, 0,
                        2.3F, 10, 0, 0, 2.5F, -4, 0, 0, 2.7F, 10, 0, 0, 2.9F, -3, 0, 0, 3.1F, 8, 0, 0, 3.5F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 1.2F, -18, 0, 0, 1.5F, 14, 0, 0, 1.7F, -8, 0, 0, 1.9F, 14, 0, 0, 2.1F, -8, 0, 0,
                        2.3F, 14, 0, 0, 2.5F, -8, 0, 0, 2.7F, 14, 0, 0, 2.9F, -6, 0, 0, 3.1F, 12, 0, 0, 3.5F, 0, 0, 0)
                .fanBoth("arm", 0.03F, 0, 30, 0, R, 0, 0, 1.2F, 0, 0, 150, 1.5F, -20, 0, 75, 1.7F, -5, 0, 140,
                        1.9F, -20, 0, 75, 2.1F, -5, 0, 140, 2.3F, -20, 0, 75, 2.5F, -5, 0, 140, 2.7F, -20, 0, 75,
                        2.9F, -5, 0, 130, 3.1F, -20, 0, 75, 3.5F, R, 0, 0)
                .fanBoth("forearm", 0.03F, 0, 0, 0, R, 0, 0, 1.2F, -10, 0, 10, 3.1F, -10, 0, 10, 3.5F, R, 0, 0)
                .build());

        // Arms lifted to the sky, swaying, the fingers trickling the rain down (1.6 s to 3.8 s).
        ACTIONS.put(GoddessAction.BLACK_RAIN, new Anim(GoddessAction.BLACK_RAIN)
                .rot("torso", 0, 0, 0, 0, 0.5F, -10, 0, 0, 1.1F, -10, 0, 7, 1.9F, -10, 0, -7, 2.7F, -10, 0, 7,
                        3.2F, -8, 0, 0, 3.6F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.5F, -28, 0, 0, 1.1F, -28, 0, -8, 1.9F, -28, 0, 8, 2.7F, -28, 0, -8,
                        3.2F, -20, 0, 0, 3.6F, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 0.5F, -8, 0, 170, 1.1F, -14, 0, 160, 1.9F, -6, 0, 172, 2.7F, -14, 0, 160,
                        3.2F, -8, 0, 168, 3.6F, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 0.5F, -10, 0, 0, 1.1F, -25, 0, -10, 1.9F, -5, 0, 5, 2.7F, -25, 0, -10,
                        3.6F, R, 0, 0)
                .right("arm_mid", 0, R, 0, 0, 0.6F, -30, 0, 110, 1.1F, -45, 0, 80, 1.9F, -25, 0, 125, 2.7F, -45, 0, 80,
                        3.6F, R, 0, 0)
                .left("arm_mid", 0, R, 0, 0, 0.6F, -30, 0, 110, 1.1F, -25, 0, 125, 1.9F, -45, 0, 80, 2.7F, -25, 0, 125,
                        3.6F, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 0.6F, -20, 0, 0, 3.2F, -20, 0, 0, 3.6F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.7F, -15, 0, 55, 3.1F, -15, 0, 55, 3.6F, R, 0, 0)
                .rotBoth("hand_top", 0, 0, 0, 0, 0.7F, 25, 0, 0, 1.0F, -25, 0, 0, 1.3F, 25, 0, 0, 1.6F, -25, 0, 0,
                        1.9F, 25, 0, 0, 2.2F, -25, 0, 0, 2.5F, 25, 0, 0, 2.8F, -25, 0, 0, 3.6F, 0, 0, 0)
                .build());

        // The right hand draws the upright stroke and the left the crossbar; they part as it opens at 1.7 s and 2.3 s.
        ACTIONS.put(GoddessAction.CROSS, new Anim(GoddessAction.CROSS)
                .rot("torso", 0, 0, 0, 0, 0.4F, -4, 10, 0, 1.0F, -4, -10, 0, 1.55F, 6, 0, 0, 1.72F, -12, 0, 0,
                        2.2F, 2, 0, 0, 2.32F, -8, 0, 0, 2.6F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.5F, 6, 0, 8, 1.1F, 6, 0, -8, 1.72F, -14, 0, 0, 2.6F, 0, 0, 0)
                .right("arm_top", 0, R, 0, 0, 0.3F, -50, 0, 170, 0.85F, -75, 0, 20, 1.55F, -60, 0, -30, 1.72F, 5, 0, 115,
                        2.2F, -30, 0, 90, 2.32F, 5, 0, 120, 2.6F, R, 0, 0)
                .left("arm_top", 0, R, 0, 0, 0.55F, -70, 0, -30, 1.1F, -70, 0, 95, 1.55F, -60, 0, -30, 1.72F, 5, 0, 115,
                        2.2F, -30, 0, 90, 2.32F, 5, 0, 120, 2.6F, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 0.4F, -5, 0, 0, 1.5F, -25, 0, -10, 1.72F, 0, 0, 5, 2.6F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.5F, -40, 0, 60, 1.55F, -55, 0, -25, 1.75F, 8, 0, 95, 2.2F, -25, 0, 70,
                        2.35F, 8, 0, 98, 2.6F, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 0.5F, -30, 0, 0, 1.55F, -40, 0, -10, 1.75F, 0, 0, 5, 2.6F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 1.55F, -30, 0, -10, 1.78F, 5, 0, 60, 2.6F, R, 0, 0)
                .build());

        // One long finger singles the victim out and follows them; the hand falls at 2.3 s.
        ACTIONS.put(GoddessAction.MARK, new Anim(GoddessAction.MARK)
                .rot("torso", 0, 0, 0, 0, 0.5F, 4, -12, 0, 2.15F, 4, -12, 0, 2.32F, 12, -6, 0, 2.8F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.5F, 4, 0, 14, 1.3F, 4, 0, 16, 2.15F, 2, 0, 14, 2.32F, 14, 0, 0, 2.8F, 0, 0, 0)
                .right("arm_top", 0, R, 0, 0, 0.5F, -84, 0, 60, 1.3F, -88, 0, 70, 2.12F, -100, 0, 90, 2.32F, -35, 0, 5,
                        2.8F, R, 0, 0)
                .right("forearm_top", 0, R, 0, 0, 0.5F, 0, 0, 0, 2.12F, -8, 0, 0, 2.32F, -30, 0, 0, 2.8F, R, 0, 0)
                .rot("hand_top_r", 0, 0, 0, 0, 0.5F, 0, 40, 0, 1.3F, 0, -40, 0, 2.12F, 0, 40, 0, 2.32F, 50, 0, 0, 2.8F, 0, 0, 0)
                .left("arm_top", 0, R, 0, 0, 0.6F, -50, 0, -25, 2.2F, -50, 0, -25, 2.8F, R, 0, 0)
                .left("forearm_top", 0, R, 0, 0, 0.6F, -60, 0, -20, 2.2F, -60, 0, -20, 2.8F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.6F, -10, 0, 70, 2.15F, -12, 0, 72, 2.34F, -30, 0, 30, 2.8F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.7F, -25, 0, 5, 2.2F, -25, 0, 5, 2.8F, R, 0, 0)
                .build());

        // A loom: right hands rise as the left fall, and each side pulls its threads down in turn (1.7 s, 2.5 s).
        ACTIONS.put(GoddessAction.LATTICE, new Anim(GoddessAction.LATTICE)
                .pos("all", 0, 0, 0, 0, 0.6F, 0, 3, 0, 2.6F, 0, 3, 0, 3.3F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 0.9F, 0, 0, 5, 1.7F, 0, 0, -6, 2.5F, 0, 0, 6, 3.3F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.9F, -6, 0, -6, 1.7F, 4, 0, 8, 2.5F, 4, 0, -8, 3.3F, 0, 0, 0)
                .fanRight("arm", 0.05F, 0, 35, 0, R, 0, 0, 0.6F, -70, 0, 90, 1.5F, -55, 0, 120, 1.7F, -75, 0, 60,
                        2.3F, -70, 0, 95, 2.5F, -70, 0, 90, 3.3F, R, 0, 0)
                .fanLeft("arm", 0.05F, 0, 35, 0, R, 0, 0, 0.6F, -70, 0, 90, 1.5F, -75, 0, 65, 1.7F, -70, 0, 95,
                        2.3F, -55, 0, 120, 2.5F, -75, 0, 60, 3.3F, R, 0, 0)
                .fanBoth("forearm", 0.05F, 0, 0, 0, R, 0, 0, 0.6F, -10, 0, 0, 2.6F, -10, 0, 0, 3.3F, R, 0, 0)
                .rotBoth("hand_top", 0, 0, 0, 0, 0.6F, -20, 0, 0, 1.7F, 35, 0, 0, 2.3F, -20, 0, 0, 2.5F, 35, 0, 0, 3.3F, 0, 0, 0)
                .build());

        // Opens wide, then crosses the upper arms in an X at 1.0 s; the middle pair follows at 1.6 s.
        ACTIONS.put(GoddessAction.CRESCENTS, new Anim(GoddessAction.CRESCENTS)
                .pos("all", 0, 0, 0, 0, 0.8F, 0, 3, 0, 1.03F, 0, -1, 0, 1.4F, 0, 2, 0, 1.63F, 0, -1, 0, 2.2F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 0.8F, -14, 0, 0, 1.03F, 16, 0, 0, 1.4F, -8, 0, 0, 1.63F, 14, 0, 0, 2.2F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.8F, -12, 0, 0, 1.03F, 12, 0, 0, 1.4F, -6, 0, 0, 1.63F, 10, 0, 0, 2.2F, 0, 0, 0)
                .rot("skirt_low", 0, 0, 0, 0, 0.8F, -6, 0, 0, 1.1F, 8, 0, 0, 1.7F, 6, 0, 0, 2.2F, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 0.8F, 28, 0, 125, 1.03F, -78, 0, -22, 1.5F, -50, 0, 30, 2.2F, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 0.8F, -20, 0, 15, 1.03F, 0, 0, 0, 2.2F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.9F, 10, 0, 95, 1.4F, 26, 0, 105, 1.63F, -78, 0, -18, 2.2F, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 1.4F, -20, 0, 10, 1.63F, 0, 0, 0, 2.2F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.8F, 15, 0, 50, 1.06F, -40, 0, 5, 1.4F, 15, 0, 50, 1.66F, -40, 0, 5, 2.2F, R, 0, 0)
                .build());

        // Each pair lifts a lance out of the halo, then casts it forward in turn (from 1.6 s).
        ACTIONS.put(GoddessAction.SOUL_LANCES, new Anim(GoddessAction.SOUL_LANCES)
                .pos("all", 0, 0, 0, 0, 0.8F, 0, 4, 0, 2.3F, 0, 4, 0, 2.9F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 0.8F, -10, 0, 0, 1.5F, -12, 0, 0, 1.75F, 10, 0, 0, 2.4F, 6, 0, 0, 2.9F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.8F, -14, 0, 0, 1.5F, -16, 0, 0, 1.75F, 8, 0, 0, 2.9F, 0, 0, 0)
                .fanBoth("arm", 0.14F, 0, 20, 0, R, 0, 0, 0.55F, 10, 0, 150, 1.4F, 14, 0, 154, 1.62F, -80, 0, 100,
                        2.2F, -70, 0, 95, 2.9F, R, 0, 0)
                .fanBoth("forearm", 0.14F, 0, 0, 0, R, 0, 0, 0.55F, -30, 0, -20, 1.4F, -35, 0, -20, 1.62F, 0, 0, 0,
                        2.9F, R, 0, 0)
                .build());

        // A waltz: two full turns with the gown flaring, scattering orbs from 1.0 s to 3.5 s.
        ACTIONS.put(GoddessAction.DANSE_MACABRE, new Anim(GoddessAction.DANSE_MACABRE)
                .rot("all", 0, 0, 0, 0, 0.6F, 0, -30, 0, 1.4F, 0, 180, 0, 2.2F, 0, 400, 0, 3.0F, 0, 620, 0,
                        3.7F, 0, 735, 0, 4.2F, 0, 720, 0)
                .pos("all", 0, 0, 0, 0, 0.6F, 0, -1, 0, 1.4F, 0, 5, 0, 3.0F, 0, 5, 0, 3.7F, 0, 1, 0, 4.2F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 0.6F, 8, 0, 0, 1.4F, -6, 0, 6, 3.0F, -6, 0, 6, 4.2F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.6F, 10, 0, 0, 1.4F, -14, 0, 10, 3.0F, -14, 0, 10, 4.2F, 0, 0, 0)
                .right("arm_top", 0, R, 0, 0, 0.6F, -40, 0, 40, 1.4F, 0, 0, 165, 3.0F, 0, 0, 160, 3.7F, -20, 0, 120,
                        4.2F, R, 0, 0)
                .left("arm_top", 0, R, 0, 0, 0.6F, -40, 0, 40, 1.4F, -10, 0, 100, 3.0F, -10, 0, 105, 3.7F, -20, 0, 110,
                        4.2F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.6F, -40, 0, 20, 1.4F, 5, 0, 95, 2.2F, 5, 0, 80, 3.0F, 5, 0, 95, 4.2F, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 1.4F, -10, 0, 15, 3.0F, -10, 0, 15, 4.2F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.6F, -30, 0, 5, 1.4F, 10, 0, 70, 3.0F, 10, 0, 65, 4.2F, R, 0, 0)
                .both("forearm_low", 0, R, 0, 0, 1.4F, -5, 0, 10, 3.0F, -5, 0, 10, 4.2F, R, 0, 0)
                .scale("skirt", 0, 1, 1, 1, 0.6F, 0.95F, 1.02F, 0.95F, 1.6F, 1.18F, 0.96F, 1.18F, 3.0F, 1.18F, 0.96F, 1.18F,
                        3.8F, 0.98F, 1.01F, 0.98F, 4.2F, 1, 1, 1)
                .build());

        // Hands gathered in prayer; each pair opens in turn as she rises, and all fall together at 2.5 s.
        ACTIONS.put(GoddessAction.REQUIEM, new Anim(GoddessAction.REQUIEM)
                .pos("all", 0, 0, 0, 0, 0.5F, 0, -2, 0, 2.3F, 0, 11, 0, 2.45F, 0, 12, 0, 2.62F, 0, -2, 0, 3.4F, 0, 0, 0,
                        4.2F, 0, 0, 0)
                .rot("torso", 0, 0, 0, 0, 0.5F, 22, 0, 0, 2.3F, -14, 0, 0, 2.45F, -16, 0, 0, 2.62F, 24, 0, 0, 3.4F, 10, 0, 0,
                        4.2F, 0, 0, 0)
                .rot("head", 0, 0, 0, 0, 0.5F, 30, 0, 0, 2.3F, -32, 0, 0, 2.45F, -34, 0, 0, 2.62F, 30, 0, 0, 3.4F, 12, 0, 0,
                        4.2F, 0, 0, 0)
                .both("arm_top", 0, R, 0, 0, 0.5F, -62, 0, -28, 0.8F, -62, 0, -28, 1.5F, 5, 0, 160, 2.45F, 8, 0, 164,
                        2.62F, -60, 0, 60, 3.4F, -30, 0, 80, 4.2F, R, 0, 0)
                .both("arm_mid", 0, R, 0, 0, 0.5F, -58, 0, -24, 1.1F, -58, 0, -24, 1.8F, 8, 0, 95, 2.45F, 10, 0, 98,
                        2.62F, -50, 0, 40, 3.4F, -25, 0, 60, 4.2F, R, 0, 0)
                .both("arm_low", 0, R, 0, 0, 0.5F, -50, 0, -18, 1.4F, -50, 0, -18, 2.1F, 5, 0, 50, 2.45F, 6, 0, 52,
                        2.62F, -40, 0, 15, 3.4F, -15, 0, 25, 4.2F, R, 0, 0)
                .both("forearm_top", 0, R, 0, 0, 0.5F, -70, 0, -25, 0.8F, -70, 0, -25, 1.5F, 0, 0, 5, 2.45F, 0, 0, 5,
                        2.62F, -15, 0, 0, 4.2F, R, 0, 0)
                .both("forearm_mid", 0, R, 0, 0, 0.5F, -70, 0, -20, 1.1F, -70, 0, -20, 1.8F, 0, 0, 8, 2.45F, 0, 0, 8,
                        2.62F, -15, 0, 0, 4.2F, R, 0, 0)
                .both("forearm_low", 0, R, 0, 0, 0.5F, -60, 0, -15, 1.4F, -60, 0, -15, 2.1F, -5, 0, 5, 2.45F, -5, 0, 5,
                        2.62F, -15, 0, 0, 4.2F, R, 0, 0)
                .rot("veil_front", 0, 0, 0, 0, 2.45F, 0, 0, 0, 2.6F, -40, 0, 0, 3.3F, 0, 0, 0, 4.2F, 0, 0, 0)
                .scale("halo", 0, 1, 1, 1, 0.5F, 0.9F, 0.9F, 1, 2.3F, 1.7F, 1.7F, 1, 2.45F, 1.75F, 1.75F, 1,
                        2.58F, 2.3F, 2.3F, 1, 3.3F, 1, 1, 1, 4.2F, 1, 1, 1)
                .build());
    }

    @Nullable
    public static AnimationDefinition forAction(GoddessAction action) {
        return ACTIONS.get(action);
    }

    enum Target {
        ROTATION, POSITION, SCALE
    }

    private static final class Anim {
        private final AnimationDefinition.Builder builder;

        Anim(float seconds) {
            this.builder = AnimationDefinition.Builder.withLength(seconds);
        }

        Anim(GoddessAction action) {
            this(action.seconds());
        }

        Anim loop() {
            this.builder.looping();
            return this;
        }

        /** Smooth rotation, in degrees added to the rest pose. */
        Anim rot(String part, float... keys) {
            return this.add(part, Target.ROTATION, AnimationChannel.Interpolations.CATMULLROM, false, keys);
        }

        /** Linear rotation: only the death animation still moves like a cut puppet. */
        Anim snap(String part, float... keys) {
            return this.add(part, Target.ROTATION, AnimationChannel.Interpolations.LINEAR, false, keys);
        }

        Anim snap(String part, Target target, float... keys) {
            return this.add(part, target, AnimationChannel.Interpolations.LINEAR, false, keys);
        }

        /** Same relative motion on both sides; values are written for the right side and mirrored for the left. */
        Anim rotBoth(String part, float... keys) {
            this.add(part + "_r", Target.ROTATION, AnimationChannel.Interpolations.CATMULLROM, false, keys);
            return this.add(part + "_l", Target.ROTATION, AnimationChannel.Interpolations.CATMULLROM, true, keys);
        }

        /** Absolute pose of a right arm joint, e.g. {@code "arm_top"}. */
        Anim right(String part, float... keys) {
            return this.pose(part, false, 0.0F, 0.0F, 0.0F, keys);
        }

        /** Absolute pose of a left arm joint, written as if it were the right one. */
        Anim left(String part, float... keys) {
            return this.pose(part, true, 0.0F, 0.0F, 0.0F, keys);
        }

        Anim both(String part, float... keys) {
            return this.right(part, keys).left(part, keys);
        }

        /**
         * One absolute pose for all three right joints of a kind ({@code "arm"} or {@code "forearm"}):
         * each pair starts {@code delay} seconds after the one above it, and the pairs are spread
         * apart by {@code spreadX} and {@code spreadZ} degrees so they open like a fan.
         */
        Anim fanRight(String joint, float delay, float spreadX, float spreadZ, float... keys) {
            return this.fan(joint, false, delay, spreadX, spreadZ, keys);
        }

        Anim fanLeft(String joint, float delay, float spreadX, float spreadZ, float... keys) {
            return this.fan(joint, true, delay, spreadX, spreadZ, keys);
        }

        Anim fanBoth(String joint, float delay, float spreadX, float spreadZ, float... keys) {
            return this.fan(joint, false, delay, spreadX, spreadZ, keys).fan(joint, true, delay, spreadX, spreadZ, keys);
        }

        private Anim fan(String joint, boolean left, float delay, float spreadX, float spreadZ, float... keys) {
            for (int i = 0; i < PAIRS.length; i++) {
                this.pose(joint + "_" + PAIRS[i], left, i * delay, (i - 1) * spreadX, (1 - i) * spreadZ, keys);
            }
            return this;
        }

        private Anim pose(String part, boolean left, float shift, float addX, float addZ, float... keys) {
            float[] rest = GoddessModelLayer.REST_DEGREES.get(part);
            if (rest == null) {
                throw new IllegalArgumentException("No rest pose known for " + part);
            }
            float[] offsets = keys.clone();
            int count = keys.length / 4;
            for (int i = 0; i < count; i++) {
                // The first and last keys stay put so every pair leaves and returns to rest together.
                if (i > 0 && i < count - 1) {
                    offsets[i * 4] += shift;
                }
                boolean toRest = Float.isNaN(keys[i * 4 + 1]);
                offsets[i * 4 + 1] = toRest ? 0.0F : keys[i * 4 + 1] + addX - rest[0];
                offsets[i * 4 + 2] = toRest ? 0.0F : keys[i * 4 + 2] - rest[1];
                offsets[i * 4 + 3] = toRest ? 0.0F : keys[i * 4 + 3] + addZ - rest[2];
            }
            return this.add(part + (left ? "_l" : "_r"), Target.ROTATION, AnimationChannel.Interpolations.CATMULLROM, left,
                    offsets);
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
