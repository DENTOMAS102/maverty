package com.dentomas.deathgoddess.client;

import com.dentomas.deathgoddess.item.TomeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

/** Opens a tome in the vanilla book screen, reading its pages from the language file. */
public final class TomeReader {
    public static void open(String tome) {
        String prefix = TomeItem.key(tome) + ".page.";
        int count = 0;
        while (I18n.exists(prefix + (count + 1))) {
            count++;
        }
        int pages = count;
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() {
                return pages;
            }

            @Override
            public FormattedText getPageRaw(int page) {
                return Component.translatable(prefix + (page + 1));
            }
        }));
    }

    private TomeReader() {
    }
}
