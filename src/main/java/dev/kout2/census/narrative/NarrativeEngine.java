package dev.kout2.census.narrative;

import dev.kout2.census.narrative.tropes.AvengerTrope;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Evaluates registered {@link Trope}s against a mob and fires any whose
 * conditions are newly met. Driven reactively from {@code Census.observe} — it
 * runs right after the event that might have triggered a story, not on a timer.
 */
public final class NarrativeEngine {
    private static final List<Trope> TROPES = List.of(
            new AvengerTrope()
    );

    private NarrativeEngine() {}

    public static void evaluate(LivingEntity mob, long now) {
        NarrativeState state = mob.getData(ModAttachments.NARRATIVE);
        for (Trope trope : TROPES) {
            if (!state.hasFired(trope.id()) && trope.matches(mob, now)) {
                trope.fire(mob, now);
                state.markFired(trope.id());
            }
        }
    }
}
