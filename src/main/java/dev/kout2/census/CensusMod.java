package dev.kout2.census;

import com.mojang.logging.LogUtils;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Census — emergent social simulation Mod entry point.
 *
 * Architecture overview (full design: plans/streamed-tinkering-pizza.md):
 *   Persona       (entity attachment, synced)
 *   MemoryStream  (entity attachment, server-only)
 *   EmotionalState(entity attachment, synced)
 *   CensusRegistry(level SavedData)
 *   NarrativeEngine (tropes, data-driven)
 *
 * Phase 0 scaffolds the project. Persona arrives in Phase 1.
 */
@Mod(CensusMod.MODID)
public class CensusMod {
    public static final String MODID = "census";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /**
     * Census creative tab. Holds the Census Book and (later) gravestones, etc.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CENSUS_TAB =
            CREATIVE_MODE_TABS.register("census_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.census"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.CENSUS_BOOK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CENSUS_BOOK.get());
                    }).build());

    public CensusMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Registry bootstrap
        ModItems.register(modEventBus);
        ModAttachments.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Census: common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Census: server starting — census registry ready");
    }
}
