package dev.kout2.census.persona;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Human-readable traits derived on demand from a {@link BigFive} profile.
 *
 * Not stored — always recomputed — so the underlying personality stays the
 * single source of truth. Narrative tropes (Phase 7) match on these names
 * (e.g. {@code the_avenger} requires {@link #VENGEFUL}).
 */
public enum DerivedTrait {
    BRAVE(b -> b.neuroticism() < 0.35f),
    COWARDLY(b -> b.neuroticism() > 0.70f),
    VENGEFUL(b -> b.agreeableness() < 0.30f && b.neuroticism() > 0.50f),
    FORGIVING(b -> b.agreeableness() > 0.70f && b.neuroticism() < 0.40f),
    CURIOUS(b -> b.openness() > 0.65f),
    INCURIOUS(b -> b.openness() < 0.30f),
    DILIGENT(b -> b.conscientiousness() > 0.65f),
    LAZY(b -> b.conscientiousness() < 0.30f),
    SOCIABLE(b -> b.extraversion() > 0.65f),
    RECLUSIVE(b -> b.extraversion() < 0.30f),
    KIND(b -> b.agreeableness() > 0.70f),
    CALLOUS(b -> b.agreeableness() < 0.25f);

    private final Predicate<BigFive> condition;

    DerivedTrait(Predicate<BigFive> condition) {
        this.condition = condition;
    }

    public boolean matches(BigFive personality) {
        return condition.test(personality);
    }

    /** Computes the full trait set for a personality. */
    public static Set<DerivedTrait> of(BigFive personality) {
        EnumSet<DerivedTrait> result = EnumSet.noneOf(DerivedTrait.class);
        for (DerivedTrait trait : values()) {
            if (trait.matches(personality)) {
                result.add(trait);
            }
        }
        return result;
    }
}
