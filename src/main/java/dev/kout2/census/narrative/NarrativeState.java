package dev.kout2.census.narrative;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which narrative tropes have already fired for a mob (so each fires at
 * most once) and any standing narrative target — currently the person this mob
 * has sworn to avenge.
 *
 * Persisted, not synced: the story logic is server-side.
 */
public final class NarrativeState {
    private final Set<String> fired;
    private Optional<UUID> avengeTarget;

    public NarrativeState() {
        this.fired = new HashSet<>();
        this.avengeTarget = Optional.empty();
    }

    private NarrativeState(Collection<String> fired, Optional<UUID> avengeTarget) {
        this.fired = new HashSet<>(fired);
        this.avengeTarget = avengeTarget;
    }

    public static final MapCodec<NarrativeState> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.listOf().optionalFieldOf("fired", List.of())
                    .forGetter(s -> List.copyOf(s.fired)),
            UUIDUtil.CODEC.optionalFieldOf("avengeTarget").forGetter(s -> s.avengeTarget)
    ).apply(inst, NarrativeState::new));

    public boolean hasFired(String tropeId) {
        return fired.contains(tropeId);
    }

    public void markFired(String tropeId) {
        fired.add(tropeId);
    }

    public Optional<UUID> avengeTarget() {
        return avengeTarget;
    }

    public void setAvengeTarget(UUID target) {
        this.avengeTarget = Optional.of(target);
    }
}
