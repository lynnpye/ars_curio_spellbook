package com.arscuriospellbook.network;

import com.arscuriospellbook.ArsCurioSpellbook;
import com.arscuriospellbook.CurioSpellbookUtil;
import com.arscuriospellbook.server.SpellbookSelection;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: select spell {@code spellSlot} on the book in the player's selected
 * ars_spellbook slot (as stored on the server). The server replies with the status line, so it
 * always describes what the server actually applied.
 */
public record SetCurioBookSlotPayload(int spellSlot) implements CustomPacketPayload {
    public static final Type<SetCurioBookSlotPayload> TYPE = new Type<>(ArsCurioSpellbook.id("set_curio_book_slot"));
    public static final StreamCodec<ByteBuf, SetCurioBookSlotPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SetCurioBookSlotPayload::new, SetCurioBookSlotPayload::spellSlot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetCurioBookSlotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            int bookSlot = SpellbookSelection.get(player);
            ItemStack book = CurioSpellbookUtil.getEquippedSpellbook(player, bookSlot);
            AbstractCaster<?> caster = SpellCasterRegistry.from(book);
            if (!book.isEmpty() && caster != null
                    && payload.spellSlot() >= 0 && payload.spellSlot() < caster.getMaxSlots()) {
                caster.setCurrentSlot(payload.spellSlot()).saveToStack(book);
            }
            // Report what the server actually has, whether or not the pick was applied.
            player.displayClientMessage(CurioSpellbookUtil.describeSlot(player, bookSlot), true);
        });
    }
}
