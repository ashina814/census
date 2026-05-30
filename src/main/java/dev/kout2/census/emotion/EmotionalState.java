package dev.kout2.census.emotion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * A mob's live emotional state: eight acute {@link Emotion} intensities plus a
 * slow {@link PADMood} baseline.
 *
 * Decay is lazy — rather than ticking every mob every tick, intensities are
 * faded by {@code DECAY^elapsed} whenever the state is touched. This keeps the
 * cost proportional to how often a mob actually experiences or is inspected,
 * not to how many mobs exist.
 */
public final class EmotionalState {
    /** Per-tick multiplicative decay of acute emotion intensities. */
    private static final float DECAY_PER_TICK = 0.98f;
    /** Cap on the exponent so a long absence just floors intensities at ~0 cheaply. */
    private static final long MAX_DECAY_TICKS = 2000L;
    /** How strongly each fired emotion drags the mood toward its PAD vector. */
    private static final float MOOD_PULL = 0.10f;

    private final float[] intensities;
    private PADMood mood;
    private long lastDecayTick;

    public EmotionalState() {
        this.intensities = new float[Emotion.COUNT];
        this.mood = PADMood.NEUTRAL;
        this.lastDecayTick = 0L;
    }

    private EmotionalState(float[] intensities, PADMood mood, long lastDecayTick) {
        this.intensities = intensities.length == Emotion.COUNT
                ? intensities : new float[Emotion.COUNT];
        this.mood = mood;
        this.lastDecayTick = lastDecayTick;
    }

    /** Applies elapsed-time decay up to {@code now}. Idempotent within a tick. */
    public void decayTo(long now) {
        long elapsed = now - lastDecayTick;
        if (elapsed <= 0) {
            return;
        }
        float factor = (float) Math.pow(DECAY_PER_TICK, Math.min(elapsed, MAX_DECAY_TICKS));
        for (int i = 0; i < intensities.length; i++) {
            intensities[i] *= factor;
        }
        lastDecayTick = now;
    }

    /** Adds intensity to one emotion (clamped 0..1) and nudges the mood. */
    public void add(Emotion emotion, float amount, long now) {
        decayTo(now);
        int i = emotion.ordinal();
        intensities[i] = Math.clamp(intensities[i] + amount, 0f, 1f);
        float pull = Math.min(MOOD_PULL, amount * 0.3f);
        mood = mood.lerp(emotion.pleasure(), emotion.arousal(), emotion.dominance(), pull);
    }

    public float intensity(Emotion emotion) {
        return intensities[emotion.ordinal()];
    }

    public PADMood mood() {
        return mood;
    }

    /** The strongest current emotion, or {@code null} if all are negligible. */
    public Emotion dominant() {
        Emotion best = null;
        float bestVal = 0.05f; // negligibility threshold
        for (Emotion e : Emotion.values()) {
            float v = intensities[e.ordinal()];
            if (v > bestVal) {
                bestVal = v;
                best = e;
            }
        }
        return best;
    }

    // ---- serialization -----------------------------------------------------

    private static List<Float> toList(float[] a) {
        List<Float> list = new ArrayList<>(a.length);
        for (float v : a) {
            list.add(v);
        }
        return list;
    }

    private static float[] toArray(List<Float> list) {
        float[] a = new float[Emotion.COUNT];
        for (int i = 0; i < Math.min(a.length, list.size()); i++) {
            a[i] = list.get(i);
        }
        return a;
    }

    public static final MapCodec<EmotionalState> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.listOf().optionalFieldOf("intensities", List.of())
                    .forGetter(st -> toList(st.intensities)),
            PADMood.CODEC.optionalFieldOf("mood", PADMood.NEUTRAL).forGetter(st -> st.mood),
            Codec.LONG.optionalFieldOf("lastDecayTick", 0L).forGetter(st -> st.lastDecayTick)
    ).apply(inst, (list, mood, tick) -> new EmotionalState(toArray(list), mood, tick)));

    public static final StreamCodec<RegistryFriendlyByteBuf, EmotionalState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, st) -> {
                        for (float v : st.intensities) {
                            buf.writeFloat(v);
                        }
                        PADMood.STREAM_CODEC.encode(buf, st.mood);
                        buf.writeVarLong(st.lastDecayTick);
                    },
                    buf -> {
                        float[] a = new float[Emotion.COUNT];
                        for (int i = 0; i < a.length; i++) {
                            a[i] = buf.readFloat();
                        }
                        PADMood mood = PADMood.STREAM_CODEC.decode(buf);
                        long tick = buf.readVarLong();
                        return new EmotionalState(a, mood, tick);
                    });
}
