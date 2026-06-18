package dev.kout2.census.event;

import dev.kout2.census.Census;
import dev.kout2.census.CensusMod;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.network.ProfilePayload;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.reflection.Reflection;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The census book's reason to exist: right-click any censused mob with it and
 * a profile screen opens — identity, feeling, opinion of you, personality and
 * recent memories. The data travels as a one-shot {@link ProfilePayload}
 * snapshot because half of it (memories, opinions, reflection) lives only on
 * the server.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class InspectionHandler {
    private static final int MEMORY_LINES = 5;

    private InspectionHandler() {}

    @SubscribeEvent
    public static void onInspect(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!event.getItemStack().is(ModItems.CENSUS_BOOK.get())) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target) || !Census.isCensused(target)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, buildProfile(target, player));
        }

        // Consume the interaction so the vanilla trade screen doesn't open.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static ProfilePayload buildProfile(LivingEntity target, ServerPlayer viewer) {
        long now = target.level().getGameTime();
        Persona persona = target.getData(ModAttachments.PERSONA);
        EmotionalState emotion = target.getData(ModAttachments.EMOTION);
        emotion.decayTo(now);
        Lineage lineage = target.getData(ModAttachments.LINEAGE);
        Reflection insight = target.getData(ModAttachments.REFLECTIONS).dominant();
        float opinion = target.getData(ModAttachments.REPUTATION).opinionOf(viewer.getUUID());
        List<ProfilePayload.MemoryLine> memories =
                target.getData(ModAttachments.MEMORY).recent(MEMORY_LINES).stream()
                        .map(e -> new ProfilePayload.MemoryLine(
                                e.type().getSerializedName(), e.tick(), e.valence()))
                        .toList();
        return new ProfilePayload(
                persona.fullName(),
                lineage.hasParents() ? lineage.generation() : 0,
                persona.personality(),
                emotion,
                insight == null ? "" : "census.reflection." + insight.type().lowerName(),
                opinion,
                now,
                memories);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.CENSUS_BOOK.get())) {
            event.getToolTip().add(Component.translatable("item.census.census_book.tip")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
