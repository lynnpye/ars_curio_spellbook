package com.arscuriospellbook.server;

import com.arscuriospellbook.ArsCurioSpellbook;
import com.arscuriospellbook.CurioSpellbookUtil;
import com.arscuriospellbook.network.SyncSelectedBookPayload;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.theillusivec4.curios.api.event.SlotModifiersUpdatedEvent;

import java.util.List;

/**
 * The server-side record of which ars_spellbook slot each player has selected. The cast, spell-menu
 * and edit keys act on that slot.
 *
 * <p>The selection is an absolute slot index, stored as a player data attachment. It's saved with the
 * player and kept on death, so it survives relogs and respawns. There's no fallback: if the selected
 * slot is empty, the keys do nothing and say so, rather than using some other book. The client keeps
 * a copy, synced by {@link SyncSelectedBookPayload}, for its menus and messages.
 *
 * <p>If the selected slot stops existing (Curios slot count reduced), the selection is reset to
 * slot 1 and the player is told. The count can change:
 * <ul>
 *   <li>during play: Curios fires {@link SlotModifiersUpdatedEvent} after resizing;</li>
 *   <li>between sessions: a pack or datapack can reduce the slot count while the player is offline,
 *       so it's checked at login (which also sends the selection to the client);</li>
 *   <li>on {@code /reload}: Curios rebuilds inventories in its own {@link OnDatapackSyncEvent}
 *       handler without firing the event above, so this listener runs at low priority, after it.</li>
 * </ul>
 *
 * <p><b>A slot count of 0 never causes a reset.</b> Curios also fires
 * {@link SlotModifiersUpdatedEvent} in the middle of rebuilding a player's inventory from saved data
 * (at login and on reload), when re-applying a saved slot modifier (such as one added with
 * {@code /curios}) grows the slot type. At that instant its map of slot types has been cleared and
 * not yet refilled, so the count reads 0. Resetting then would discard a perfectly valid selection;
 * an earlier version did exactly that. A genuine count of 0 means the pack has no spellbook slots
 * at all, which isn't a reason to discard the stored choice either. Every request is also checked
 * against the current slot count when it arrives.
 */
@EventBusSubscriber(modid = ArsCurioSpellbook.MODID)
public final class SpellbookSelection {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ArsCurioSpellbook.MODID);

    /** The selected ars_spellbook slot index. 0 (slot 1) until the player picks another. */
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SELECTED_BOOK_SLOT =
            ATTACHMENT_TYPES.register("selected_book_slot",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    private SpellbookSelection() {}

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    public static int get(ServerPlayer player) {
        return player.getData(SELECTED_BOOK_SLOT);
    }

    /**
     * K / Ctrl+K: step the selection to the next ({@code step} > 0) or previous slot, through every
     * slot including empty ones, wrapping around. Then report the newly selected slot.
     */
    public static void cycle(ServerPlayer player, int step) {
        int slotCount = CurioSpellbookUtil.getSpellbookSlotCount(player);
        if (slotCount > 0) {
            int next = Math.floorMod(get(player) + Integer.signum(step), slotCount);
            set(player, next);
        }
        player.displayClientMessage(CurioSpellbookUtil.describeSlot(player, get(player)), true);
    }

    /**
     * Next / previous spell key: step the spell of the book in the selected slot forward
     * ({@code step} > 0) or back, wrapping around through every spell slot, the same way Ars
     * Nouveau's own keys do for a held book. Then report the result. Curios syncs the changed book
     * back to the client. If the selected slot is empty, just say so.
     */
    public static void cycleSpell(ServerPlayer player, int step) {
        int bookSlot = get(player);
        ItemStack book = CurioSpellbookUtil.getEquippedSpellbook(player, bookSlot);
        AbstractCaster<?> caster = SpellCasterRegistry.from(book);
        if (!book.isEmpty() && caster != null) {
            AbstractCaster<?> stepped = step > 0 ? caster.setNextSlot() : caster.setPreviousSlot();
            stepped.saveToStack(book);
        }
        player.displayClientMessage(CurioSpellbookUtil.describeSlot(player, bookSlot), true);
    }

    /**
     * If the selected slot no longer exists, resets the selection to slot 1 and tells the player.
     * Always re-sends the selection to the client.
     */
    /**
     * If the selected slot no longer exists, resets the selection to slot 1 and tells the player.
     * Always re-sends the selection to the client. A slot count of 0 never causes a reset (see the
     * class comment).
     */
    private static void check(ServerPlayer player) {
        int selected = get(player);
        int slotCount = CurioSpellbookUtil.getSpellbookSlotCount(player);
        if (slotCount > 0 && selected >= slotCount) {
            set(player, 0);
            player.displayClientMessage(Component.translatable(
                    "message." + ArsCurioSpellbook.MODID + ".spellbook_slot_removed", selected + 1), true);
        } else {
            sync(player);
        }
    }

    private static void set(ServerPlayer player, int bookSlot) {
        player.setData(SELECTED_BOOK_SLOT, bookSlot);
        sync(player);
    }

    /** Re-sends the selection to the client, e.g. after refusing a request based on a stale copy. */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncSelectedBookPayload(get(player)));
    }

    @SubscribeEvent
    public static void onSlotsChanged(SlotModifiersUpdatedEvent event) {
        // Fired on both sides; only the server acts. The client hears about it through the sync.
        if (event.getEntity() instanceof ServerPlayer player && event.getTypes().contains(ArsCurioSpellbook.SLOT_ID)) {
            check(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            check(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // A null player means /reload, for everyone online; otherwise one player is joining.
        List<ServerPlayer> players = event.getPlayer() != null
                ? List.of(event.getPlayer())
                : event.getPlayerList().getPlayers();
        players.forEach(SpellbookSelection::check);
    }
}
