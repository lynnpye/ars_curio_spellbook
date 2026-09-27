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
 * Client -> server: K / Ctrl+K. Step the selected ars_spellbook slot forward ({@code step} > 0) or
 * back. The server owns the selection, so it does the stepping, stores the result, syncs it back and
 * reports it.
 */
public record CycleSelectedBookPayload(int step) implements CustomPacketPayload {
    public static final Type<CycleSelectedBookPayload> TYPE = new Type<>(ArsCurioSpellbook.id("cycle_selected_book"));
    public static final StreamCodec<ByteBuf, CycleSelectedBookPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CycleSelectedBookPayload::new, CycleSelectedBookPayload::step);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleSelectedBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpellbookSelection.cycle(player, payload.step());
            }
        });
    }
}
