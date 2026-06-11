package dev.kout2.census.narrative;

import dev.kout2.census.CensusMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads tropes from {@code data/<namespace>/census_tropes/*.json} — vanilla
 * datapack machinery, so packs can add, override or remove stories and
 * {@code /reload} picks the changes up live.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class TropeLoader extends SimpleJsonResourceReloadListener<TropeDefinition> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CensusMod.MODID, "tropes");

    private static volatile List<Trope> tropes = List.of();

    private TropeLoader() {
        super(TropeDefinition.CODEC, FileToIdConverter.json("census_tropes"));
    }

    /** The currently loaded tropes (empty until the first datapack load). */
    public static List<Trope> tropes() {
        return tropes;
    }

    @Override
    protected void apply(Map<Identifier, TropeDefinition> definitions,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        List<Trope> loaded = new ArrayList<>(definitions.size());
        definitions.forEach((id, definition) -> {
            try {
                loaded.add(new DataDrivenTrope(id.toString(), definition));
            } catch (IllegalArgumentException e) {
                // Bad trait/emotion name in a pack: skip that trope, keep the rest.
                CensusMod.LOGGER.error("Census: invalid trope {}: {}", id, e.getMessage());
            }
        });
        tropes = List.copyOf(loaded);
        CensusMod.LOGGER.info("Census: loaded {} narrative trope(s)", loaded.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ID, new TropeLoader());
    }
}
