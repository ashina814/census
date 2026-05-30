package dev.kout2.census.client;

import dev.kout2.census.CensusMod;
import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.persona.DerivedTrait;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * On-screen "glance" HUD: look at a censused mob and its name, mood and traits
 * appear at the top of the screen — no book, no commands.
 *
 * Built purely from synced data (Persona, EmotionalState, Lineage), so the
 * client can render it every frame. Server-only details (what it thinks of you,
 * its last memory) stay in the census book's fuller card.
 */
@EventBusSubscriber(modid = CensusMod.MODID, value = Dist.CLIENT)
public final class CensusHudOverlay {
    private static final int LINE_HEIGHT = 10;
    private static final int TOP_MARGIN = 6;
    private static final long REBUILD_INTERVAL_MS = 200L;

    // Cache so we don't allocate translatable Components every single frame.
    private static int cachedTargetId = -1;
    private static long cachedAtMs = 0L;
    private static List<Component> cachedLines = List.of();

    private CensusHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.level == null) {
            return;
        }
        if (!(mc.crosshairPickEntity instanceof LivingEntity target)
                || !target.hasData(ModAttachments.PERSONA)) {
            return;
        }
        Persona persona = target.getData(ModAttachments.PERSONA);
        if (persona.id().equals(Persona.UNKNOWN.id())) {
            return; // placeholder default, not a real persona
        }

        long nowMs = System.currentTimeMillis();
        if (target.getId() != cachedTargetId || nowMs - cachedAtMs > REBUILD_INTERVAL_MS) {
            cachedLines = buildLines(target, persona);
            cachedTargetId = target.getId();
            cachedAtMs = nowMs;
        }
        List<Component> lines = cachedLines;
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int y = TOP_MARGIN;
        for (Component line : lines) {
            int x = (screenWidth - font.width(line)) / 2;
            graphics.drawString(font, line, x, y, 0xFFFFFFFF);
            y += LINE_HEIGHT;
        }
    }

    private static List<Component> buildLines(LivingEntity target, Persona persona) {
        List<Component> lines = new ArrayList<>(3);

        Lineage lineage = target.getData(ModAttachments.LINEAGE);
        MutableComponent name = Component.literal(persona.fullName());
        if (lineage.hasParents()) {
            name.append(" ").append(Component.translatable("census.gen", lineage.generation()));
        }
        lines.add(name.withStyle(ChatFormatting.GOLD));

        EmotionalState emotion = target.getData(ModAttachments.EMOTION);
        emotion.decayTo(target.level().getGameTime());
        Emotion dominant = emotion.dominant();
        Component feeling = dominant == null
                ? Component.translatable("census.calm")
                : Component.translatable("census.emotion." + dominant.lowerName());
        Component mood = Component.translatable("census.mood." + emotion.mood().label());
        lines.add(Component.translatable("census.line.feeling", feeling, mood)
                .withStyle(ChatFormatting.YELLOW));

        MutableComponent traits = Component.empty();
        boolean first = true;
        for (DerivedTrait trait : persona.traits()) {
            if (!first) {
                traits.append("、");
            }
            traits.append(Component.translatable("census.trait." + trait.name().toLowerCase()));
            first = false;
        }
        if (!first) {
            lines.add(traits.withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
