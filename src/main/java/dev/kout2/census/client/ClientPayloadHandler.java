package dev.kout2.census.client;

import dev.kout2.census.network.ProfilePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-side endpoints for Census payloads. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleProfile(ProfilePayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                Minecraft.getInstance().setScreen(new CensusProfileScreen(payload)));
    }
}
