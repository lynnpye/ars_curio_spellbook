package com.arscuriospellbook.client;

import com.arscuriospellbook.ArsCurioSpellbook;
import com.arscuriospellbook.network.CastCurioBookPayload;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.SecondaryIconPosition;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.client.registry.ModKeyBindings;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ArsCurioSpellbook.MODID, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static final PressTracker CAST = new PressTracker(ModKeyMappings.CAST_SPELL);
    private static final PressTracker NEXT_BOOK = new PressTracker(ModKeyMappings.NEXT_SPELLBOOK);
    private static final PressTracker PREVIOUS_BOOK = new PressTracker(ModKeyMappings.PREVIOUS_SPELLBOOK);
    private static final PressTracker NEXT_SPELL = new PressTracker(ModKeyMappings.NEXT_SPELL);
    private static final PressTracker PREVIOUS_SPELL = new PressTracker(ModKeyMappings.PREVIOUS_SPELL);

    private ClientInputHandler() {}

    /*
     * Key bindings are read once per client tick through consumeClick()/isDown(), as NeoForge
     * recommends; InputEvent is not used. Vanilla only "clicks" a binding while no screen is open,
     * so these checks only react to in-world presses. Input while our radial menu is open is
     * handled by CurioSpellbookRadialScreen itself.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        CurioSpellbookEditor.tick(mc);

        // The radial and editor keys open a screen, which stops further clicks, so any click
        // counts. The others act in-world and use PressTracker to ignore OS auto-repeat.
        boolean radialPressed = drainClicks(ModKeyMappings.OPEN_SPELL_RADIAL);
        boolean editPressed = drainClicks(ModKeyMappings.EDIT_SPELLBOOK);
        boolean castPressed = CAST.pollNewPress();
        boolean nextPressed = NEXT_BOOK.pollNewPress();
        boolean previousPressed = PREVIOUS_BOOK.pollNewPress();
        boolean nextSpellPressed = NEXT_SPELL.pollNewPress();
        boolean previousSpellPressed = PREVIOUS_SPELL.pollNewPress();

        if (player == null) {
            SelectedSpellbook.reset();
            return;
        }
        if (mc.screen != null) {
            return;
        }
        // Never do two things on the same press. With the defaults this can't happen, but players
        // may rebind two actions to one key. NeoForge only clicks bindings whose modifier matches,
        // so a <key> / Shift+<key> pair stays separate; if two are bound to the exact same key and
        // modifier, both get clicked and the first match here wins.
        if (radialPressed) {
            openRadial(mc, player);
        } else if (editPressed) {
            CurioSpellbookEditor.open(mc, player);
        } else if (nextPressed) {
            SelectedSpellbook.cycleBook(1);
        } else if (previousPressed) {
            SelectedSpellbook.cycleBook(-1);
        } else if (nextSpellPressed) {
            SelectedSpellbook.cycleSpell(1);
        } else if (previousSpellPressed) {
            SelectedSpellbook.cycleSpell(-1);
        } else if (castPressed) {
            cast(mc, player);
        }
    }

    private static boolean drainClicks(KeyMapping mapping) {
        boolean clicked = false;
        while (mapping.consumeClick()) {
            clicked = true;
        }
        return clicked;
    }

    /**
     * Turns a binding's clicks into one event per physical key-down. Vanilla also "clicks" a binding
     * for every OS auto-repeat while a key is held, so a click alone doesn't mean a new press: it
     * only counts if the key wasn't already held on the previous tick. This is the same
     * one-action-per-press rule Ars Nouveau uses for its quick-cast keys. A tap shorter than one
     * tick still registers through its click.
     */
    private static final class PressTracker {
        private final KeyMapping mapping;
        private boolean wasDown = false;

        PressTracker(KeyMapping mapping) {
            this.mapping = mapping;
        }

        boolean pollNewPress() {
            boolean clicked = drainClicks(mapping);
            boolean newPress = clicked && !wasDown;
            wasDown = mapping.isDown();
            return newPress;
        }
    }

    private static void openRadial(Minecraft mc, LocalPlayer player) {
        ItemStack book = SelectedSpellbook.bookOrReport(player);
        if (!(book.getItem() instanceof SpellBook spellBook)) {
            return;
        }
        RadialMenu<AbstractSpellPart> menu = new RadialMenu<>(
                slot -> SelectedSpellbook.selectSpell(player, book, slot),
                spellBook.getRadialMenuSlotsForSpellpart(book),
                SecondaryIconPosition.NORTH,
                RenderUtils::drawSpellPart,
                0);
        mc.setScreen(new CurioSpellbookRadialScreen(menu));
    }

    private static void cast(Minecraft mc, LocalPlayer player) {
        // If the player rebinds our cast key onto Ars Nouveau's own "Selection HUD" key (V by
        // default) and is holding a radial item such as a spellbook, Ars opens the held item's
        // menu on this press. Let that win instead of also casting the worn book.
        if (ModKeyMappings.CAST_SPELL.same(ModKeyBindings.OPEN_RADIAL_HUD)
                && (player.getMainHandItem().getItem() instanceof IRadialProvider
                || player.getOffhandItem().getItem() instanceof IRadialProvider)) {
            return;
        }

        // Report an empty selected slot straight away. The server checks again, since this view of
        // the slots can lag Curios' sync by a tick.
        if (SelectedSpellbook.bookOrReport(player).isEmpty()) {
            return;
        }

        Entity camera = mc.getCameraEntity() != null ? mc.getCameraEntity() : player;
        PacketDistributor.sendToServer(new CastCurioBookPayload(camera.getXRot(), camera.getYRot()));
    }
}
