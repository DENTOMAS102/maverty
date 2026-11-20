package com.dentomas.deathgoddess.client.model;

import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Keyframed actions come from {@link GoddessAnimations}; everything that must never repeat exactly
 * (the eyes, the cloth, the twitching fingers, the hover) is layered on top procedurally.
 */
public class GoddessModel extends HierarchicalModel<GoddessOfDeath> {
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final String[] PAIRS = {"top", "mid", "low"};
    private static final String[] SIDES = {"r", "l"};
    private static final String[] TRAINS = {"train_r", "train_l", "train_back", "train_front_r", "train_front_l",
            "train_back_r", "train_back_l"};

    private final ModelPart root;
    private final ModelPart all;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart skirt;
    private final ModelPart skirtMid;
    private final ModelPart skirtLow;
    private final ModelPart veilFront;
    private final ModelPart veilFront2;
    private final ModelPart veilBack;
    private final ModelPart veilSideR;
    private final ModelPart veilSideL;
    private final ModelPart crownRing;
    private final ModelPart halo;
    private final ModelPart[] eyes = new ModelPart[GoddessModelLayer.EYE_COUNT];
    private final ModelPart[] pupils = new ModelPart[GoddessModelLayer.EYE_COUNT];
    private final List<ModelPart> panels = new ArrayList<>();
    private final List<ModelPart> trains = new ArrayList<>();
    private final List<ModelPart> arms = new ArrayList<>();
    private final List<ModelPart> fingers = new ArrayList<>();
    private final List<ModelPart> fingertips = new ArrayList<>();
    /** Root-to-hand chains, used to hang the marionette strings from each wrist. */
    private final ModelPart[][] handChains = new ModelPart[6][];
    private float partialTick;

