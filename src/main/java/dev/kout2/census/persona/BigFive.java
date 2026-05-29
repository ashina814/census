package dev.kout2.census.persona;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

/**
 * The Big Five (OCEAN) personality model — the backbone of every Persona.
 *
 * Each dimension is a float in [0,1]. Reference: Costa &amp; McCrae (NEO-PI).
 * These five numbers modulate emotion appraisal (Phase 3), memory importance
 * (Phase 2), and utility-based action selection (Phase 6).
 *
 * @param openness          curiosity, preference for novelty
 * @param conscientiousness diligence, planning, impulse control
 * @param extraversion      sociability, assertiveness
 * @param agreeableness     compassion, cooperation, trust
 * @param neuroticism       emotional instability, anxiety
 */
public record BigFive(
        float openness,
        float conscientiousness,
        float extraversion,
        float agreeableness,
        float neuroticism
) {
    public static final Codec<BigFive> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("openness").forGetter(BigFive::openness),
            Codec.FLOAT.fieldOf("conscientiousness").forGetter(BigFive::conscientiousness),
            Codec.FLOAT.fieldOf("extraversion").forGetter(BigFive::extraversion),
            Codec.FLOAT.fieldOf("agreeableness").forGetter(BigFive::agreeableness),
            Codec.FLOAT.fieldOf("neuroticism").forGetter(BigFive::neuroticism)
    ).apply(inst, BigFive::new));

    public static final StreamCodec<ByteBuf, BigFive> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BigFive::openness,
            ByteBufCodecs.FLOAT, BigFive::conscientiousness,
            ByteBufCodecs.FLOAT, BigFive::extraversion,
            ByteBufCodecs.FLOAT, BigFive::agreeableness,
            ByteBufCodecs.FLOAT, BigFive::neuroticism,
            BigFive::new
    );

    /**
     * Draws a trait value centred on 0.5 with a roughly bell-shaped spread,
     * approximating a Beta(2,2) distribution via the mean of two uniforms.
     * This keeps most villagers "average" while allowing rare extremes.
     */
    private static float roll(RandomSource random) {
        return (random.nextFloat() + random.nextFloat()) * 0.5f;
    }

    /** Generates a fresh, unrelated personality. */
    public static BigFive random(RandomSource random) {
        return new BigFive(roll(random), roll(random), roll(random), roll(random), roll(random));
    }

    /**
     * Inherits from two parents: 40% of each parent's average, plus Gaussian
     * noise, clamped to [0,1]. Children resemble parents but drift over
     * generations (Phase 4 dynasty system).
     */
    public static BigFive inherit(BigFive a, BigFive b, RandomSource random) {
        return new BigFive(
                mix(a.openness, b.openness, random),
                mix(a.conscientiousness, b.conscientiousness, random),
                mix(a.extraversion, b.extraversion, random),
                mix(a.agreeableness, b.agreeableness, random),
                mix(a.neuroticism, b.neuroticism, random)
        );
    }

    private static float mix(float a, float b, RandomSource random) {
        float parentAvg = (a + b) * 0.5f;
        float noise = (float) random.nextGaussian() * 0.15f;
        // 40% heritability with regression toward the population mean (0.5).
        float value = 0.5f + 0.4f * (parentAvg - 0.5f) + noise;
        return Math.clamp(value, 0.0f, 1.0f);
    }
}
