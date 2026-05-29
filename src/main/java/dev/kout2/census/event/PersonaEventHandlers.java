package dev.kout2.census.event;

import dev.kout2.census.CensusMod;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.persona.generator.PersonaGenerator;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Hooks vanilla entity lifecycle to assign personas.
 *
 * Phase 1: every Villager that enters a level server-side gets a persona if it
 * doesn't already have one. We surface the name immediately by borrowing the
 * vanilla custom-name tag (a dedicated renderer with emotion overlays comes in
 * Phase 8). Later phases broaden this to animals and hostiles.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class PersonaEventHandlers {
    private PersonaEventHandlers() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Villager villager) {
            if (!villager.hasData(ModAttachments.PERSONA)) {
                long now = villager.level().getGameTime();
                Persona persona = PersonaGenerator.generate(villager.getRandom(), now);
                villager.setData(ModAttachments.PERSONA, persona);
                applyNameTag(villager, persona);
            }
        }
    }

    /** Shows the persona's full name above the villager via the vanilla name tag. */
    private static void applyNameTag(Villager villager, Persona persona) {
        villager.setCustomName(Component.literal(persona.fullName()));
        villager.setCustomNameVisible(true);
    }
}
