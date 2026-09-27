package com.arscuriospellbook.client;

import com.arscuriospellbook.CurioSpellbookUtil;
import com.arscuriospellbook.network.SetCurioEditSessionPayload;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.book.ParticleOverviewScreen;
import com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Opens Ars Nouveau's spell editor on the book in the selected ars_spellbook slot.
 *
 * <p>Ars' editor screens assume the book is in a hand. Two mixins redirect them:
 * <ul>
 *   <li>{@code SpellSlottedScreenMixin} asks {@link #beginScreenConstruction} which stack a new
 *       editor screen should use. A screen is "curio-bound" if it's opened by the edit key, or
 *       opened while a curio-bound screen is showing (for example the particle editor opened from
 *       the spell editor). Curio-bound screens are remembered in {@link #BOUND}.</li>
 *   <li>{@code SpellEditorSavePacketMixin} makes the server save to the worn book while the player's
 *       edit session is active. {@link #tick} keeps that session in step with whether a curio-bound
 *       screen is showing.</li>
 * </ul>
 */
public final class CurioSpellbookEditor {
    /** Editor screens showing the worn book. Weak, so closed screens can be garbage collected. */
    private static final Set<Screen> BOUND = Collections.newSetFromMap(new WeakHashMap<>());

    private static boolean openingFromKey = false;
    private static boolean constructingBoundScreen = false;
    private static boolean sessionActive = false;
    /** The ars_spellbook slot of the book being edited. */
    private static int editingBookSlot = -1;

    private CurioSpellbookEditor() {}

    /**
     * Edit key pressed in-world: open the spell editor on the book in the selected ars_spellbook
     * slot, or say why there isn't one.
     */
    static void open(Minecraft mc, LocalPlayer player) {
        if (!(SelectedSpellbook.bookOrReport(player).getItem() instanceof SpellBook)) {
            return;
        }
        editingBookSlot = SelectedSpellbook.get();
        // Don't let the particle editor reuse a cached screen built for a held book.
        clearParticleEditorCache();
        // Start the session before the editor exists, so no save can reach the server first.
        setSession(true);
        openingFromKey = true;
        try {
            // The hand is only a label here. The constructor mixin supplies the worn book, and the
            // server session makes the "main hand" saves go to the worn book.
            mc.setScreen(new GuiSpellBook(InteractionHand.MAIN_HAND));
        } finally {
            openingFromKey = false;
        }
    }

    /** Called every client tick. Keeps the server session matching what's on screen. */
    static void tick(Minecraft mc) {
        if (mc.player == null) {
            // Disconnected: the server drops the session on logout.
            sessionActive = false;
            editingBookSlot = -1;
            BOUND.clear();
            return;
        }
        boolean showingBoundEditor = mc.screen != null && BOUND.contains(mc.screen);
        if (showingBoundEditor != sessionActive) {
            setSession(showingBoundEditor);
            if (!showingBoundEditor && BOUND.contains(ParticleOverviewScreen.lastScreen)) {
                // Don't let a later held-book edit reuse a particle screen built for the worn book.
                clearParticleEditorCache();
            }
        }
    }

    /** Whether a worn-book editor screen is showing (closed by the client if the selection changes). */
    static boolean isShowingBoundEditor(Minecraft mc) {
        return mc.screen != null && BOUND.contains(mc.screen);
    }

    /** From the constructor mixin: which stack a new editor screen should edit. */
    public static ItemStack beginScreenConstruction(ItemStack heldStack) {
        Minecraft mc = Minecraft.getInstance();
        constructingBoundScreen = openingFromKey || (mc.screen != null && BOUND.contains(mc.screen));
        if (!constructingBoundScreen) {
            return heldStack;
        }
        ItemStack worn = CurioSpellbookUtil.getEquippedSpellbook(mc.player, editingBookSlot);
        return worn.isEmpty() ? heldStack : worn;
    }

    /** From the constructor mixin, once the screen is built. */
    public static void endScreenConstruction(SpellSlottedScreen screen) {
        if (constructingBoundScreen) {
            BOUND.add(screen);
        }
        constructingBoundScreen = false;
    }

    private static void setSession(boolean active) {
        sessionActive = active;
        PacketDistributor.sendToServer(new SetCurioEditSessionPayload(active, editingBookSlot));
    }

    private static void clearParticleEditorCache() {
        ParticleOverviewScreen.lastScreen = null;
        ParticleOverviewScreen.LAST_SELECTED_PART = null;
    }
}
