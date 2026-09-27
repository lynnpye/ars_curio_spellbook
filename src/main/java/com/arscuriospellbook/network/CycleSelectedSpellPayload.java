package com.arscuriospellbook.network;

import com.arscuriospellbook.ArsCurioSpellbook;
import com.arscuriospellbook.server.SpellbookSelection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: next / previous spell key. Step the spell of the book in the selected
 * ars_spellbook slot forward ({@code step} > 0) or back. The server does the stepping, like Ars
 * Nouveau's own next/previous keys for a held book, so rapid presses can't be lost to the client's
 * copy of the book lagging behind the server's.
 */
public record CycleSelectedSpellPayload(int step) implements CustomPacketPayload {
    public static final Type<CycleSelectedSpellPayload> TYPE = new Type<>(ArsCurioSpellbook.id("cycle_selected_spell"));
    public static final StreamCodec<ByteBuf, CycleSelectedSpellPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CycleSelectedSpellPayload::new, CycleSelectedSpellPayload::step);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleSelectedSpellPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpellbookSelection.cycleSpell(player, payload.step());
            }
        });
    }
}
