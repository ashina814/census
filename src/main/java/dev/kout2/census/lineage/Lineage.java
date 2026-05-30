package dev.kout2.census.lineage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * A mob's place in a family tree: who its parents were (by persona id) and how
 * many generations deep the line runs.
 *
 * Parent ids are persona ids, not entity UUIDs, so a lineage survives even
 * after the parents die and their entities are gone — which is the whole point:
 * a child can still grieve and avenge a parent it can no longer see.
 *
 * @param parentA    first parent's persona id, empty for founders
 * @param parentB    second parent's persona id, empty for founders/single
 * @param generation 0 for founders, otherwise max(parents)+1
 */
public record Lineage(Optional<UUID> parentA, Optional<UUID> parentB, int generation) {
    public static final Lineage FOUNDER = new Lineage(Optional.empty(), Optional.empty(), 0);

    public static final MapCodec<Lineage> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            UUIDUtil.CODEC.optionalFieldOf("parentA").forGetter(Lineage::parentA),
            UUIDUtil.CODEC.optionalFieldOf("parentB").forGetter(Lineage::parentB),
            Codec.INT.optionalFieldOf("generation", 0).forGetter(Lineage::generation)
    ).apply(inst, Lineage::new));

    public static Lineage child(UUID parentAId, UUID parentBId, int parentGeneration) {
        return new Lineage(Optional.of(parentAId), Optional.of(parentBId), parentGeneration + 1);
    }

    public boolean hasParents() {
        return parentA.isPresent() || parentB.isPresent();
    }

    public boolean isChildOf(UUID personaId) {
        return parentA.filter(personaId::equals).isPresent()
                || parentB.filter(personaId::equals).isPresent();
    }
}
