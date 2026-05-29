package dev.kout2.census.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * One remembered observation.
 *
 * @param tick       game time the event happened
 * @param type       what kind of event
 * @param subject    the other party (player or mob UUID) — empty for ambient
 *                   events with no clear cause
 * @param importance 0–10, computed at record time from {@link ImportanceScorer}
 * @param valence    emotional sign in [-1,1], copied from the event type
 * @param note       optional human-readable annotation for inspection
 */
public record MemoryEntry(
        long tick,
        EventType type,
        Optional<UUID> subject,
        float importance,
        float valence,
        Optional<String> note
) {
    public static final Codec<MemoryEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.fieldOf("tick").forGetter(MemoryEntry::tick),
            EventType.CODEC.fieldOf("type").forGetter(MemoryEntry::type),
            UUIDUtil.CODEC.optionalFieldOf("subject").forGetter(MemoryEntry::subject),
            Codec.FLOAT.fieldOf("importance").forGetter(MemoryEntry::importance),
            Codec.FLOAT.fieldOf("valence").forGetter(MemoryEntry::valence),
            Codec.STRING.optionalFieldOf("note").forGetter(MemoryEntry::note)
    ).apply(inst, MemoryEntry::new));
}
