package com.arscuriospellbook.client;

import com.arscuriospellbook.ArsCurioSpellbook;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's hotkeys. Registered through NeoForge, so they appear under Options > Controls >
 * Key Binds in their own "Ars Curio Spellbook" category and can be rebound (including modifiers
 * and mouse buttons) like any other binding.
 *
 * <p>Defaults:
 * <ul>
 *   <li>B casts (left hand, reachable while moving).</li>
 *   <li>H opens the spell radial (right hand).</li>
 *   <li>Shift+K opens the spell editor.</li>
 *   <li>K / Ctrl+K select the next / previous ars_spellbook slot that holds a book, for players
 *       with more than one such slot.</li>
 *   <li>Next / previous spell on the selected book: Ctrl+L / Shift+L.</li>
 * </ul>
 * Cast, radial and editor all act on the selected book. None of these defaults is used by
 * vanilla, Ars Nouveau or Iron's Spells 'n Spellbooks. Note that B is JourneyMap's and Xaero's
 * Minimap's default "create waypoint" key.
 */
@EventBusSubscriber(modid = ArsCurioSpellbook.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories." + ArsCurioSpellbook.MODID;

    public static final KeyMapping OPEN_SPELL_RADIAL = new KeyMapping(
            "key." + ArsCurioSpellbook.MODID + ".open_spell_radial",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    public static final KeyMapping CAST_SPELL = new KeyMapping(
            "key." + ArsCurioSpellbook.MODID + ".cast_spell",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY);

    public static final KeyMapping EDIT_SPELLBOOK = new KeyMapping(
            "key." + ArsCurioSpellbook.MODID + ".edit_spellbook",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    public static final KeyMapping NEXT_SPELLBOOK = new KeyMapping(
            "key." + ArsCurioSpellbook.MODID + ".next_spellbook",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    public static final KeyMapping PREVIOUS_SPELLBOOK = new KeyMapping(
            "key." + ArsCurioSpellbook.MODID + ".previous_spellbook",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    public static final KeyMapping NEXT_SPELL = new KeyMapping(
            "key." + ArsCurioSpellbook.MODID + ".next_spell",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            CATEGORY);

    public static final KeyMapping PREVIOUS_SPELL = new KeyMapping(
            "key." + ArsCurioSpellbook.MODID + ".previous_spell",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            CATEGORY);

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SPELL_RADIAL);
        event.register(CAST_SPELL);
        event.register(EDIT_SPELLBOOK);
        event.register(NEXT_SPELLBOOK);
        event.register(PREVIOUS_SPELLBOOK);
        event.register(NEXT_SPELL);
        event.register(PREVIOUS_SPELL);
    }
}
