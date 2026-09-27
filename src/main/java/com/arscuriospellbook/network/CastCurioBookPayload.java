package com.arscuriospellbook.network;

import com.arscuriospellbook.ArsCurioSpellbook;
import com.arscuriospellbook.server.CurioBookCasting;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: cast the selected spell of the book in the player's selected ars_spellbook slot
 * (as stored on the server).
 * Carries the client camera rotation, the same way Ars Nouveau's own cast packet does, so spells
 * aim where the player is actually looking.
 */
public record CastCurioBookPayload(float xRot, float yRot) implements CustomPacketPayload {
    public static final Type<CastCurioBookPayload> TYPE = new Type<>(ArsCurioSpellbook.id("cast_curio_book"));
    public static final StreamCodec<ByteBuf, CastCurioBookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CastCurioBookPayload::xRot,
            ByteBufCodecs.FLOAT, CastCurioBookPayload::yRot,
            CastCurioBookPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastCurioBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CurioBookCasting.cast(player, payload.xRot(), payload.yRot());
            }
        });
    }
}
