package dev.kout2.census;

import dev.kout2.census.emotion.appraisal.Appraiser;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.memory.Memories;
import dev.kout2.census.memory.MemoryEntry;
import dev.kout2.census.narrative.NarrativeEngine;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The single entry point event handlers use to feed something that happened
 * into a mob's inner life. One observation fans out to every per-mob system:
 * it is logged to memory and appraised into emotion (and, in later phases, will
 * also touch reputation and the narrative engine).
 *
 * Keeping this fan-out in one place means event handlers stay declarative —
 * "this mob was harmed by that player" — and never need to know how many
 * subsystems care.
 */
public final class Census {
    /** Scales (importance × valence) into a reputation swing toward the subject. */
    private static final float REPUTATION_SCALE = 3.0f;

    private Census() {}

    public static void observe(LivingEntity holder, EventType type, @Nullable UUID subject) {
        if (!holder.hasData(ModAttachments.PERSONA)) {
            return;
        }
        Persona persona = holder.getData(ModAttachments.PERSONA);
        MemoryEntry entry = Memories.record(holder, persona, type, subject);
        Appraiser.appraise(holder, persona, type);
        if (subject != null) {
            // Positive events raise the subject's standing, negative ones lower it,
            // weighted by how strongly the event was felt.
            float delta = entry.importance() * entry.valence() * REPUTATION_SCALE;
            holder.getData(ModAttachments.REPUTATION).adjust(subject, delta);
        }
        // A fresh memory may complete a narrative trope (e.g. the Avenger).
        NarrativeEngine.evaluate(holder, holder.level().getGameTime());
    }
}
