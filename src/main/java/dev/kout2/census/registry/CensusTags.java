package dev.kout2.census.registry;

import dev.kout2.census.CensusMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Which species get an inner life, decided by entity-type tags so datapacks
 * can extend the census without code. Villagers are always fully censused and
 * aren't tag-controlled.
 */
public final class CensusTags {
    /** Animals: a given name, a personality, memories — and utility behaviours. */
    public static final TagKey<EntityType<?>> CENSUSED_ANIMALS =
            TagKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(CensusMod.MODID, "censused_animals"));

    /** Hostiles: a full (once-human) name and inner state, no extra behaviours. */
    public static final TagKey<EntityType<?>> CENSUSED_HOSTILES =
            TagKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(CensusMod.MODID, "censused_hostiles"));

    private CensusTags() {}
}
