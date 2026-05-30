package dev.kout2.census.event;

import dev.kout2.census.CensusMod;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.Gossip;
import dev.kout2.census.social.SocialBonds;
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
 * The social heartbeat: on a throttled server tick, villagers meet their
 * neighbours to gossip and grow bonds (and pair off when fond enough), and
 * established couples occasionally bear a child.
 *
 * A per-round {@link #MAX_PAIRS} budget plus the heavy gating inside
 * {@link SocialBonds#tryReproduce} keep the cost flat regardless of village
 * size.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class SocialEventHandlers {
    private static final int INTERVAL = 200;        // ~10s between social rounds
    private static final double MEET_RADIUS = 8.0;
    private static final int MAX_PAIRS = 50;        // meetings per round, server-wide
    private static final float MEET_CHANCE = 0.25f;

    private SocialEventHandlers() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        long now = server.getTickCount();
        int budget = MAX_PAIRS;
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends Villager> villagers =
                    level.getEntities(EntityType.VILLAGER, v -> v.hasData(ModAttachments.PERSONA));
            for (Villager villager : villagers) {
                // Couples may bear a child (heavily gated inside).
                SocialBonds.tryReproduce(level, villager, now);

                if (budget <= 0 || villager.getRandom().nextFloat() > MEET_CHANCE) {
                    continue;
                }
                Villager neighbour = nearestNeighbour(level, villager);
                if (neighbour != null) {
                    Gossip.exchange(villager, neighbour, now);
                    SocialBonds.meet(villager, neighbour);
                    budget--;
                }
            }
        }
    }

    private static Villager nearestNeighbour(ServerLevel level, Villager self) {
        AABB box = self.getBoundingBox().inflate(MEET_RADIUS);
        return level.getEntitiesOfClass(Villager.class, box,
                        v -> v != self && v.hasData(ModAttachments.PERSONA)).stream()
                .min(Comparator.comparingDouble(v -> v.distanceToSqr(self)))
                .orElse(null);
    }
}
