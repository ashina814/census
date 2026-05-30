package dev.kout2.census.reflection;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Kinds of distilled insight a mob forms from its memories — the "reflection"
 * layer of the Generative-Agents memory model. Subject-directed insights
 * (RESENTS, TRUSTS) name another being; self-states (GRIEVING, TRAUMATIZED,
 * CONTENT) describe the mob's standing disposition.
 *
 * Reflections outlive the raw events that formed them and colour mood and
 * narrative, giving a mob a consistent "where it stands in life".
 */
public enum ReflectionType implements StringRepresentable {
    RESENTS("resents", true),
    TRUSTS("trusts", true),
    GRIEVING("grieving", false),
    TRAUMATIZED("traumatized", false),
    CONTENT("content", false);

    public static final Codec<ReflectionType> CODEC = StringRepresentable.fromEnum(ReflectionType::values);

    private final String name;
    private final boolean directed;

    ReflectionType(String name, boolean directed) {
        this.name = name;
        this.directed = directed;
    }

    /** True if this insight is about someone else (carries a subject). */
    public boolean isDirected() {
        return directed;
    }

    public String lowerName() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
