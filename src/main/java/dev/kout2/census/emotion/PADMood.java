package dev.kout2.census.emotion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Pleasure-Arousal-Dominance mood: a mob's slow-moving emotional baseline.
 *
 * Where {@link Emotion} intensities are acute and decay within seconds, mood is
 * an exponential moving average nudged by each emotion that fires, so a string
 * of bad experiences leaves a mob lastingly anxious or sullen. Each axis is
 * roughly in [-1,1].
 */
public record PADMood(float pleasure, float arousal, float dominance) {
    public static final PADMood NEUTRAL = new PADMood(0f, 0f, 0f);

    public static final Codec<PADMood> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.fieldOf("pleasure").forGetter(PADMood::pleasure),
            Codec.FLOAT.fieldOf("arousal").forGetter(PADMood::arousal),
            Codec.FLOAT.fieldOf("dominance").forGetter(PADMood::dominance)
    ).apply(inst, PADMood::new));

    public static final StreamCodec<ByteBuf, PADMood> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PADMood::pleasure,
            ByteBufCodecs.FLOAT, PADMood::arousal,
            ByteBufCodecs.FLOAT, PADMood::dominance,
            PADMood::new
    );

    /** Moves a fraction {@code alpha} of the way toward a target PAD point. */
    public PADMood lerp(float tp, float ta, float td, float alpha) {
        return new PADMood(
                pleasure + (tp - pleasure) * alpha,
                arousal + (ta - arousal) * alpha,
                dominance + (td - dominance) * alpha
        );
    }

    /** A short human label for the current octant of PAD space. */
    public String label() {
        boolean p = pleasure >= 0;
        boolean a = arousal >= 0;
        boolean d = dominance >= 0;
        if (p && a && d) return "exuberant";
        if (p && a) return "dependent";       // pleasant, aroused, submissive
        if (p && d) return "relaxed";
        if (p) return "docile";
        if (a && d) return "hostile";
        if (a) return "anxious";
        if (d) return "disdainful";
        return "bored";
    }
}
