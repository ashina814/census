package dev.kout2.census.reflection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * One distilled insight.
 *
 * @param type     the kind of insight
 * @param subject  the being it concerns (present only for directed types)
 * @param strength 0..1 conviction
 * @param tick     when it was last reaffirmed
 */
public record Reflection(ReflectionType type, Optional<UUID> subject, float strength, long tick) {
    public static final Codec<Reflection> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ReflectionType.CODEC.fieldOf("type").forGetter(Reflection::type),
            UUIDUtil.CODEC.optionalFieldOf("subject").forGetter(Reflection::subject),
            Codec.FLOAT.fieldOf("strength").forGetter(Reflection::strength),
            Codec.LONG.fieldOf("tick").forGetter(Reflection::tick)
    ).apply(inst, Reflection::new));
}
