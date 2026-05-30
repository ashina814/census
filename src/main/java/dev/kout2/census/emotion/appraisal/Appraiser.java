package dev.kout2.census.emotion.appraisal;

import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.persona.BigFive;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Appraisal: maps an event to emotional responses, in the OCC tradition.
 *
 * Each event type triggers a fixed set of base emotions (its "appraisal"),
 * whose intensities are then modulated by the mob's Big Five personality — so a
 * disagreeable, neurotic villager flares into ANGER where an agreeable, stable
 * one mostly feels DISTRESS at the same blow.
 */
public final class Appraiser {
    private Appraiser() {}

    private record Reaction(Emotion emotion, float base) {}

    private static List<Reaction> reactionsFor(EventType type) {
        return switch (type) {
            case FED -> List.of(new Reaction(Emotion.JOY, 0.30f), new Reaction(Emotion.GRATITUDE, 0.40f));
            case GIFTED -> List.of(new Reaction(Emotion.JOY, 0.50f), new Reaction(Emotion.GRATITUDE, 0.70f));
            case HEALED -> List.of(new Reaction(Emotion.JOY, 0.40f), new Reaction(Emotion.GRATITUDE, 0.50f));
            case HARMED -> List.of(new Reaction(Emotion.DISTRESS, 0.50f),
                    new Reaction(Emotion.ANGER, 0.50f), new Reaction(Emotion.FEAR, 0.30f));
            case WITNESSED_DEATH -> List.of(new Reaction(Emotion.DISTRESS, 0.50f),
                    new Reaction(Emotion.FEAR, 0.50f));
            case RELATIVE_KILLED -> List.of(new Reaction(Emotion.DISTRESS, 0.80f),
                    new Reaction(Emotion.ANGER, 0.80f));
        };
    }

    /** Applies the appraisal of {@code type} to {@code holder}'s emotional state. */
    public static void appraise(LivingEntity holder, Persona persona, EventType type) {
        EmotionalState state = holder.getData(ModAttachments.EMOTION);
        long now = holder.level().getGameTime();
        BigFive b = persona.personality();
        for (Reaction r : reactionsFor(type)) {
            float amount = r.base() * modulation(r.emotion(), b);
            if (amount > 0f) {
                state.add(r.emotion(), amount, now);
            }
        }
        // Re-set to trigger client sync (EmotionalState is a synced attachment).
        holder.setData(ModAttachments.EMOTION, state);
    }

    /**
     * Personality multiplier for how strongly a given emotion is felt.
     * Grounded in the usual Big Five correlates: extraverts feel more positive
     * affect; neurotics more fear/distress; the disagreeable anger more easily.
     */
    private static float modulation(Emotion emotion, BigFive b) {
        float m = switch (emotion) {
            case JOY -> 1f + b.extraversion() * 0.3f - b.neuroticism() * 0.3f;
            case GRATITUDE -> 1f + b.agreeableness() * 0.4f;
            case PRIDE -> 1f + b.extraversion() * 0.2f;
            case HOPE -> 1f + b.openness() * 0.2f - b.neuroticism() * 0.2f;
            case ANGER -> 1f - b.agreeableness() * 0.5f + b.neuroticism() * 0.3f;
            case FEAR -> 1f + b.neuroticism() * 0.5f;
            case DISTRESS -> 1f + b.neuroticism() * 0.4f;
            case SHAME -> 1f + b.neuroticism() * 0.3f;
        };
        return Math.max(0f, m);
    }
}
