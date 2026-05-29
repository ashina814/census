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
     * Records an event on {@code holder} if it has a persona. The importance is
     * scored from the holder's personality; valence comes from the event type.
     *
     * @param subject the other party's UUID (player or mob), or {@code null}
     */
    public static void record(LivingEntity holder, EventType type, @Nullable UUID subject) {
        if (!holder.hasData(ModAttachments.PERSONA)) {
            return;
        }
        Persona persona = holder.getData(ModAttachments.PERSONA);
        long now = holder.level().getGameTime();
        float importance = ImportanceScorer.score(type, persona.personality());
        MemoryEntry entry = new MemoryEntry(
                now, type, Optional.ofNullable(subject),
                importance, type.baseValence(), Optional.empty());
        holder.getData(ModAttachments.MEMORY).add(entry, now);
    }
}
