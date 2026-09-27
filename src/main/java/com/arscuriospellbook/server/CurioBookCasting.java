package com.arscuriospellbook.server;

import com.arscuriospellbook.CurioSpellbookUtil;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Server-side casting from a spellbook that lives in a Curios slot rather than a hand.
 *
 * <p>This mirrors {@code AbstractCaster#castSpell}, but that method always reads the casting stack
 * from {@code entity.getItemInHand(hand)}. Here the worn book is passed explicitly everywhere a
 * stack is needed (the SpellContext's caster tool, entity casts, and the UseOnContext for block
 * casts), so the player's hands are never touched. The caster's overridable hooks
 * ({@code getSpell}, {@code modifySpellBeforeCasting}, {@code getSpellResolver}) are still called,
 * so addon casters behave as they would when held.
 */
public final class CurioBookCasting {
    /** Hand reported to Ars APIs that require one. The book itself is never read from this hand. */
    private static final InteractionHand NOMINAL_HAND = InteractionHand.MAIN_HAND;

    private CurioBookCasting() {}

    /**
     * Casts the selected spell of the book in the player's selected ars_spellbook slot. If that slot
     * is empty (the client normally catches this first, but its view can lag Curios' sync by a tick),
     * nothing is cast and the player is told.
     */
    public static void cast(ServerPlayer player, float xRot, float yRot) {
        int bookSlot = SpellbookSelection.get(player);
        ItemStack book = CurioSpellbookUtil.getEquippedSpellbook(player, bookSlot);
        if (book.isEmpty()) {
            player.displayClientMessage(CurioSpellbookUtil.describeSlot(player, bookSlot), true);
            return;
        }
        AbstractCaster<?> caster = SpellCasterRegistry.from(book);
        if (caster == null) {
            return;
        }

        // Same trick as Ars Nouveau's PacketCastSpell: aim with the client's camera rotation.
        float prevXRot = player.getXRot();
        float prevYHeadRot = player.getYHeadRot();
        player.setXRot(xRot);
        player.setYHeadRot(yRot);
        try {
            castFromStack(player, book, caster);
        } finally {
            player.setXRot(prevXRot);
            player.setYHeadRot(prevYHeadRot);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void castFromStack(ServerPlayer player, ItemStack book, AbstractCaster caster) {
        ServerLevel level = player.serverLevel();

        Spell spell = caster.getSpell(level, player, NOMINAL_HAND, caster);
        spell = caster.modifySpellBeforeCasting(level, player, NOMINAL_HAND, spell);
        if (!spell.isValid()) {
            PortUtil.sendMessageNoSpam(player, Component.translatable("ars_nouveau.invalid_spell"));
            return;
        }

        SpellContext context = new SpellContext(level, spell, player, new PlayerCaster(player), book);
        SpellResolver resolver = caster.getSpellResolver(context, level, player, NOMINAL_HAND);

        boolean isSensitive = resolver.spell.getBuffsAtIndex(0, player, AugmentSensitive.INSTANCE) > 0;
        double reach = 0.5 + player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        HitResult result = SpellUtil.rayTrace(player, reach, 1, isSensitive);

        // Note: a held book skips casting when right-clicking a block entity, because the
        // right-click is meant for the block (opening a chest, inscribing at a scribes table).
        // The cast key has no competing block interaction, so it always casts.

        if (result instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity) {
            resolver.onCastOnEntity(book, entityHit.getEntity(), NOMINAL_HAND);
            return;
        }

        if (result instanceof BlockHitResult blockHit && (result.getType() == HitResult.Type.BLOCK || isSensitive)) {
            resolver.onCastOnBlock(new BookUseOnContext(level, player, book, blockHit));
            return;
        }

        resolver.onCast(book, level);
    }

    /** UseOnContext whose item is the worn book rather than whatever is in the player's hand. */
    private static final class BookUseOnContext extends UseOnContext {
        BookUseOnContext(ServerLevel level, ServerPlayer player, ItemStack book, BlockHitResult hit) {
            super(level, player, NOMINAL_HAND, book, hit);
        }
    }
}
