package com.arscuriospellbook.server;

import com.arscuriospellbook.ArsCurioSpellbook;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side record of which players currently have Ars Nouveau's spell editor open on a book in
 * one of their ars_spellbook slots (rather than on a held book), and which slot.
 *
 * <p>Ars' editor save packets only say "main hand" or "off hand". While a player is in a session,
 * {@code com.arscuriospellbook.mixin.SpellEditorSavePacketMixin} points those packets at the book in
 * the session's slot instead. The client starts and ends the session as its curio-bound editor
 * screens open and close (see {@code CurioSpellbookEditor}).
 */
@EventBusSubscriber(modid = ArsCurioSpellbook.MODID)
public final class CurioEditSessions {
    /** Slot value for a blocked session: every save is dropped (no ars_spellbook slot is -1). */
    public static final int BLOCKED = -1;

    private static final Map<UUID, Integer> ACTIVE = new ConcurrentHashMap<>();

    private CurioEditSessions() {}

    public static void start(Player player, int bookSlot) {
        ACTIVE.put(player.getUUID(), bookSlot);
    }

    /**
     * Starts a session that drops every save. Used when the client's editor is showing a different
     * slot from the server's selection. Simply not starting a session would be worse: Ars' save
     * packets would then fall through to their normal behavior and save to the book in the player's
     * hand.
     */
    public static void block(Player player) {
        ACTIVE.put(player.getUUID(), BLOCKED);
    }

    public static void end(Player player) {
        ACTIVE.remove(player.getUUID());
    }

    /**
     * The ars_spellbook slot the player is editing, {@link #BLOCKED} if saves must be dropped, or null
     * if no worn-book editor is open.
     */
    public static @Nullable Integer getBookSlot(Player player) {
        return ACTIVE.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }
}
