package com.dentomas.deathgoddess.entity;

import java.util.Locale;

/**
 * Everything the Goddess can be doing. The duration is in ticks and is also the length of the
 * matching keyframe animation, which is built from it in GoddessAnimations.
 */
public enum GoddessAction {
    NONE(0),
    SPAWN(70),
    PHASE(70),
    SWEEP(34),
    EYES(40),
    GAZE(60),
    STRINGS(38),
    VEIL_STEP(30),
    GRAVE_BLOOM(46),
    SEVERANCE(50),
    DEATH_KNELL(70),
    BLACK_RAIN(72),
    CROSS(52),
    MARK(56),
    LATTICE(66),
    CRESCENTS(44),
    SOUL_LANCES(58),
    DANSE_MACABRE(84),
    REQUIEM(84);

    public static final GoddessAction[] VALUES = values();

    public final int duration;

    GoddessAction(int duration) {
        this.duration = duration;
    }

    public float seconds() {
        return this.duration / 20.0F;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static GoddessAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }

    public static GoddessAction byName(String name) {
        for (GoddessAction action : VALUES) {
            if (action.id().equals(name.toLowerCase(Locale.ROOT))) {
                return action;
            }
        }
        return NONE;
    }
}
