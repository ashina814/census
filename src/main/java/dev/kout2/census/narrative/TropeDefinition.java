package dev.kout2.census.narrative;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kout2.census.memory.EventType;

import java.util.List;
import java.util.Optional;

/**
 * The JSON shape of a narrative trope — what datapack authors write under
 * {@code data/<namespace>/census_tropes/*.json}. A trope fires once per mob
 * when its (optional) trait gate and any one trigger are satisfied; the
 * matched trigger names a culprit, and the effects are applied against them.
 *
 * Example ({@code data/census/census_tropes/avenger.json}):
 * <pre>{@code
 * {
 *   "required_trait": "vengeful",
 *   "triggers": [
 *     { "memory_event": "relative_killed", "max_opinion": -20.0 },
 *     { "max_opinion": -60.0 }
 *   ],
 *   "effects": { "set_avenge_target": true, "announce": "census.trope.avenger",
 *                "angry_particles": true }
 * }
 * }</pre>
 *
 * @param requiredTrait lowercase {@link dev.kout2.census.persona.DerivedTrait}
 *                      name the mob must currently have, if any
 * @param triggers      any one of these matching selects the culprit
 * @param effects       what happens to the mob/culprit when the trope fires
 */
public record TropeDefinition(
        Optional<String> requiredTrait,
        List<Trigger> triggers,
        Effects effects
) {
    /**
     * One way the trope can fire. With {@code memoryEvent}: the mob remembers
     * such an event and its opinion of that memory's subject is at most
     * {@code maxOpinion}. Without: the mob's most-hated acquaintance is at or
     * below {@code maxOpinion}.
     */
    public record Trigger(Optional<EventType> memoryEvent, float maxOpinion) {
        public static final Codec<Trigger> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                EventType.CODEC.optionalFieldOf("memory_event").forGetter(Trigger::memoryEvent),
                Codec.FLOAT.fieldOf("max_opinion").forGetter(Trigger::maxOpinion)
        ).apply(inst, Trigger::new));
    }

    /**
     * @param setAvengeTarget mark the culprit in NarrativeState (AvengeGoal hunts them)
     * @param announce        translation key broadcast to nearby players, with the
     *                        mob's full name as the single argument
     * @param angryParticles  burst angry-villager particles over the mob's head
     * @param addEmotion      lowercase {@link dev.kout2.census.emotion.Emotion}
     *                        name to surge in the mob, if any
     */
    public record Effects(boolean setAvengeTarget, Optional<String> announce,
                          boolean angryParticles, Optional<String> addEmotion) {
        public static final Codec<Effects> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.optionalFieldOf("set_avenge_target", false).forGetter(Effects::setAvengeTarget),
                Codec.STRING.optionalFieldOf("announce").forGetter(Effects::announce),
                Codec.BOOL.optionalFieldOf("angry_particles", false).forGetter(Effects::angryParticles),
                Codec.STRING.optionalFieldOf("add_emotion").forGetter(Effects::addEmotion)
        ).apply(inst, Effects::new));
    }

    public static final Codec<TropeDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("required_trait").forGetter(TropeDefinition::requiredTrait),
            Trigger.CODEC.listOf().fieldOf("triggers").forGetter(TropeDefinition::triggers),
            Effects.CODEC.fieldOf("effects").forGetter(TropeDefinition::effects)
    ).apply(inst, TropeDefinition::new));
}
