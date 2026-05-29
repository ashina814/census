package dev.kout2.census;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side entry point. Renderers and HUD overlays (persona name tags,
 * emotion overlays) will register here in later phases.
 */
@Mod(value = CensusMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CensusMod.MODID, value = Dist.CLIENT)
public class CensusModClient {
    public CensusModClient(ModContainer container) {
        // Future: ConfigurationScreen, ClientHooks for PersonaNameRenderer, etc.
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CensusMod.LOGGER.info("Census: client setup");
    }
}
