package dev.kout2.census.report;

import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.memory.MemoryEntry;
import dev.kout2.census.memory.MemoryStream;
import dev.kout2.census.persona.DerivedTrait;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.reflection.Reflection;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.ReputationBook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a compact, at-a-glance "card" summarising a mob's inner state for one
 * viewer. Fully localised via translation keys (en/ja), so it reads in the
 * client's language. Used by the census book; reused by a Phase 8 HUD.
 */
public final class CensusReport {
    private CensusReport() {}

    public static Component card(LivingEntity entity, UUID viewer, long now) {
        Persona p = entity.getData(ModAttachments.PERSONA);
        EmotionalState emotion = entity.getData(ModAttachments.EMOTION);
        emotion.decayTo(now);
        Lineage lineage = entity.getData(ModAttachments.LINEAGE);
        ReputationBook reputation = entity.getData(ModAttachments.REPUTATION);
        MemoryStream memory = entity.getData(ModAttachments.MEMORY);

        MutableComponent card = Component.literal("");

        // Name (+ generation)
        MutableComponent nameLine = Component.literal(p.fullName());
        if (lineage.hasParents()) {
            nameLine.append(" ").append(Component.translatable("census.gen", lineage.generation()));
        }
        appendLine(card, nameLine, ChatFormatting.GOLD);

        // Feeling + mood
        Emotion dominant = emotion.dominant();
        Component feeling = dominant == null
                ? Component.translatable("census.calm")
                : Component.translatable("census.emotion." + dominant.lowerName());
        Component mood = Component.translatable("census.mood." + emotion.mood().label());
        appendLine(card, Component.translatable("census.line.feeling", feeling, mood),
                ChatFormatting.YELLOW);

        // Opinion of the viewer
        float opinion = reputation.opinionOf(viewer);
        appendLine(card, Component.translatable("census.line.opinion",
                        Component.translatable("census.opinion." + opinionKey(opinion)),
                        String.format("%+.0f", opinion)),
                opinionColor(opinion));

        // Standing state of mind (reflection)
        Reflection insight = entity.getData(ModAttachments.REFLECTIONS).dominant();
        if (insight != null) {
            appendLine(card, Component.translatable("census.line.mind",
                    Component.translatable("census.reflection." + insight.type().lowerName())),
                    ChatFormatting.LIGHT_PURPLE);
        }

        // Most recent memory
        List<MemoryEntry> recent = memory.recent(1);
        if (!recent.isEmpty()) {
            MemoryEntry last = recent.get(0);
            appendLine(card, Component.translatable("census.line.last",
                            Component.translatable("census.event." + last.type().getSerializedName()),
                            ago(now - last.tick())),
                    ChatFormatting.GRAY);
        }

        // Traits
        Set<DerivedTrait> traits = p.traits();
        if (!traits.isEmpty()) {
            MutableComponent traitsLine = Component.literal("");
            boolean first = true;
            for (DerivedTrait trait : traits) {
                if (!first) {
                    traitsLine.append("、");
                }
                traitsLine.append(Component.translatable("census.trait." + trait.name().toLowerCase()));
                first = false;
            }
            appendLine(card, traitsLine, ChatFormatting.DARK_GRAY);
        }
        return card;
    }

    private static void appendLine(MutableComponent card, Component content, ChatFormatting color) {
        card.append(content.copy().withStyle(color)).append("\n");
    }

    private static String opinionKey(float o) {
        if (o > 25f) return "adores";
        if (o > 5f) return "likes";
        if (o < -25f) return "hates";
        if (o < -5f) return "dislikes";
        return "neutral";
    }

    private static ChatFormatting opinionColor(float o) {
        if (o > 5f) return ChatFormatting.GREEN;
        if (o < -5f) return ChatFormatting.RED;
        return ChatFormatting.GRAY;
    }

    private static Component ago(long ticks) {
        long t = Math.max(0L, ticks);
        if (t < 1200) return Component.translatable("census.ago.seconds", t / 20);
        if (t < 24000) return Component.translatable("census.ago.minutes", t / 1200);
        return Component.translatable("census.ago.days", t / 24000);
    }
}
