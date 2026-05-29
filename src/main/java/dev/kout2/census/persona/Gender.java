package dev.kout2.census.persona;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Used only to pick a name pool. Vanilla villagers are genderless; this is a
 * Census-internal cosmetic for name generation and family roles.
 */
public enum Gender implements StringRepresentable {
    MALE("male"),
    FEMALE("female");

    public static final Codec<Gender> CODEC = StringRepresentable.fromEnum(Gender::values);
    public static final StreamCodec<ByteBuf, Gender> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> values()[i], Gender::ordinal);

    private final String name;

    Gender(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
