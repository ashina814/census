package dev.kout2.census.event;

import dev.kout2.census.Census;
import dev.kout2.census.CensusMod;
import dev.kout2.census.census.CensusRegistry;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.reflection.Reflector;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.Gossip;
import dev.kout2.census.social.SocialBonds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The social heartbeat — gossip, bond growth, courtship and couple-driven
 * birth — driven by a spread tick scheduler rather than a once-every-N-ticks
 * spike.
 *
 * Each level keeps a cached roster of its censused villagers, re-scanned only
 * every {@link #ROSTER_REFRESH} ticks (so the O(n) entity query runs no more
 * often than before). A rolling cursor then processes just a thin slice of that
 * roster every tick, so the whole village is visited about once per refresh
 * window with the work amortised across all ticks instead of stalling one.
 * This keeps per-tick time flat as villages grow into the hundreds.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class SocialEventHandlers {
    private static final int ROSTER_REFRESH = 200;     // re-scan villagers ~every 10s
    private static final int SPREAD_TICKS = 200;        // visit the whole roster over this window
    private static final int MAX_MEETINGS_PER_TICK = 2; // server-wide meeting budget per tick
    private static final double MEET_RADIUS = 8.0;
    private static final float MEET_CHANCE = 0.25f;

    private static final Map<ResourceKey<Level>, Roster> ROSTERS = new HashMap<>();

    private SocialEventHandlers() {}

    /** Per-level cached villager list with a rolling cursor. */
    private static final class Roster {
        List<? extends Villager> villagers = List.of();
        int cursor = 0;
        long refreshedAt = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.getTickCount();
        int meetingBudget = MAX_MEETINGS_PER_TICK;

        CensusRegistry registry = CensusRegistry.get(server);
        boolean anyGrief = registry.hasPendingGrief();

        for (ServerLevel level : server.getAllLevels()) {
            Roster roster = ROSTERS.computeIfAbsent(level.dimension(), k -> new Roster());
            if (now - roster.refreshedAt >= ROSTER_REFRESH) {
                roster.villagers = level.getEntities(EntityType.VILLAGER, Census::isCensused);
                roster.refreshedAt = now;
            }
            int size = roster.villagers.size();
            if (size == 0) {
                continue;
            }
            int perTick = Math.max(1, (size + SPREAD_TICKS - 1) / SPREAD_TICKS);
            for (int i = 0; i < perTick; i++) {
                Villager villager = roster.villagers.get(roster.cursor % size);
                roster.cursor++;
                if (!villager.isAlive() || !Census.isCensused(villager)) {
                    continue; // died/unloaded since the roster was taken
                }
                if (anyGrief) {
                    deliverGrief(registry, villager);
                }
                if (Reflector.isDue(villager, now)) {
                    Reflector.reflect(villager, now);
                }
                SocialBonds.tryReproduce(level, villager, now);
                if (meetingBudget > 0 && villager.getRandom().nextFloat() < MEET_CHANCE) {
                    Villager neighbour = nearestNeighbour(level, villager);
                    if (neighbour != null) {
                        Gossip.exchange(villager, neighbour, now);
                        SocialBonds.meet(villager, neighbour);
                        meetingBudget--;
                    }
                }
            }
        }
    }

    /** Forget cached rosters when the server stops (avoids holding stale entities). */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ROSTERS.clear();
    }

    /** Delivers any world-registry grief queued for this villager (distant kin revenge). */
    private static void deliverGrief(CensusRegistry registry, Villager villager) {
        UUID personaId = villager.getData(ModAttachments.PERSONA).id();
        for (CensusRegistry.PendingGrief grief : registry.drainGrief(personaId)) {
            Census.observe(villager, EventType.RELATIVE_KILLED, grief.killer().orElse(null));
        }
    }

    private static Villager nearestNeighbour(ServerLevel level, Villager self) {
        AABB box = self.getBoundingBox().inflate(MEET_RADIUS);
        Villager nearest = null;
        double best = Double.MAX_VALUE;
        for (Villager v : level.getEntitiesOfClass(Villager.class, box,
                v -> v != self && Census.isCensused(v))) {
            double d = v.distanceToSqr(self);
            if (d < best) {
                best = d;
                nearest = v;
            }
        }
        return nearest;
    }
}
