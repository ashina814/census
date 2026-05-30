package dev.kout2.census.social;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * A villager's domestic situation: who they are partnered with (a lifelong
 * pairing formed from mutual affection, not a fed-bread fling) and when they
 * last had a child.
 *
 * Partner is stored both as an entity UUID (to locate the living partner) and a
 * persona id (stable, for a child's lineage even after a partner dies).
 */
public final class Household {
    private Optional<UUID> partner;
    private Optional<UUID> partnerPersona;
    private long lastChildTick;

    public Household() {
        this.partner = Optional.empty();
        this.partnerPersona = Optional.empty();
        this.lastChildTick = Long.MIN_VALUE;
    }

    private Household(Optional<UUID> partner, Optional<UUID> partnerPersona, long lastChildTick) {
        this.partner = partner;
        this.partnerPersona = partnerPersona;
        this.lastChildTick = lastChildTick;
    }

    public static final MapCodec<Household> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            UUIDUtil.CODEC.optionalFieldOf("partner").forGetter(h -> h.partner),
            UUIDUtil.CODEC.optionalFieldOf("partnerPersona").forGetter(h -> h.partnerPersona),
            Codec.LONG.optionalFieldOf("lastChildTick", Long.MIN_VALUE).forGetter(h -> h.lastChildTick)
    ).apply(inst, Household::new));

    public boolean isPartnered() {
        return partner.isPresent();
    }

    public Optional<UUID> partner() {
        return partner;
    }

    public Optional<UUID> partnerPersona() {
        return partnerPersona;
    }

    public void setPartner(UUID entityId, UUID personaId) {
        this.partner = Optional.of(entityId);
        this.partnerPersona = Optional.of(personaId);
    }

    public long lastChildTick() {
        return lastChildTick;
    }

    public void recordChild(long tick) {
        this.lastChildTick = tick;
    }
}
