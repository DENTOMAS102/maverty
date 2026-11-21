package com.dentomas.deathgoddess;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * config/deathgoddess-common.toml: the boss's strength, the watching eye and the scattered tomes.
 * Every comment is written in English and Russian, since the file is what pack authors read.
 */
public final class GoddessConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue ARMOR;
    public static final ForgeConfigSpec.DoubleValue ARMOR_TOUGHNESS;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_CAP;

    public static final ForgeConfigSpec.BooleanValue WATCH_ENABLED;
    public static final ForgeConfigSpec.IntValue LORDS_REQUIRED;
    public static final ForgeConfigSpec.IntValue WATCH_MIN_MINUTES;
    public static final ForgeConfigSpec.IntValue WATCH_MAX_MINUTES;
    public static final ForgeConfigSpec.IntValue CALM_SECONDS;

    public static final ForgeConfigSpec.DoubleValue TOME_CHANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("The Goddess of Death. For reference, the vanilla Ender Dragon has 200 health and no armour.",
                        "Богиня Смерти. Для сравнения: у обычного Дракона Края 200 единиц здоровья и нет брони.")
                .push("goddess");
        MAX_HEALTH = builder.comment("Maximum health.", "Максимальное здоровье.")
                .defineInRange("maxHealth", 600.0D, 1.0D, 1024.0D);
        ARMOR = builder.comment("Armour points.", "Очки брони.").defineInRange("armor", 12.0D, 0.0D, 30.0D);
        ARMOR_TOUGHNESS = builder.comment("Armour toughness.", "Твёрдость брони.")
                .defineInRange("armorToughness", 6.0D, 0.0D, 20.0D);
        DAMAGE_MULTIPLIER = builder.comment("Multiplies the damage of every one of her attacks.",
                        "Множитель урона всех её атак.")
                .defineInRange("damageMultiplier", 1.5D, 0.1D, 20.0D);
        DAMAGE_CAP = builder.comment("The most damage a single hit can do to her, however strong the weapon. 0 disables the cap.",
                        "Наибольший урон, который она получает от одного удара, каким бы сильным ни было оружие.",
                        "0 отключает ограничение.")
                .defineInRange("damageCap", 22.0D, 0.0D, 10000.0D);
        builder.pop();

        builder.comment("The eye that opens above a player once enough Lords have been killed.",
                        "Which mobs count as Lords is set by the entity type tag deathgoddess:lords.",
                        "Око, которое открывается над игроком, когда убито достаточно Лордов.",
                        "Какие мобы считаются Лордами, задаёт тег типов сущностей deathgoddess:lords.")
                .push("watchingEye");
        WATCH_ENABLED = builder.comment("Set to false to switch the eye off entirely.",
                        "Значение false полностью отключает око.")
                .define("enabled", true);
        LORDS_REQUIRED = builder.comment("How many different Lords must have been killed in this world before the eye appears.",
                        "Сколько разных Лордов должно быть убито в этом мире, прежде чем появится око.")
                .defineInRange("lordsRequired", 3, 0, 100);
        WATCH_MIN_MINUTES = builder.comment("Shortest time between two appearances, in minutes.",
                        "Наименьшее время между двумя появлениями, в минутах.")
                .defineInRange("minMinutes", 25, 1, 10000);
        WATCH_MAX_MINUTES = builder.comment("Longest time between two appearances, in minutes.",
                        "Наибольшее время между двумя появлениями, в минутах.")
                .defineInRange("maxMinutes", 70, 1, 10000);
        CALM_SECONDS = builder.comment("The eye only opens above a player who has neither dealt nor taken damage for this many seconds.",
                        "Око открывается только над игроком, который столько секунд не наносил и не получал урона.")
                .defineInRange("calmSeconds", 45, 0, 3600);
        builder.pop();

        builder.comment("The tomes about her.", "Тома о ней.").push("tomes");
        TOME_CHANCE = builder.comment("Chance for any generated loot chest in the world to also hold one of her tomes.",
                        "Шанс того, что в любом сгенерированном сундуке с добычей окажется один из её томов.")
                .defineInRange("chanceInChests", 0.05D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private GoddessConfig() {
    }
}
