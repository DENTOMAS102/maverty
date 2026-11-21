package com.dentomas.deathgoddess.loot;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.GoddessConfig;
import com.dentomas.deathgoddess.item.TomeItem;
import com.dentomas.deathgoddess.registry.ModItems;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Slips one of her tomes into generated chests anywhere in the world, vanilla or modded. */
public class TomeLootModifier extends LootModifier {
    public static final Supplier<Codec<TomeLootModifier>> CODEC = Suppliers.memoize(
            () -> RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, TomeLootModifier::new)));

    public TomeLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        // Her own ruins already place the right volume themselves.
        boolean chest = table != null && table.getPath().contains("chest") && !table.getNamespace().equals(DeathGoddessMod.MODID);
        if (chest && context.getRandom().nextDouble() < GoddessConfig.TOME_CHANCE.get()) {
            loot.add(TomeItem.random(ModItems.TOME.get(), context.getRandom()));
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
