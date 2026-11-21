package com.dentomas.deathgoddess.item;

import com.dentomas.deathgoddess.client.TomeReader;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * A readable book about the Goddess. One item, many volumes: the "Tome" tag says which. Titles,
 * authors and pages live in the language files, so every volume reads in the player's language.
 */
public class TomeItem extends Item {
    public static final String TAG = "Tome";
    /** Must match the ids in tools/tomes.json (tools/gen_resources.py checks this). */
    public static final List<String> TOMES = List.of("prayer_of_the_veil", "litany_of_six_hands", "prayer_against_being_seen",
            "on_the_death_of_a_god", "of_false_gods", "the_last_sermon", "the_stonecutters_account", "the_emptied_village",
            "she_counts_the_lords", "puppet_and_puppeteer");

    public TomeItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, String tome) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString(TAG, tome);
        return stack;
    }

    public static ItemStack random(Item item, RandomSource random) {
        return create(item, TOMES.get(random.nextInt(TOMES.size())));
    }

    public static String tomeOf(ItemStack stack) {
        String tome = stack.hasTag() ? stack.getTag().getString(TAG) : "";
        return TOMES.contains(tome) ? tome : TOMES.get(0);
    }

    public static String key(String tome) {
        return "tome.deathgoddess." + tome;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(key(tomeOf(stack)) + ".title");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("book.byAuthor", Component.translatable(key(tomeOf(stack)) + ".author"))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            String tome = tomeOf(stack);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TomeReader.open(tome));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items) {
        if (this.allowedIn(tab)) {
            for (String tome : TOMES) {
                items.add(create(this, tome));
            }
        }
    }
}
