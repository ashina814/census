package dev.kout2.census.event;

import dev.kout2.census.Census;
import dev.kout2.census.CensusMod;
import dev.kout2.census.block.GravestoneBlockEntity;
import dev.kout2.census.census.CensusRegistry;
import dev.kout2.census.config.CensusConfig;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.registry.ModBlocks;
import dev.kout2.census.social.SocialBonds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns gameplay events into observations on censused mobs. Each call to
 * {@link Census#observe} fans out to both memory (Phase 2) and emotional
 * appraisal (Phase 3).
 *
 * Handles being hurt (HARMED), being fed (FED), and witnessing a nearby death
 * (WITNESSED_DEATH).
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class MemoryEventHandlers {
    private static final double WITNESS_RADIUS = 16.0;

    private MemoryEventHandlers() {}

    /** Victim remembers being harmed, and by whom. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !Census.isCensused(victim)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Census.observe(victim, EventType.HARMED, attacker != null ? attacker.getUUID() : null);
    }

    /** A mob remembers being offered food (the interaction fires before vanilla handling). */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity target
                && Census.isCensused(target)
                && event.getItemStack().has(DataComponents.FOOD)) {
            Census.observe(target, EventType.FED, event.getEntity().getUUID());
        }
    }

    /**
     * Nearby censused mobs remember a death. Those who loved the deceased — kin,
     * or anyone closely bonded — grieve hard (RELATIVE_KILLED, blamed on the
     * killer: the seed of revenge), while bystanders merely WITNESSED_DEATH.
     *
     * Generalising "kin" to "bond" is what frees the revenge story from rare
     * breeding: kill a villager and its <i>friends</i> may come for you.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) {
            return;
        }
        UUID deadPersonaId = Census.isCensused(dead)
                ? dead.getData(ModAttachments.PERSONA).id() : null;
        UUID deadEntityId = dead.getUUID();
        Entity killer = event.getSource().getEntity();
        UUID killerId = killer != null ? killer.getUUID() : null;

        // Nearby witnesses grieve by bond; blood kin are handled below via the
        // registry so they react at any distance (and aren't double-counted here).
        AABB box = dead.getBoundingBox().inflate(WITNESS_RADIUS);
        List<LivingEntity> witnesses = dead.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != dead && Census.isCensused(e));
        for (LivingEntity witness : witnesses) {
            if (deadPersonaId != null && isChildOf(witness, deadPersonaId)) {
                continue; // kin → registry path
            }
            if (SocialBonds.bondToward(witness, deadEntityId) >= SocialBonds.GRIEF_BOND) {
                Census.observe(witness, EventType.RELATIVE_KILLED, killerId);
            } else {
                Census.observe(witness, EventType.WITNESSED_DEATH, deadEntityId);
            }
        }

        // World-registry path: every child of the deceased — loaded, far, or in
        // an unloaded chunk — is queued to grieve and (if vengeful) avenge.
        if (deadPersonaId != null) {
            MinecraftServer server = dead.level().getServer();
            if (server != null) {
                CensusRegistry registry = CensusRegistry.get(server);
                long day = dead.level().getGameTime() / 24000L;
                registry.recordDeath(deadPersonaId, day);
                for (UUID childId : registry.childrenOf(deadPersonaId)) {
                    registry.enqueueGrief(childId, Optional.ofNullable(killerId), dead.level().getGameTime());
                }
            }
            if (CensusConfig.GRAVESTONES_ENABLED.get()) {
                placeGravestone(dead, killer);
            }
        }
    }

    /** Leaves a headstone where a censused mob fell, inscribed with its epitaph. */
    private static void placeGravestone(LivingEntity dead, Entity killer) {
        Level level = dead.level();
        BlockPos pos = dead.blockPosition();
        if (!isReplaceable(level, pos)) {
            pos = pos.above();
            if (!isReplaceable(level, pos)) {
                return;
            }
        }
        level.setBlockAndUpdate(pos, ModBlocks.GRAVESTONE.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof GravestoneBlockEntity grave) {
            Persona persona = dead.getData(ModAttachments.PERSONA);
            int generation = dead.getData(ModAttachments.LINEAGE).generation();
            long day = level.getGameTime() / 24000L;
            String killerName = killer != null ? killer.getName().getString() : "";
            grave.inscribe(persona.fullName(), generation, day, killerName);
        }
    }

    private static boolean isReplaceable(Level level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }

    private static boolean isChildOf(LivingEntity entity, UUID parentPersonaId) {
        return entity.hasData(ModAttachments.LINEAGE)
                && entity.getData(ModAttachments.LINEAGE).isChildOf(parentPersonaId);
    }
}
