package dev.kout2.census.reputation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kout2.census.config.CensusConfig;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A mob's standing opinions of others — players and other mobs alike — keyed by
 * UUID, each clamped to [-100, +100].
 *
 * Opinions form firsthand from events (being fed raises them, being harmed
 * lowers them) and secondhand through {@link Gossip}. Because they are keyed by
 * UUID and stored per mob, a village can hold a shared, drifting reputation of
 * the player that no single villager witnessed in full.
 */
public final class ReputationBook {
    public static final float MIN = -100f;
    public static final float MAX = 100f;

    /** UUIDs serialize as strings so they can be map keys. */
    private static final Codec<UUID> UUID_STRING =
            Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private final Map<UUID, Float> opinions;

    public ReputationBook() {
        this.opinions = new HashMap<>();
    }

    private ReputationBook(Map<UUID, Float> initial) {
        this.opinions = new HashMap<>(initial);
    }

    public static final MapCodec<ReputationBook> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.unboundedMap(UUID_STRING, Codec.FLOAT).optionalFieldOf("opinions", Map.of())
                    .forGetter(b -> b.opinions)
    ).apply(inst, ReputationBook::new));

    public float opinionOf(UUID id) {
        return opinions.getOrDefault(id, 0f);
    }

    public boolean knows(UUID id) {
        return opinions.containsKey(id);
    }

    /** Nudges the opinion of {@code id} by {@code delta}, clamped to range. */
    public void adjust(UUID id, float delta) {
        set(id, opinionOf(id) + delta);
    }

    public void set(UUID id, float value) {
        opinions.put(id, Math.clamp(value, MIN, MAX));
        prune();
    }

    /**
     * Keeps the map bounded: when it exceeds the configured cap, forget the
     * weakest (closest-to-neutral) opinion — the one least likely to matter.
     * Early-outs in the common (under-cap) case so the per-write cost stays O(1);
     * since {@link #set} adds at most one entry, a single eviction suffices.
     */
    private void prune() {
        if (opinions.size() <= CensusConfig.REPUTATION_MAX_ENTRIES.get()) {
            return;
        }
        opinions.entrySet().stream()
                .min(Comparator.comparingDouble(e -> Math.abs(e.getValue())))
                .ifPresent(e -> opinions.remove(e.getKey()));
    }

    /** Read-only view (no defensive copy); callers must not mutate this book while iterating. */
    public Set<Map.Entry<UUID, Float>> entries() {
        return Collections.unmodifiableSet(opinions.entrySet());
    }

    public int size() {
        return opinions.size();
    }
}
