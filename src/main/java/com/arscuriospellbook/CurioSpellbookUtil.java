package com.arscuriospellbook;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Side-agnostic helpers for the spellbooks worn in the {@code ars_spellbook} Curios slots.
 *
 * <p>Modpacks can give players more than one {@code ars_spellbook} slot, so books are identified by
 * their index within that slot type ({@code bookSlot}). Which slot is selected is stored on the
 * server (see {@code SpellbookSelection}).
 */
public final class CurioSpellbookUtil {
    private CurioSpellbookUtil() {}

    /** A spellbook in one of the ars_spellbook slots. {@code stack} is the live stored stack. */
    public record WornBook(int bookSlot, ItemStack stack) {}

    /** All spellbooks in the entity's ars_spellbook slots, in slot order. */
    public static List<WornBook> getEquippedSpellbooks(@Nullable LivingEntity entity) {
        List<WornBook> books = new ArrayList<>();
        IItemHandler stacks = getSlotStacks(entity);
        if (stacks != null) {
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.getItem() instanceof SpellBook) {
                    books.add(new WornBook(i, stack));
                }
            }
        }
        return books;
    }

    /**
     * The live spellbook stack in ars_spellbook slot {@code bookSlot}, or {@link ItemStack#EMPTY} if
     * that slot doesn't exist or doesn't hold a spellbook. Writing data components to the returned
     * stack persists the change.
     */
    public static @NotNull ItemStack getEquippedSpellbook(@Nullable LivingEntity entity, int bookSlot) {
        IItemHandler stacks = getSlotStacks(entity);
        if (stacks == null || bookSlot < 0 || bookSlot >= stacks.getSlots()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = stacks.getStackInSlot(bookSlot);
        return stack.getItem() instanceof SpellBook ? stack : ItemStack.EMPTY;
    }

    /** How many ars_spellbook slots the entity currently has (0 if none). */
    public static int getSpellbookSlotCount(@Nullable LivingEntity entity) {
        IItemHandler stacks = getSlotStacks(entity);
        return stacks == null ? 0 : stacks.getSlots();
    }

    /**
     * The one-line status for ars_spellbook slot {@code bookSlot}, shown on the action bar, for
     * example {@code Spellbook slot 2: Archmage Spell Book — 3 Fireball}. The spell part uses Ars
     * Nouveau's own HUD format: slot number, then name. Also covers an empty slot, a slot that
     * doesn't exist, and having no spellbook slots at all. Built from translatable components, so it
     * can be created on either side.
     */
    public static Component describeSlot(@Nullable LivingEntity entity, int bookSlot) {
        int slotCount = getSpellbookSlotCount(entity);
        if (slotCount == 0) {
            return Component.translatable("message." + ArsCurioSpellbook.MODID + ".no_spellbook_slots");
        }
        if (bookSlot < 0 || bookSlot >= slotCount) {
            return Component.translatable("message." + ArsCurioSpellbook.MODID + ".missing_spellbook_slot", bookSlot + 1);
        }
        ItemStack book = getEquippedSpellbook(entity, bookSlot);
        AbstractCaster<?> caster = SpellCasterRegistry.from(book);
        if (book.isEmpty() || caster == null) {
            return Component.translatable("message." + ArsCurioSpellbook.MODID + ".empty_spellbook_slot", bookSlot + 1);
        }
        String spellName = caster.getSpellName();
        String spell = (caster.getCurrentSlot() + 1) + (spellName.isEmpty() ? "" : " " + spellName);
        return Component.translatable("message." + ArsCurioSpellbook.MODID + ".selected_spellbook",
                bookSlot + 1, book.getHoverName(), spell);
    }

    /** Whether {@code stack} is one of the spellbooks in the entity's ars_spellbook slots. */
    public static boolean isInSpellbookSlot(@Nullable LivingEntity entity, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (WornBook book : getEquippedSpellbooks(entity)) {
            // Curios hands out the stored stack itself, so identity normally matches. ItemStack.matches
            // is a fallback in case a wrapper ever returns a copy.
            if (book.stack() == stack || ItemStack.matches(book.stack(), stack)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable IItemHandler getSlotStacks(@Nullable LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inv -> inv.getStacksHandler(ArsCurioSpellbook.SLOT_ID))
                .map(handler -> (IItemHandler) handler.getStacks())
                .orElse(null);
    }
}
