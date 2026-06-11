package dev.kout2.census.persona.generator;

import dev.kout2.census.persona.BigFive;
import dev.kout2.census.persona.Gender;
import dev.kout2.census.persona.Persona;
import net.minecraft.util.RandomSource;

import java.util.UUID;

/**
 * Builds fresh {@link Persona}s. Phase 4 will add a parent-aware overload that
 * inherits surname and personality via {@link BigFive#inherit}.
 */
public final class PersonaGenerator {
    private PersonaGenerator() {}

    /** A wholly new, unrelated persona (first-generation villagers). */
    public static Persona generate(RandomSource random, long currentTick) {
        Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
        BigFive personality = BigFive.random(random);
        return new Persona(
                UUID.randomUUID(),
                NameGenerator.givenName(random, gender),
                NameGenerator.familyName(random),
                currentTick,
                gender,
                personality
        );
    }

    /** An animal: a given name only, no family line. */
    public static Persona generateAnimal(RandomSource random, long currentTick) {
        Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
        return new Persona(
                UUID.randomUUID(),
                NameGenerator.givenName(random, gender),
                "",
                currentTick,
                gender,
                BigFive.random(random)
        );
    }

    /** A child of two known personas: inherits the father's surname (Phase 4 stub). */
    public static Persona generateChild(Persona parentA, Persona parentB,
                                        RandomSource random, long currentTick) {
        Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
        BigFive personality = BigFive.inherit(parentA.personality(), parentB.personality(), random);
        return new Persona(
                UUID.randomUUID(),
                NameGenerator.givenName(random, gender),
                parentA.familyName(),
                currentTick,
                gender,
                personality
        );
    }
}
