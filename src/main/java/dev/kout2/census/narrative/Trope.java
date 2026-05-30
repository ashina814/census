package dev.kout2.census.narrative;

import net.minecraft.world.entity.LivingEntity;

/**
 * A narrative trope: an authored story-pattern that fires when a mob's
 * procedurally-grown state (personality + memory + reputation + lineage) lines
 * up with it. The Wildermyth-style marriage of authored intent and emergent
 * detail — the writer says "an avenger hunts the one who wronged their blood",
 * the simulation decides who, when and why.
 */
public interface Trope {
    /** Stable id, also the persistence key for "already fired". */
    String id();

    /** True when this mob currently satisfies the trope's preconditions. */
    boolean matches(LivingEntity mob, long now);

    /** Enact the trope's consequence on the mob/world. Called once. */
    void fire(LivingEntity mob, long now);
}
