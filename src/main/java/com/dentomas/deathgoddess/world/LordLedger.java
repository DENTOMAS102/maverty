package com.dentomas.deathgoddess.world;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Remembers, per world, which Lords have been killed. Kept with the overworld's saved data. */
public class LordLedger extends SavedData {
    private static final String NAME = "deathgoddess_lords";
    private final Set<String> slain = new LinkedHashSet<>();

    public static LordLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(LordLedger::load, LordLedger::new, NAME);
    }

    private static LordLedger load(CompoundTag tag) {
        LordLedger ledger = new LordLedger();
        ListTag list = tag.getList("Slain", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ledger.slain.add(list.getString(i));
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String id : this.slain) {
            list.add(StringTag.valueOf(id));
        }
        tag.put("Slain", list);
        return tag;
    }

    /** @return true if this kind of Lord had not been killed before */
    public boolean record(String id) {
        boolean added = this.slain.add(id);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public void clear() {
        this.slain.clear();
        this.setDirty();
    }

    public int count() {
        return this.slain.size();
    }

    public Set<String> slain() {
        return this.slain;
    }
}
