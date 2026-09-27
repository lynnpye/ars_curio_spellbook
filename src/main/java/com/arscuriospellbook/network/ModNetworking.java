package com.arscuriospellbook.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "4";

    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(SetCurioBookSlotPayload.TYPE, SetCurioBookSlotPayload.STREAM_CODEC, SetCurioBookSlotPayload::handle);
        registrar.playToServer(CastCurioBookPayload.TYPE, CastCurioBookPayload.STREAM_CODEC, CastCurioBookPayload::handle);
        registrar.playToServer(SetCurioEditSessionPayload.TYPE, SetCurioEditSessionPayload.STREAM_CODEC, SetCurioEditSessionPayload::handle);
        registrar.playToServer(CycleSelectedBookPayload.TYPE, CycleSelectedBookPayload.STREAM_CODEC, CycleSelectedBookPayload::handle);
        registrar.playToServer(CycleSelectedSpellPayload.TYPE, CycleSelectedSpellPayload.STREAM_CODEC, CycleSelectedSpellPayload::handle);

        // Client-bound. The handler is a lambda whose body calls into the client package. A dedicated
        // server registers it but never runs it, and the JVM only resolves the client class when the
        // call first executes, so no client code is loaded there.
        registrar.playToClient(SyncSelectedBookPayload.TYPE, SyncSelectedBookPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> com.arscuriospellbook.client.SelectedSpellbook.onServerSync(payload.bookSlot())));
    }
}
