package dev.kout2.census.report;

import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.memory.MemoryEntry;
import dev.kout2.census.memory.MemoryStream;
import dev.kout2.census.persona.DerivedTrait;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.ReputationBook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Builds a compact, at-a-glance "card" summarising a mob's inner state for one
 * viewer — name, current feeling, what it thinks of you, and its latest strong
 * memory. Used by the census book (right-click) so players can read a mob
 * without typing a single command. Phase 8 will reuse this for an on-screen HUD.
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

        MutableComponent card = Component.literal("")
                .append(line(ChatFormatting.GOLD, p.fullName()
                        + (lineage.hasParents() ? " (gen " + lineage.generation() + ")" : "")));

        Emotion dominant = emotion.dominant();
        card.append(line(ChatFormatting.YELLOW, "feeling: "
                + (dominant == null ? "calm" : dominant.lowerName())
                + " · mood: " + emotion.mood().label()));

        float opinion = reputation.opinionOf(viewer);
        card.append(line(opinionColor(opinion), "thinks of you: " + opinionWord(opinion)
                + String.format(" (%+.0f)", opinion)));

        List<MemoryEntry> recent = memory.recent(1);
        if (!recent.isEmpty()) {
            MemoryEntry last = recent.get(0);
            card.append(line(ChatFormatting.GRAY, "last: " + last.type().getSerializedName()
                    + " (" + ago(now - last.tick()) + ")"));
        }

        String traits = traits(p);
        if (!traits.isEmpty()) {
            card.append(line(ChatFormatting.DARK_GRAY, traits));
        }
        return card;
    }

    private static MutableComponent line(ChatFormatting color, String text) {
        return Component.literal(text + "\n").withStyle(color);
    }

    private static String opinionWord(float o) {
        if (o > 25f) return "adores you";
        if (o > 5f) return "likes you";
        if (o < -25f) return "hates you";
        if (o < -5f) return "dislikes you";
        return "neutral on you";
    }

    private static ChatFormatting opinionColor(float o) {
        if (o > 5f) return ChatFormatting.GREEN;
        if (o < -5f) return ChatFormatting.RED;
        return ChatFormatting.GRAY;
    }

    private static String ago(long ticks) {
        long t = Math.max(0L, ticks);
        if (t < 1200) return (t / 20) + "s ago";
        if (t < 24000) return (t / 1200) + "m ago";
        return (t / 24000) + "d ago";
    }

    private static String traits(Persona p) {
        StringJoiner joiner = new StringJoiner(", ");
        for (DerivedTrait trait : p.traits()) {
            joiner.add(trait.name().toLowerCase());
        }
        return joiner.toString();
    }
}
