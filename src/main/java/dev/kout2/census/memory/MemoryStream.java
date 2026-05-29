package dev.kout2.census.memory;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A mob's chronological log of observations — the substrate the Reflection
 * pass (Phase 3) will distil into lasting insights, after Park et al.'s
 * "Generative Agents" memory-stream design.
 *
 * Server-only: never synced to clients. Bounded at {@link #MAX_OBSERVATIONS};
 * when full, the lowest-scoring entry (importance decayed by age) is evicted,
 * so vivid and recent memories survive while trivial old ones fade.
 */
public final class MemoryStream {
    public static final int MAX_OBSERVATIONS = 200;
    private static final float AGE_DECAY_PER_DAY = 0.5f;

    private final List<MemoryEntry> observations;

    public MemoryStream() {
        this.observations = new ArrayList<>();
    }

    private MemoryStream(List<MemoryEntry> initial) {
        this.observations = new ArrayList<>(initial);
    }

    public static final MapCodec<MemoryStream> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            MemoryEntry.CODEC.listOf().optionalFieldOf("observations", List.of())
                    .forGetter(ms -> ms.observations)
    ).apply(inst, MemoryStream::new));

    /** Appends an observation, evicting the weakest memory if over capacity. */
    public void add(MemoryEntry entry, long currentTick) {
        observations.add(entry);
        if (observations.size() > MAX_OBSERVATIONS) {
            int weakest = 0;
            float weakestScore = Float.MAX_VALUE;
            for (int i = 0; i < observations.size(); i++) {
                float s = retentionScore(observations.get(i), currentTick);
                if (s < weakestScore) {
                    weakestScore = s;
                    weakest = i;
                }
            }
            observations.remove(weakest);
        }
    }

    /** Importance decayed linearly by age; used for eviction. */
    private static float retentionScore(MemoryEntry entry, long currentTick) {
        float ageDays = Math.max(0L, currentTick - entry.tick()) / 24000.0f;
        return entry.importance() - ageDays * AGE_DECAY_PER_DAY;
    }

    /** Most recent {@code n} entries, newest first. */
    public List<MemoryEntry> recent(int n) {
        int from = Math.max(0, observations.size() - n);
        List<MemoryEntry> slice = new ArrayList<>(observations.subList(from, observations.size()));
        slice.sort((a, b) -> Long.compare(b.tick(), a.tick()));
        return slice;
    }

    public List<MemoryEntry> all() {
        return List.copyOf(observations);
    }

    public int size() {
        return observations.size();
    }

    public boolean isEmpty() {
        return observations.isEmpty();
    }
}