    public GoddessModel(ModelPart root) {
        this.root = root;
        this.all = root.getChild("all");
        this.torso = this.all.getChild("torso");
        ModelPart chest = this.torso.getChild("chest");
        this.head = chest.getChild("head");
        this.skirt = this.all.getChild("skirt");
        this.skirtMid = this.skirt.getChild("skirt_mid");
        this.skirtLow = this.skirtMid.getChild("skirt_low");
        this.veilFront = this.head.getChild("veil_front");
        this.veilFront2 = this.head.getChild("veil_front2");
        this.veilBack = this.head.getChild("veil_back");
        this.veilSideR = this.head.getChild("veil_side_r");
        this.veilSideL = this.head.getChild("veil_side_l");
        this.crownRing = this.head.getChild("crown").getChild("crown_ring");
        this.halo = this.all.getChild("halo");
        for (int i = 0; i < this.eyes.length; i++) {
            this.eyes[i] = this.halo.getChild("eye_" + i);
            this.pupils[i] = this.eyes[i].getChild("pupil_" + i);
        }
        for (int i = 0; this.skirt.hasChild("panel_" + i); i++) {
            this.panels.add(this.skirt.getChild("panel_" + i));
        }
        for (String name : TRAINS) {
            this.trains.add(this.skirtLow.getChild(name));
        }
        int chain = 0;
        for (String pair : PAIRS) {
            for (String side : SIDES) {
                String key = pair + "_" + side;
                ModelPart arm = chest.getChild("arm_" + key);
                ModelPart forearm = arm.getChild("forearm_" + key);
                ModelPart hand = forearm.getChild("hand_" + key);
                this.arms.add(arm);
                this.handChains[chain++] = new ModelPart[]{this.all, this.torso, chest, arm, forearm, hand};
                for (int f = 0; f < 4; f++) {
                    ModelPart finger = hand.getChild("finger_" + key + "_" + f);
                    this.fingers.add(finger);
                    this.fingertips.add(finger.getChild("fingertip_" + key + "_" + f));
                }
                this.fingers.add(hand.getChild("thumb_" + key));
                this.fingertips.add(hand.getChild("thumb_" + key).getChild("thumbtip_" + key));
            }
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    public ModelPart[][] handChains() {
        return this.handChains;
    }

    @Override
    public void prepareMobModel(GoddessOfDeath entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(GoddessOfDeath entity, float limbSwing, float limbSwingAmount, float age, float headYaw,
                          float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        float dying = entity.dyingTicks > 0
                ? Mth.clamp((entity.dyingTicks + this.partialTick) / GoddessOfDeath.DEATH_TICKS, 0.0F, 1.0F) : 0.0F;
        // Fades all living motion out during the first fifth of the death animation.
        float life = 1.0F - Mth.clamp(dying * 5.0F, 0.0F, 1.0F);
        int action = entity.getAction();
        boolean gazing = action == GoddessOfDeath.GAZE;
        boolean enraged = entity.isEnraged();

        this.animate(entity.idleAnim, GoddessAnimations.IDLE, age);
        this.animate(entity.spawnAnim, GoddessAnimations.SPAWN, age);
        this.animate(entity.sweepAnim, GoddessAnimations.SWEEP, age);
        this.animate(entity.eyesAnim, GoddessAnimations.EYES, age);
        this.animate(entity.gazeAnim, GoddessAnimations.GAZE, age);
        this.animate(entity.stringsAnim, GoddessAnimations.STRINGS, age);
        this.animate(entity.veilStepAnim, GoddessAnimations.VEIL_STEP, age);
        this.animate(entity.phaseAnim, GoddessAnimations.PHASE, age);
        this.animate(entity.deathAnim, GoddessAnimations.DEATH, age);

        this.head.yRot += headYaw * DEG * life;
        this.head.xRot += headPitch * DEG * 0.7F * life;

        // She hovers; when she drifts forward everything loose trails behind her.
        float move = Math.min(limbSwingAmount, 1.0F) * life;
        this.all.y += (Mth.sin(age * 0.055F) * 0.7F - 0.6F) * life;
        this.torso.xRot += move * 0.16F;
        this.skirt.xRot += move * 0.08F;
        this.skirtMid.xRot += move * 0.12F + Mth.sin(age * 0.05F) * 0.012F;
        this.skirtLow.xRot += move * 0.14F + Mth.sin(age * 0.05F - 0.8F) * 0.016F;
        for (ModelPart arm : this.arms) {
            arm.xRot += move * 0.22F;
        }
        for (int i = 0; i < this.panels.size(); i++) {
            ModelPart panel = this.panels.get(i);
            panel.xRot += Mth.sin(age * 0.045F + i * 1.3F) * 0.035F - move * 0.07F;
            panel.zRot += Mth.sin(age * 0.031F + i * 2.1F) * 0.02F;
        }
        for (int i = 0; i < this.trains.size(); i++) {
            ModelPart train = this.trains.get(i);
            train.xRot += Mth.sin(age * 0.04F + i * 1.9F) * 0.05F;
            train.zRot += Mth.cos(age * 0.036F + i * 1.1F) * 0.04F;
        }
        float veil = Mth.sin(age * 0.06F);
        this.veilFront.xRot += veil * 0.03F + move * 0.03F;
        this.veilFront.zRot += Mth.sin(age * 0.043F) * 0.02F;
        this.veilFront2.xRot += Mth.sin(age * 0.06F - 0.7F) * 0.045F;
        this.veilBack.xRot += veil * 0.03F + move * 0.3F;
        this.veilSideR.zRot += Mth.sin(age * 0.05F) * 0.03F + move * 0.05F;
        this.veilSideL.zRot -= Mth.sin(age * 0.05F + 0.6F) * 0.03F + move * 0.05F;
        this.crownRing.zRot += Mth.sin(age * 0.02F) * 0.05F;

        this.animateFingers(age, life);
        this.animateEyes(age, headYaw, headPitch, dying, gazing, enraged);
    }

    private void animateFingers(float age, float life) {
        for (int i = 0; i < this.fingers.size(); i++) {
            // A slow curl plus the odd sudden jerk, as if something tugged the string.
            float jerk = hash(Mth.floor(age / 6.0F), i) > 0.9F ? 0.3F : 0.0F;
            float curl = (Mth.sin(age * 0.08F + i * 0.9F) * 0.08F - jerk) * life;
            this.fingers.get(i).xRot += curl;
            this.fingertips.get(i).xRot += curl * 1.4F - Mth.sin(age * 0.08F + i * 0.9F - 0.6F) * 0.05F * life;
        }
    }

    private void animateEyes(float age, float headYaw, float headPitch, float dying, boolean gazing, boolean enraged) {
        float speed = enraged ? 1.8F : 1.0F;
        float lookX = Mth.clamp(headYaw / 50.0F, -1.0F, 1.0F);
        float lookY = Mth.clamp(headPitch / 40.0F, -1.0F, 1.0F);
        int count = this.eyes.length;
        for (int i = 0; i < count; i++) {
            ModelPart eye = this.eyes[i];
            ModelPart pupil = this.pupils[i];
            float phase = i * 2.399F;
            eye.x += Mth.cos(age * 0.043F * speed + phase) * 0.45F;
            eye.y += Mth.sin(age * 0.061F * speed + phase * 1.7F) * 0.6F;
            eye.zRot += Mth.sin(age * 0.03F + phase) * 0.12F;

            float period = 70.0F + (i * 37 % 90);
            float blink = (age + i * 53.0F) % period;
            float open = blink < 6.0F ? Math.abs(blink - 3.0F) / 3.0F : 1.0F;
            if (gazing) {
                open = 1.25F;
            }
            // In death the eyes shut one after another.
            open *= 1.0F - Mth.clamp((dying * 1.7F - (float) i / count) * 6.0F, 0.0F, 1.0F);
            eye.yScale *= Math.max(0.04F, open);

            // Saccades: hold a direction, then snap to the next one.
            float segment = age * speed / (22.0F + (i % 5) * 9.0F) + i * 0.37F;
            int step = Mth.floor(segment);
            float snap = Mth.clamp((segment - step) * 6.0F, 0.0F, 1.0F);
            float wanderX = Mth.lerp(snap, hash(step - 1, i), hash(step, i)) * 2.0F - 1.0F;
            float wanderY = Mth.lerp(snap, hash(step - 1, i + 97), hash(step, i + 97)) * 2.0F - 1.0F;
            float rangeX = GoddessModelLayer.EYE_WIDTH[i] * 0.24F;
            float rangeY = GoddessModelLayer.EYE_HEIGHT[i] * 0.16F;
            if (gazing) {
                pupil.xScale *= 1.5F;
                pupil.yScale *= 1.5F;
            } else {
                pupil.x += (wanderX * 0.7F + lookX * 0.3F) * rangeX;
                pupil.y += (wanderY * 0.7F + lookY * 0.3F) * rangeY;
                if (enraged) {
                    pupil.xScale *= 1.25F;
                    pupil.yScale *= 1.25F;
                }
            }
        }
    }

    private static float hash(int a, int b) {
        return Mth.frac(Mth.sin(a * 127.1F + b * 311.7F) * 43758.547F);
    }
}
