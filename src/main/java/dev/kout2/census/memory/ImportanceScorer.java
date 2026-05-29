package dev.kout2.census.memory;

import dev.kout2.census.persona.BigFive;

/**
 * Scores how strongly an event sticks in memory.
 *
 * Follows the design formula {@code base × |valence| × (1 + neuroticism·0.5)}:
 * anxious (high-neuroticism) personalities encode events more intensely, the
 * same way they do for humans. Result is clamped to [0,10].
 */
public final class ImportanceScorer {
    private ImportanceScorer() {}

    public static float score(EventType type, BigFive personality) {
        float base = type.baseImportance();
        float valenceWeight = Math.abs(type.baseValence());
        float neuroticismBoost = 1.0f + personality.neuroticism() * 0.5f;
        return Math.clamp(base * valenceWeight * neuroticismBoost, 0.0f, 10.0f);
    }
}
