package dev.kout2.census.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers Census's custom payloads. Wired from the mod constructor onto the
 * mod event bus (see {@code CensusMod}).
 */
public final class CensusNetwork {
    private CensusNetwork() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Lambda (not a method ref) so the client-only handler class is loaded
        // lazily on invocation, never during registration on a dedicated server.
        registrar.playToClient(ProfilePayload.TYPE, ProfilePayload.STREAM_CODEC,
                (payload, context) ->
                        dev.kout2.census.client.ClientPayloadHandler.handleProfile(payload, context));
    }
}
