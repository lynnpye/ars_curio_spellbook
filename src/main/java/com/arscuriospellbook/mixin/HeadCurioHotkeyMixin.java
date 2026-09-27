package com.arscuriospellbook.mixin;

import com.arscuriospellbook.CurioSpellbookUtil;
import com.hollingsworth.arsnouveau.client.keybindings.KeyHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps Ars Nouveau's "Head Curio Menu" action (default G) away from the books in the
 * ars_spellbook slots (there may be more than one).
 *
 * <p>{@code KeyHandler.checkCurioHotkey} runs when that binding's key is pressed. It loops over
 * every worn curio and toggles the radial menu of each item that has one. It's meant for the
 * Alchemist's Crown. A worn spellbook also has a radial menu, which causes two problems:
 * <ul>
 *   <li>With the crown also worn, the loop opens one menu and then closes it for the other item,
 *       so nothing appears.</li>
 *   <li>With the book alone, the book's menu opens, but a spell picked there is saved to the book
 *       in the player's hand, not the worn one.</li>
 * </ul>
 * Inside that loop only, every book in an ars_spellbook slot reads as an empty stack, so the
 * action behaves exactly as in plain Ars. This doesn't depend on which key the action is bound
 * to, and other Ars code that iterates worn curios (mana discounts, spell stat modifiers) is
 * unaffected. Worn books' spells
 * are selected with this mod's own keys instead.
 */
@Mixin(KeyHandler.class)
public abstract class HeadCurioHotkeyMixin {

    @ModifyExpressionValue(
            method = "checkCurioHotkey",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/items/IItemHandlerModifiable;getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack ars_curio_spellbook$hideWornBooks(ItemStack stack) {
        // Every ars_spellbook slot, not just the selected one: any worn book caught in this loop
        // would bring back both problems described above.
        if (CurioSpellbookUtil.isInSpellbookSlot(Minecraft.getInstance().player, stack)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }
}
