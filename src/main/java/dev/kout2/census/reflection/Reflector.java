package dev.kout2.census.reflection;

import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.memory.MemoryEntry;
import dev.kout2.census.memory.MemoryStream;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.ReputationBook;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Distils a mob's raw memories and standing opinions into a few lasting
 * {@link Reflection}s, and lets those insights drift its long-term mood.
 *
 * Runs periodically per mob (gated by {@link #INTERVAL}), driven from the
 * social scheduler so the cost is spread and bounded.
 */
public final class Reflector {
    /** Minimum ticks between reflections for one mob (~5 in-game minutes). */
    public static final int INTERVAL = 6000;

    private static final float RESENT_THRESHOLD = -40f;
    private static final float TRUST_THRESHOLD = 40f;
    private static final int TRAUMA_COUNT = 3;
    private static final long GRIEF_WINDOW = 72000L; // ~3 days
    private static final int MAX_INSIGHTS = 4;
    private static final float MOOD_ALPHA = 0.15f;

    private Reflector() {}

    /** Whether {@code mob} is due to reflect again. */
    public static boolean isDue(LivingEntity mob, long now) {
        return now - mob.getData(ModAttachments.REFLECTIONS).lastReflectTick() >= INTERVAL;
    }

    public static void reflect(LivingEntity mob, long now) {
        ReputationBook reputation = mob.getData(ModAttachments.REPUTATION);
        MemoryStream memory = mob.getData(ModAttachments.MEMORY);

        List<Reflection> insights = new ArrayList<>();

        // Subject-directed: lasting like or dislike of specific beings.
        for (Map.Entry<java.util.UUID, Float> opinion : reputation.entries()) {
            float v = opinion.getValue();
            if (v <= RESENT_THRESHOLD) {
                insights.add(new Reflection(ReflectionType.RESENTS,
                        Optional.of(opinion.getKey()), Math.min(1f, -v / 100f), now));
            } else if (v >= TRUST_THRESHOLD) {
                insights.add(new Reflection(ReflectionType.TRUSTS,
                        Optional.of(opinion.getKey()), Math.min(1f, v / 100f), now));
            }
        }

        // Self-states from the shape of recent memory.
        int heavyNegatives = 0;
        boolean recentLoss = false;
        for (MemoryEntry e : memory.recent(40)) {
            if (e.valence() < 0 && e.importance() > 5f) {
                heavyNegatives++;
            }
            if ((e.type() == EventType.RELATIVE_KILLED || e.type() == EventType.WITNESSED_DEATH)
                    && now - e.tick() < GRIEF_WINDOW) {
                recentLoss = true;
            }
        }
        if (recentLoss) {
            insights.add(new Reflection(ReflectionType.GRIEVING, Optional.empty(), 1f, now));
        }
        if (heavyNegatives >= TRAUMA_COUNT) {
            insights.add(new Reflection(ReflectionType.TRAUMATIZED, Optional.empty(),
                    Math.min(1f, heavyNegatives / 5f), now));
        }
        if (insights.isEmpty()) {
            insights.add(new Reflection(ReflectionType.CONTENT, Optional.empty(), 0.5f, now));
        }

        // Keep only the strongest few.
        insights.sort(Comparator.comparingDouble(Reflection::strength).reversed());
        if (insights.size() > MAX_INSIGHTS) {
            insights = new ArrayList<>(insights.subList(0, MAX_INSIGHTS));
        }
        mob.getData(ModAttachments.REFLECTIONS).replace(insights, now);

        applyMoodDrift(mob, now, recentLoss, heavyNegatives >= TRAUMA_COUNT);
    }

    /** Lasting insights gently colour the mob's baseline mood. */
    private static void applyMoodDrift(LivingEntity mob, long now, boolean grieving, boolean traumatized) {
        EmotionalState state = mob.getData(ModAttachments.EMOTION);
        state.decayTo(now);
        if (traumatized) {
            state.pullMood(-0.3f, 0.3f, -0.3f, MOOD_ALPHA);   // anxious, on edge
        } else if (grieving) {
            state.pullMood(-0.3f, -0.1f, -0.2f, MOOD_ALPHA);  // sorrowful, withdrawn
        } else {
            state.pullMood(0.15f, 0f, 0.1f, MOOD_ALPHA * 0.5f); // slow return to contentment
        }
        mob.setData(ModAttachments.EMOTION, state);
    }
}
