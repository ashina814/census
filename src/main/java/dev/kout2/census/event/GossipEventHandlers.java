package dev.kout2.census.event;

import dev.kout2.census.CensusMod;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.Gossip;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Drives {@link Gossip} on a throttled server tick.
 *
 * Every {@link #INTERVAL} ticks a fraction of villagers each strike up a chat
 * with their nearest neighbour and share rumours. A per-tick {@link #MAX_PAIRS}
 * budget caps the cost regardless of village size, so this stays cheap even
 * with hundreds of villagers loaded.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class GossipEventHandlers {
    private static final int INTERVAL = 200;        // ~10s between gossip rounds
    private static final double MEET_RADIUS = 8.0;
    private static final int MAX_PAIRS = 50;        // exchanges per round, server-wide
    private static final float GOSSIP_CHANCE = 0.25f;

    private GossipEventHandlers() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        long now = server.getTickCount();
        int budget = MAX_PAIRS;
        for (ServerLevel level : server.getAllLevels()) {
            if (budget <= 0) {
                break;
            }
            List<? extends Villager> villagers =
                    level.getEntities(EntityType.VILLAGER, v -> v.hasData(ModAttachments.PERSONA));
            for (Villager speaker : villagers) {
                if (budget <= 0) {
                    break;
                }
                if (speaker.getRandom().nextFloat() > GOSSIP_CHANCE) {
                    continue;
                }
                Villager listener = nearestPartner(level, speaker);
                if (listener != null) {
                    Gossip.exchange(speaker, listener, now);
                    budget--;
                }
            }
        }
    }

    private static Villager nearestPartner(ServerLevel level, Villager speaker) {
        AABB box = speaker.getBoundingBox().inflate(MEET_RADIUS);
        return level.getEntitiesOfClass(Villager.class, box,
                        v -> v != speaker && v.hasData(ModAttachments.PERSONA)).stream()
                .min(Comparator.comparingDouble(v -> v.distanceToSqr(speaker)))
                .orElse(null);
    }
}
