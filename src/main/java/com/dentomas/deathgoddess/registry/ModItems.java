package com.dentomas.deathgoddess.registry;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.item.MourningEffigyItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DeathGoddessMod.MODID);

    public static final RegistryObject<Item> GODDESS_SPAWN_EGG = ITEMS.register("goddess_of_death_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GODDESS, 0x3B0713, 0x080608,
                    new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<Item> MOURNING_EFFIGY = ITEMS.register("mourning_effigy",
            () -> new MourningEffigyItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)
                    .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> EYE_OF_OBLIVION = ITEMS.register("eye_of_oblivion",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC).rarity(Rarity.EPIC).fireResistant()));

    private ModItems() {
    }
}
