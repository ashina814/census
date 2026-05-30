package dev.kout2.census.event;

import dev.kout2.census.CensusMod;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.registry.ModItems;
import dev.kout2.census.report.CensusReport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The census book's reason to exist: right-click any censused mob with it to
 * read an at-a-glance {@link CensusReport} card in chat — no commands to type.
 *
 * This is the everyday inspection tool; the {@code /census} subcommands remain
 * for the detailed, number-by-number views.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class InspectionHandler {
    private InspectionHandler() {}

    @SubscribeEvent
    public static void onInspect(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!event.getItemStack().is(ModItems.CENSUS_BOOK.get())) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)
                || !target.hasData(ModAttachments.PERSONA)) {
            return;
        }
        Player player = event.getEntity();
        long now = player.level().getGameTime();
        player.displayClientMessage(CensusReport.card(target, player.getUUID(), now), false);

        // Consume the interaction so the vanilla trade screen doesn't open.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.CENSUS_BOOK.get())) {
            event.getToolTip().add(Component.translatable("item.census.census_book.tip")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
