package dev.kout2.census.persona;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Set;
import java.util.UUID;

/**
 * The identity core attached to every censused mob.
 *
 * Phase 1 keeps this lean: identity + personality. Memories live in a separate
 * {@code MemoryStream} attachment (Phase 2); emotions in {@code EmotionalState}
 * (Phase 3); reputations and goals arrive with the dynasty/AI phases. Splitting
 * concerns keeps each attachment's serialization simple and independently
 * syncable.
 *
 * @param id          stable identity (independent of the entity UUID, survives
 *                    re-spawns / conversions in later phases)
 * @param givenName   personal name
 * @param familyName  inherited surname (shared across a lineage)
 * @param birthTick   game time of birth, for age calculations
 * @param gender      cosmetic, drives name pool only
 * @param personality the Big Five profile — the source of all derived behaviour
 */
public record Persona(
        UUID id,
        String givenName,
        String familyName,
        long birthTick,
        Gender gender,
        BigFive personality
) {
    /**
     * Map codec form — required by NeoForge's {@code AttachmentType.serialize},
     * which writes into the holder's existing data map.
     */
    public static final MapCodec<Persona> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Persona::id),
            Codec.STRING.fieldOf("givenName").forGetter(Persona::givenName),
            Codec.STRING.fieldOf("familyName").forGetter(Persona::familyName),
            Codec.LONG.fieldOf("birthTick").forGetter(Persona::birthTick),
            Gender.CODEC.fieldOf("gender").forGetter(Persona::gender),
            BigFive.CODEC.fieldOf("personality").forGetter(Persona::personality)
    ).apply(inst, Persona::new));

    public static final Codec<Persona> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, Persona> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Persona::id,
            ByteBufCodecs.STRING_UTF8, Persona::givenName,
            ByteBufCodecs.STRING_UTF8, Persona::familyName,
            ByteBufCodecs.VAR_LONG, Persona::birthTick,
            Gender.STREAM_CODEC, Persona::gender,
            BigFive.STREAM_CODEC, Persona::personality,
            Persona::new
    );

    /** Placeholder returned by the attachment's default supplier; never persisted. */
    public static final Persona UNKNOWN = new Persona(
            new UUID(0L, 0L), "", "", 0L, Gender.MALE,
            new BigFive(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    );

    public String fullName() {
        // Animals carry only a given name (empty family name).
        return familyName.isEmpty() ? givenName : givenName + " " + familyName;
    }

    public Set<DerivedTrait> traits() {
        return DerivedTrait.of(personality);
    }

    /** Age in whole in-game days, given the current game time. */
    public long ageInDays(long currentTick) {
        return Math.max(0L, (currentTick - birthTick) / 24000L);
    }
}
