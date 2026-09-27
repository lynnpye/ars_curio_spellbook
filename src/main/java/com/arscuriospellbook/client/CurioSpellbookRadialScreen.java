package com.arscuriospellbook.client;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenu;
import com.hollingsworth.arsnouveau.setup.config.Config;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Ars Nouveau's radial spell menu, driven by our own key instead of Ars' "Selection HUD" key.
 *
 * <p>In "hold" mode, Ars' {@link GuiRadialMenu} closes itself as soon as <em>Ars'</em> radial key is
 * not held, which would close this menu instantly when opened with a different key. So the
 * built-in hold handling is switched off and re-implemented here against
 * {@link ModKeyMappings#OPEN_SPELL_RADIAL}, using the standard Screen hooks: {@link #tick()} for the
 * hold-mode release check, and {@link #keyPressed}/{@link #mouseClicked} for the toggle-mode close.
 * Whether the menu is hold or toggle follows Ars' own client config option
 * ({@code toggleSelectionHUD}), so both menus behave the same way.
 */
public class CurioSpellbookRadialScreen extends GuiRadialMenu<AbstractSpellPart> {
    private final boolean holdToOpen;

    public CurioSpellbookRadialScreen(RadialMenu<AbstractSpellPart> menu) {
        super(menu);
        this.holdToOpen = !Config.TOGGLE_RADIAL_HUD.get();
        setHoldToOpenGUI(false);
    }

    /** Hold mode: once the open key is released, select the hovered spell (if any) and close. */
    @Override
    public void tick() {
        super.tick();
        if (holdToOpen && !isPhysicallyDown(ModKeyMappings.OPEN_SPELL_RADIAL)) {
            // GuiRadialMenu#mouseClicked commits the hovered slot (if any) and closes the screen.
            // The coordinates are unused by that logic; the hovered slot is tracked during render.
            super.mouseClicked(0, 0, 0);
            if (Minecraft.getInstance().screen == this) {
                onClose();
            }
        }
    }

    /** Toggle mode: pressing the open key again closes the menu without changing the spell. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!holdToOpen && ModKeyMappings.OPEN_SPELL_RADIAL.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Toggle mode, for a radial key bound to a mouse button. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!holdToOpen && ModKeyMappings.OPEN_SPELL_RADIAL.matchesMouse(button)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Whether the binding's key or mouse button is physically held. Needed because
     * {@link KeyMapping#isDown()} always reports false while a screen is open.
     */
    private static boolean isPhysicallyDown(KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }
}
