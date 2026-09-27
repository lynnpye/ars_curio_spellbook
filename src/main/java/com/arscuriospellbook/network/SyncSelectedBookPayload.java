package com.arscuriospellbook.network;

import com.arscuriospellbook.ArsCurioSpellbook;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the player's selected ars_spellbook slot, sent at login, after a datapack
 * reload, and whenever the server changes it. Handled on the client by
 * {@code SelectedSpellbook#onServerSync}; see {@link ModNetworking} for how that's registered
 * without loading client code on a dedicated server.
 */
public record SyncSelectedBookPayload(int bookSlot) implements CustomPacketPayload {
    public static final Type<SyncSelectedBookPayload> TYPE = new Type<>(ArsCurioSpellbook.id("sync_selected_book"));
    public static final StreamCodec<ByteBuf, SyncSelectedBookPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SyncSelectedBookPayload::new, SyncSelectedBookPayload::bookSlot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
