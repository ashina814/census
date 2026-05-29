package dev.kout2.census.event;

import dev.kout2.census.CensusMod;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.memory.Memories;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Turns gameplay events into memories on censused mobs.
 *
 * Phase 2 handles three things: being hurt (HARMED), being fed (FED), and
 * witnessing a nearby death (WITNESSED_DEATH). The disposition/emotional
 * consequences of these memories arrive in Phase 3.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class MemoryEventHandlers {
    private static final double WITNESS_RADIUS = 16.0;

    private MemoryEventHandlers() {}

    /** Victim remembers being harmed, and by whom. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !victim.hasData(ModAttachments.PERSONA)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Memories.record(victim, EventType.HARMED, attacker != null ? attacker.getUUID() : null);
    }

    /** A mob remembers being offered food (the interaction fires before vanilla handling). */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity target
                && target.hasData(ModAttachments.PERSONA)
                && event.getItemStack().has(DataComponents.FOOD)) {
            Memories.record(target, EventType.FED, event.getEntity().getUUID());
        }
    }

    /** Nearby censused mobs remember witnessing a death. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) {
            return;
        }
        AABB box = dead.getBoundingBox().inflate(WITNESS_RADIUS);
        List<LivingEntity> witnesses = dead.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != dead && e.hasData(ModAttachments.PERSONA));
        for (LivingEntity witness : witnesses) {
            Memories.record(witness, EventType.WITNESSED_DEATH, dead.getUUID());
        }
    }
}
