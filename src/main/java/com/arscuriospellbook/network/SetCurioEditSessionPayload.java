package com.arscuriospellbook.network;

import com.arscuriospellbook.ArsCurioSpellbook;
import com.arscuriospellbook.server.CurioEditSessions;
import com.arscuriospellbook.server.SpellbookSelection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the player's spell editor is (or is no longer) showing the worn book in
 * ars_spellbook slot {@code bookSlot}, so Ars' editor save packets should target it.
 *
 * <p>The server only starts the session if {@code bookSlot} matches its own selection. The client's
 * copy of the selection can briefly lag the server's (for example right after the server resets it
 * because a slot was removed). If they disagree, the editor is showing a different book from the
 * one the server would save to, so the server starts a <em>blocked</em> session that drops every
 * save, and re-sends the selection, which makes the client close the editor. {@code bookSlot} is
 * ignored when {@code active} is false.
 */
public record SetCurioEditSessionPayload(boolean active, int bookSlot) implements CustomPacketPayload {
    public static final Type<SetCurioEditSessionPayload> TYPE = new Type<>(ArsCurioSpellbook.id("set_curio_edit_session"));
    public static final StreamCodec<ByteBuf, SetCurioEditSessionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetCurioEditSessionPayload::active,
            ByteBufCodecs.VAR_INT, SetCurioEditSessionPayload::bookSlot,
            SetCurioEditSessionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetCurioEditSessionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!payload.active()) {
                CurioEditSessions.end(player);
            } else if (payload.bookSlot() == SpellbookSelection.get(player)) {
                CurioEditSessions.start(player, payload.bookSlot());
            } else {
                CurioEditSessions.block(player);
                SpellbookSelection.sync(player);
            }
        });
    }
}
