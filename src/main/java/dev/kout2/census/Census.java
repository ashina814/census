package dev.kout2.census;

import dev.kout2.census.emotion.appraisal.Appraiser;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.memory.Memories;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

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
    private Census() {}

    public static void observe(LivingEntity holder, EventType type, @Nullable java.util.UUID subject) {
        if (!holder.hasData(ModAttachments.PERSONA)) {
            return;
        }
        Persona persona = holder.getData(ModAttachments.PERSONA);
        Memories.record(holder, persona, type, subject);
        Appraiser.appraise(holder, persona, type);
    }
}
