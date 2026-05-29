package dev.kout2.census.memory;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Kinds of remembered events. Each carries a base importance (0–10, before
 * personality modulation) and a base valence (emotional sign, -1..+1).
 *
 * Phase 2 wires FED, HARMED and WITNESSED_DEATH; the rest are reserved for
 * later phases (gifting in the ambition system, healing, family death in the
 * dynasty system).
 */
public enum EventType implements StringRepresentable {
    FED("fed", 2.0f, 0.6f),
    GIFTED("gifted", 4.0f, 0.85f),
    HEALED("healed", 3.0f, 0.7f),
    HARMED("harmed", 6.0f, -0.7f),
    WITNESSED_DEATH("witnessed_death", 7.0f, -0.8f),
    RELATIVE_KILLED("relative_killed", 9.0f, -0.95f);

    public static final Codec<EventType> CODEC = StringRepresentable.fromEnum(EventType::values);

    private final String name;
    private final float baseImportance;
    private final float baseValence;

    EventType(String name, float baseImportance, float baseValence) {
        this.name = name;
        this.baseImportance = baseImportance;
        this.baseValence = baseValence;
    }

    public float baseImportance() {
        return baseImportance;
    }

    public float baseValence() {
        return baseValence;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
