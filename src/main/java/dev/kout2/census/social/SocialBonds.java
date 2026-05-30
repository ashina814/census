package dev.kout2.census.social;

import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.persona.generator.PersonaGenerator;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.ReputationBook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * The social layer: villagers who keep company grow fond of one another, fond
 * unpartnered adults pair off for life, and established couples occasionally
 * have a child — a "medieval" reproduction driven by relationships rather than
 * the vanilla bread-and-beds mechanic.
 *
 * Bonds reuse {@link ReputationBook}: affection between two villagers is just
 * positive opinion of each other, so it also feeds gossip, grief and revenge.
 */
public final class SocialBonds {
    /** Affection gained by each villager per friendly meeting. */
    private static final float BOND_PER_MEET = 2.5f;
    /** Mutual affection at which two unpartnered adults pair off. */
    public static final float COURTSHIP_BOND = 40f;
    /** Affection toward someone whose death is felt as a personal loss. */
    public static final float GRIEF_BOND = 25f;

    private static final double REPRODUCE_RADIUS = 6.0;
    private static final double CROWD_RADIUS = 32.0;
    private static final int LOCAL_POP_CAP = 16;
    private static final long CHILD_COOLDOWN = 24000L;   // ~1 in-game day
    private static final float REPRODUCE_CHANCE = 0.10f;

    private SocialBonds() {}

    /** Two villagers spend a moment together; each warms a little to the other. */
    public static void meet(Villager a, Villager b) {
        a.getData(ModAttachments.REPUTATION).adjust(b.getUUID(), BOND_PER_MEET);
        b.getData(ModAttachments.REPUTATION).adjust(a.getUUID(), BOND_PER_MEET);
        tryCourtship(a, b);
    }

    /** Fond, unpartnered adults pair off for life. */
    private static void tryCourtship(Villager a, Villager b) {
        if (a.isBaby() || b.isBaby()) {
            return;
        }
        Household ha = a.getData(ModAttachments.HOUSEHOLD);
        Household hb = b.getData(ModAttachments.HOUSEHOLD);
        if (ha.isPartnered() || hb.isPartnered()) {
            return;
        }
        float aToB = a.getData(ModAttachments.REPUTATION).opinionOf(b.getUUID());
        float bToA = b.getData(ModAttachments.REPUTATION).opinionOf(a.getUUID());
        if (aToB < COURTSHIP_BOND || bToA < COURTSHIP_BOND) {
            return;
        }
        if (!a.hasData(ModAttachments.PERSONA) || !b.hasData(ModAttachments.PERSONA)) {
            return;
        }
        ha.setPartner(b.getUUID(), b.getData(ModAttachments.PERSONA).id());
        hb.setPartner(a.getUUID(), a.getData(ModAttachments.PERSONA).id());
    }

    /**
     * Attempt to bear a child to {@code parent}'s couple. Heavily gated — a
     * nearby living partner, both adults, a long cooldown, a local population
     * cap and a low chance — so villages grow slowly and stay performant.
     */
    public static void tryReproduce(ServerLevel level, Villager parent, long now) {
        Household home = parent.getData(ModAttachments.HOUSEHOLD);
        Optional<UUID> partnerId = home.partner();
        if (partnerId.isEmpty() || parent.isBaby()) {
            return;
        }
        // Only the lower-UUID partner runs the attempt, so a couple tries once.
        if (parent.getUUID().compareTo(partnerId.get()) > 0) {
            return;
        }
        if (now - home.lastChildTick() < CHILD_COOLDOWN) {
            return;
        }
        if (parent.getRandom().nextFloat() > REPRODUCE_CHANCE) {
            return;
        }
        if (!(level.getEntity(partnerId.get()) instanceof Villager partner)
                || partner.isBaby()
                || partner.distanceToSqr(parent) > REPRODUCE_RADIUS * REPRODUCE_RADIUS) {
            return;
        }
        if (now - partner.getData(ModAttachments.HOUSEHOLD).lastChildTick() < CHILD_COOLDOWN) {
            return;
        }
        if (isCrowded(level, parent)) {
            return;
        }
        bearChild(level, parent, partner, now);
    }

    private static boolean isCrowded(ServerLevel level, Villager parent) {
        return level.getEntitiesOfClass(Villager.class, parent.getBoundingBox().inflate(CROWD_RADIUS))
                .size() >= LOCAL_POP_CAP;
    }

    private static void bearChild(ServerLevel level, Villager a, Villager b, long now) {
        Villager child = a.getBreedOffspring(level, b);
        if (child == null) {
            return;
        }
        Persona personaA = a.getData(ModAttachments.PERSONA);
        Persona personaB = b.getData(ModAttachments.PERSONA);
        int parentGen = Math.max(
                a.getData(ModAttachments.LINEAGE).generation(),
                b.getData(ModAttachments.LINEAGE).generation());

        Persona childPersona = PersonaGenerator.generateChild(personaA, personaB, child.getRandom(), now);
        // Set identity before the entity joins the world, so the join handler
        // keeps it instead of rolling a random founder persona.
        child.setData(ModAttachments.PERSONA, childPersona);
        child.setData(ModAttachments.LINEAGE, Lineage.child(personaA.id(), personaB.id(), parentGen));
        child.setAge(-24000);

        Vec3 mid = a.position().add(b.position()).scale(0.5);
        child.snapTo(mid.x, mid.y, mid.z, child.getYRot(), child.getXRot());
        level.addFreshEntityWithPassengers(child);

        a.getData(ModAttachments.HOUSEHOLD).recordChild(now);
        b.getData(ModAttachments.HOUSEHOLD).recordChild(now);
    }

    /** Bond strength {@code observer} feels toward {@code subjectId} (0 if none). */
    public static float bondToward(Entity observer, UUID subjectId) {
        return observer.getData(ModAttachments.REPUTATION).opinionOf(subjectId);
    }
}
