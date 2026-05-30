package dev.kout2.census.memory;

import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Entry point for writing memories. Centralises the "only censused mobs
 * remember, importance is personality-weighted" rules so event handlers stay
 * thin.
 */
public final class Memories {
    private Memories() {}

    /**
     * Records an event using an already-resolved persona. Importance is scored
     * from the personality; valence comes from the event type. Always routed
     * through {@link dev.kout2.census.Census#observe} so memory, emotion and
     * narrative stay in lock-step — there is intentionally no persona-less
     * shortcut that would bypass that fan-out.
     *
     * @param subject the other party's UUID (player or mob), or {@code null}
     * @return the stored entry, so callers can reuse its importance/valence
     */
    public static MemoryEntry record(LivingEntity holder, Persona persona, EventType type,
                                     @Nullable UUID subject) {
        long now = holder.level().getGameTime();
        float importance = ImportanceScorer.score(type, persona.personality());
        MemoryEntry entry = new MemoryEntry(
                now, type, Optional.ofNullable(subject),
                importance, type.baseValence(), Optional.empty());
        holder.getData(ModAttachments.MEMORY).add(entry, now);
        return entry;
    }
}
