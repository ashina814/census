package dev.kout2.census.event;

import dev.kout2.census.Census;
import dev.kout2.census.CensusMod;
import dev.kout2.census.ai.AvengeGoal;
import dev.kout2.census.ai.utility.UtilityGoal;
import dev.kout2.census.census.CensusRegistry;
import dev.kout2.census.config.CensusConfig;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.persona.generator.PersonaGenerator;
import dev.kout2.census.registry.CensusTags;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Hands out personas as entities enter the world, in three tiers:
 *
 * <ul>
 * <li><b>Villagers</b> — the full treatment: surname, lineage inheritance from
 *     nearby parents at birth, world-registry record, social/utility goals.</li>
 * <li><b>Animals</b> ({@code #census:censused_animals}) — a given name and an
 *     inner life, plus utility behaviours: a dog can come to fear you, or love
 *     you enough to bring you things.</li>
 * <li><b>Hostiles</b> ({@code #census:censused_hostiles}) — a full, once-human
 *     name and inner state, no behaviour changes. Their name tag stays hidden;
 *     you only learn who you're fighting by looking (HUD) or the census book.</li>
 * </ul>
 *
 * The tag tiers are datapack-extensible, like tropes and actions.
 *
 * On villager breeding: vanilla fires no event — the baby simply joins via
 * {@code addFreshEntity}, but {@code VillagerMakeLove} has already snapped it
 * onto parent A and re-aged both parents, so the two nearest censused adults
 * are reliably the parents and the child inherits.
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
        if (event.getEntity() instanceof Villager villager) {
            joinVillager(villager);
        } else if (event.getEntity() instanceof PathfinderMob mob) {
            if (mob.getType().is(CensusTags.CENSUSED_ANIMALS)) {
                joinAnimal(mob);
            } else if (mob.getType().is(CensusTags.CENSUSED_HOSTILES)) {
                joinHostile(mob);
            }
        }
    }

    private static void joinVillager(Villager villager) {
        // Census goals live on the (non-persisted) goalSelector, so they must be
        // (re)installed on every join — including villagers loaded from disk.
        // Avenge (0) outranks utility behaviours (1): a sworn avenger advances
        // where the utility selector would have it flee.
        villager.goalSelector.addGoal(0, new AvengeGoal(villager));
        villager.goalSelector.addGoal(1, new UtilityGoal(villager));

        // Already censused (e.g. loaded from disk) — attachments deserialize
        // before the entity joins, so this guard preserves existing identities.
        if (Census.isCensused(villager)) {
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
        applyNameTag(villager, persona, CensusConfig.NAME_TAGS_ALWAYS_VISIBLE.get());

        // Record the new life in the world registry (for cross-world family/revenge).
        MinecraftServer server = villager.level().getServer();
        if (server != null) {
            CensusRegistry.get(server).register(persona, lineage);
        }
    }

    private static void joinAnimal(PathfinderMob mob) {
        mob.goalSelector.addGoal(1, new UtilityGoal(mob));
        if (Census.isCensused(mob)) {
            return;
        }
        Persona persona = PersonaGenerator.generateAnimal(
                mob.getRandom(), mob.level().getGameTime());
        mob.setData(ModAttachments.PERSONA, persona);
        applyNameTag(mob, persona, CensusConfig.NAME_TAGS_ALWAYS_VISIBLE.get());
    }

    private static void joinHostile(PathfinderMob mob) {
        if (Census.isCensused(mob)) {
            return;
        }
        Persona persona = PersonaGenerator.generate(
                mob.getRandom(), mob.level().getGameTime());
        mob.setData(ModAttachments.PERSONA, persona);
        // A hidden name: hordes shouldn't be a wall of floating text, but look
        // at one (HUD) and you learn who it used to be.
        applyNameTag(mob, persona, false);
    }

    /** The (up to two) nearest censused adult villagers — the presumed parents. */
    private static List<Villager> findParents(Villager baby) {
        AABB box = baby.getBoundingBox().inflate(PARENT_SEARCH_RADIUS);
        return baby.level().getEntitiesOfClass(Villager.class, box,
                        v -> v != baby && !v.isBaby() && Census.isCensused(v)).stream()
                .sorted(Comparator.comparingDouble(v -> v.distanceToSqr(baby)))
                .limit(2)
                .toList();
    }

    private static void applyNameTag(LivingEntity entity, Persona persona, boolean visible) {
        entity.setCustomName(Component.literal(persona.fullName()));
        entity.setCustomNameVisible(visible);
    }
}
