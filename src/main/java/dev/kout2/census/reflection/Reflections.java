package dev.kout2.census.reflection;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A mob's current set of {@link Reflection}s plus when it last reflected.
 * Server-only: behaviour, mood and the census book read it; the HUD does not.
 */
public final class Reflections {
    private List<Reflection> insights;
    private long lastReflectTick;

    public Reflections() {
        this.insights = new ArrayList<>();
        this.lastReflectTick = Long.MIN_VALUE;
    }

    private Reflections(List<Reflection> insights, long lastReflectTick) {
        this.insights = new ArrayList<>(insights);
        this.lastReflectTick = lastReflectTick;
    }

    public static final MapCodec<Reflections> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Reflection.CODEC.listOf().optionalFieldOf("insights", List.of())
                    .forGetter(r -> r.insights),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("lastReflectTick", Long.MIN_VALUE)
                    .forGetter(r -> r.lastReflectTick)
    ).apply(inst, Reflections::new));

    public void replace(List<Reflection> next, long now) {
        this.insights = new ArrayList<>(next);
        this.lastReflectTick = now;
    }

    public long lastReflectTick() {
        return lastReflectTick;
    }

    public List<Reflection> all() {
        return insights;
    }

    /** The strongest current insight, or {@code null} if the mob holds none. */
    public Reflection dominant() {
        return insights.stream().max(Comparator.comparingDouble(Reflection::strength)).orElse(null);
    }
}
