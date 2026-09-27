package com.arscuriospellbook.client;

import com.arscuriospellbook.CurioSpellbookUtil;
import com.arscuriospellbook.network.CycleSelectedBookPayload;
import com.arscuriospellbook.network.CycleSelectedSpellPayload;
import com.arscuriospellbook.network.SetCurioBookSlotPayload;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The client's copy of the selected ars_spellbook slot, plus selecting spells on that book.
 *
 * <p>The server owns the selection (see {@code SpellbookSelection}) and sends it at login and
 * whenever it changes. This copy is used to open the right book's menu or editor, and to say
 * straight away when the selected slot is empty. The selection is absolute: an empty slot stays
 * selected, and the keys report it rather than falling back to another book.
 *
 * <p>Changes of book or spell are reported with one line on the action bar (the brief text above the
 * hotbar). Ars Nouveau's HUD only follows the book in the selected hotbar slot, so worn books aren't
 * shown there.
 */
public final class SelectedSpellbook {
    private static int selectedBookSlot = 0;

    private SelectedSpellbook() {}

    /** The selected ars_spellbook slot index, as last sent by the server. */
    static int get() {
        return selectedBookSlot;
    }

    /**
     * The spellbook in the selected slot, or {@link ItemStack#EMPTY}. If it's empty, the player is
     * told why (empty slot, slot doesn't exist, or no spellbook slots at all).
     */
    static ItemStack bookOrReport(LocalPlayer player) {
        ItemStack book = CurioSpellbookUtil.getEquippedSpellbook(player, selectedBookSlot);
        if (book.isEmpty()) {
            showStatus(player);
        }
        return book;
    }

    /** K / Ctrl+K: ask the server to step the selection. It replies with the result and a message. */
    static void cycleBook(int step) {
        PacketDistributor.sendToServer(new CycleSelectedBookPayload(step));
    }

    /**
     * Next / previous spell key: ask the server to step the selected book's spell. The server owns
     * the book's data, so it does the stepping and replies with the status line; Curios syncs the
     * changed book back. Stepping on a client copy could lose presses when the server's sync arrives
     * between two quick presses.
     */
    static void cycleSpell(int step) {
        PacketDistributor.sendToServer(new CycleSelectedSpellPayload(step));
    }

    /**
     * Spell menu pick: selects {@code spellSlot} on the selected book. The client copy is updated
     * immediately so the book looks right straight away; the server's copy is authoritative and syncs
     * back through Curios. The status line comes from the server, so it reports what was applied.
     */
    static void selectSpell(LocalPlayer player, ItemStack book, int spellSlot) {
        AbstractCaster<?> caster = SpellCasterRegistry.from(book);
        if (caster == null) {
            return;
        }
        caster.setCurrentSlot(spellSlot).saveToStack(book);
        PacketDistributor.sendToServer(new SetCurioBookSlotPayload(spellSlot));
    }

    /** Shows the selected slot's book and spell (or why there isn't one) on the action bar. */
    static void showStatus(LocalPlayer player) {
        player.displayClientMessage(CurioSpellbookUtil.describeSlot(player, selectedBookSlot), true);
    }

    /**
     * The server sent the selection. If it changed while our spell menu or a worn-book editor was
     * open, that screen belongs to the previous book, so it's closed. That also tells the player
     * something happened; the server sends the message saying what.
     */
    public static void onServerSync(int bookSlot) {
        boolean changed = bookSlot != selectedBookSlot;
        selectedBookSlot = bookSlot;
        Minecraft mc = Minecraft.getInstance();
        if (changed && (mc.screen instanceof CurioSpellbookRadialScreen || CurioSpellbookEditor.isShowingBoundEditor(mc))) {
            mc.setScreen(null);
        }
    }

    /** Forgets the selection on disconnect; the server re-sends it at the next login. */
    static void reset() {
        selectedBookSlot = 0;
    }
}
