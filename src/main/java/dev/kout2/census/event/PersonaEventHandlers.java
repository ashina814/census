package dev.kout2.census.event;

import dev.kout2.census.CensusMod;
import dev.kout2.census.ai.FleeThreatGoal;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.persona.generator.PersonaGenerator;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Assigns personas and lineage as villagers enter the world.
 *
 * Villager breeding fires no dedicated event — the baby simply joins the level
 * via {@code addFreshEntity}. But at that moment {@code VillagerMakeLove} has
 * already snapped the baby onto parent A's position and aged both parents back
 * to adults, so the two nearest censused adults are reliably the parents. A
 * baby therefore <i>inherits</i> (surname + blended Big Five) instead of rolling
 * a fresh founder persona.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class PersonaEventHandlers {
    private static final double PARENT_SEARCH_RADIUS = 4.0;

    private PersonaEventHandlers() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        // Census goals live on the (non-persisted) goalSelector, so they must be
        // (re)installed on every join — including villagers loaded from disk.
        installGoals(villager);

        // Already censused (e.g. loaded from disk) — attachments deserialize
        // before the entity joins, so this guard preserves existing identities.
        if (villager.hasData(ModAttachments.PERSONA)) {
            return;
        }

        long now = villager.level().getGameTime();
        RandomSource random = villager.getRandom();

        List<Villager> parents = villager.isBaby() ? findParents(villager) : List.of();
        Persona persona;
        Lineage lineage;
        if (parents.size() >= 2) {
            Persona a = parents.get(0).getData(ModAttachments.PERSONA);
            Persona b = parents.get(1).getData(ModAttachments.PERSONA);
            int parentGen = Math.max(
                    parents.get(0).getData(ModAttachments.LINEAGE).generation(),
                    parents.get(1).getData(ModAttachments.LINEAGE).generation());
            persona = PersonaGenerator.generateChild(a, b, random, now);
            lineage = Lineage.child(a.id(), b.id(), parentGen);
        } else {
            persona = PersonaGenerator.generate(random, now);
            lineage = Lineage.FOUNDER;
        }

        villager.setData(ModAttachments.PERSONA, persona);
        villager.setData(ModAttachments.LINEAGE, lineage);
        applyNameTag(villager, persona);
    }

    /** Installs Census behaviours onto the villager's goal selector (idempotent per join). */
    private static void installGoals(Villager villager) {
        villager.goalSelector.addGoal(1, new FleeThreatGoal(villager));
    }

    /** The (up to two) nearest censused adult villagers — the presumed parents. */
    private static List<Villager> findParents(Villager baby) {
        AABB box = baby.getBoundingBox().inflate(PARENT_SEARCH_RADIUS);
        return baby.level().getEntitiesOfClass(Villager.class, box,
                        v -> v != baby && !v.isBaby() && v.hasData(ModAttachments.PERSONA)).stream()
                .sorted(Comparator.comparingDouble(v -> v.distanceToSqr(baby)))
                .limit(2)
                .toList();
    }

    /** Shows the persona's full name above the villager via the vanilla name tag. */
    private static void applyNameTag(Villager villager, Persona persona) {
        villager.setCustomName(Component.literal(persona.fullName()));
        villager.setCustomNameVisible(true);
    }
}
