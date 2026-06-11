package dev.kout2.census.ai.utility;

import dev.kout2.census.CensusMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.List;
import java.util.Map;

/**
 * Loads utility actions from {@code data/<namespace>/census_actions/*.json};
 * the same datapack machinery as {@link dev.kout2.census.narrative.TropeLoader},
 * so packs can retune or extend behaviour and {@code /reload} applies it live.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class ActionLoader extends SimpleJsonResourceReloadListener<ActionDefinition> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CensusMod.MODID, "actions");

    private static volatile List<ActionDefinition> actions = List.of();

    private ActionLoader() {
        super(ActionDefinition.CODEC, FileToIdConverter.json("census_actions"));
    }

    /** The currently loaded actions (empty until the first datapack load). */
    public static List<ActionDefinition> actions() {
        return actions;
    }

    @Override
    protected void apply(Map<Identifier, ActionDefinition> definitions,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        actions = List.copyOf(definitions.values());
        CensusMod.LOGGER.info("Census: loaded {} utility action(s)", actions.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ID, new ActionLoader());
    }
}
