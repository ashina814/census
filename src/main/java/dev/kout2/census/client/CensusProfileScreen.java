package dev.kout2.census.client;

import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.network.ProfilePayload;
import dev.kout2.census.persona.BigFive;
import dev.kout2.census.persona.DerivedTrait;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The census book's profile screen: one mob's inner life on a single card —
 * identity, current feeling, what it thinks of you, its personality as bars,
 * and its freshest memories. Rendered entirely from a {@link ProfilePayload}
 * snapshot, so it shows server truth, not just synced state.
 */
public final class CensusProfileScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 10;

    private static final int PANEL_BG = 0xF0101018;
    private static final int PANEL_BORDER = 0xFF3A3A50;
    private static final int BAR_BG = 0xFF2A2A38;
    private static final int BAR_FILL = 0xFF5FA8E8;

    private static final int GOLD = 0xFFFFC74A;
    private static final int YELLOW = 0xFFFFE08A;
    private static final int PURPLE = 0xFFC9A0DC;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int DARK_GRAY = 0xFF777788;
    private static final int GREEN = 0xFF7FD98A;
    private static final int RED = 0xFFE07070;

    private final ProfilePayload profile;

    public CensusProfileScreen(ProfilePayload profile) {
        super(Component.literal(profile.fullName()));
        this.profile = profile;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int contentHeight = computeHeight();
        int x = (width - PANEL_WIDTH) / 2;
        int y = Math.max(12, (height - contentHeight) / 2);
        graphics.fill(x - 1, y - 1, x + PANEL_WIDTH + 1, y + contentHeight + 1, PANEL_BORDER);
        graphics.fill(x, y, x + PANEL_WIDTH, y + contentHeight, PANEL_BG);

        int tx = x + PADDING;
        int ty = y + PADDING;

        // Identity
        MutableComponent name = Component.literal(profile.fullName());
        if (profile.generation() > 0) {
            name.append(" ").append(Component.translatable("census.gen", profile.generation()));
        }
        graphics.drawString(font, name, x + (PANEL_WIDTH - font.width(name)) / 2, ty, GOLD);
        ty += LINE_HEIGHT + 2;

        // Feeling + mood
        Emotion dominant = profile.emotion().dominant();
        Component feeling = dominant == null
                ? Component.translatable("census.calm")
                : Component.translatable("census.emotion." + dominant.lowerName());
        Component mood = Component.translatable("census.mood." + profile.emotion().mood().label());
        graphics.drawString(font, Component.translatable("census.line.feeling", feeling, mood), tx, ty, YELLOW);
        ty += LINE_HEIGHT;

        // Mind (reflection), when present
        if (!profile.mindKey().isEmpty()) {
            graphics.drawString(font, Component.translatable("census.line.mind",
                    Component.translatable(profile.mindKey())), tx, ty, PURPLE);
            ty += LINE_HEIGHT;
        }

        // Opinion of the viewer
        float opinion = profile.opinion();
        Component opinionLine = Component.translatable("census.line.opinion",
                Component.translatable("census.opinion." + opinionKey(opinion)),
                String.format("%+.0f", opinion));
        graphics.drawString(font, opinionLine, tx, ty, opinion > 5f ? GREEN : opinion < -5f ? RED : GRAY);
        ty += LINE_HEIGHT + 4;

        // Personality bars
        graphics.drawString(font, Component.translatable("census.gui.personality"), tx, ty, DARK_GRAY);
        ty += LINE_HEIGHT;
        BigFive b = profile.personality();
        ty = bar(graphics, tx, ty, "census.gui.big5.o", b.openness());
        ty = bar(graphics, tx, ty, "census.gui.big5.c", b.conscientiousness());
        ty = bar(graphics, tx, ty, "census.gui.big5.e", b.extraversion());
        ty = bar(graphics, tx, ty, "census.gui.big5.a", b.agreeableness());
        ty = bar(graphics, tx, ty, "census.gui.big5.n", b.neuroticism());

        // Traits (derived client-side from the same personality)
        MutableComponent traits = Component.empty();
        boolean first = true;
        for (DerivedTrait trait : DerivedTrait.of(b)) {
            if (!first) {
                traits.append("、");
            }
            traits.append(Component.translatable("census.trait." + trait.name().toLowerCase()));
            first = false;
        }
        if (!first) {
            graphics.drawString(font, traits, tx, ty, GRAY);
        }
        ty += LINE_HEIGHT + 4;

        // Recent memories
        graphics.drawString(font, Component.translatable("census.gui.memories"), tx, ty, DARK_GRAY);
        ty += LINE_HEIGHT;
        if (profile.memories().isEmpty()) {
            graphics.drawString(font, Component.translatable("census.gui.no_memories"), tx, ty, DARK_GRAY);
        }
        for (ProfilePayload.MemoryLine line : profile.memories()) {
            Component entry = Component.translatable("census.line.last",
                    Component.translatable("census.event." + line.eventKey()),
                    ago(profile.gameTime() - line.tick()));
            int color = line.valence() > 0.1f ? GREEN : line.valence() < -0.1f ? RED : GRAY;
            graphics.drawString(font, entry, tx, ty, color);
            ty += LINE_HEIGHT;
        }
    }

    /** Draws one labelled personality bar; returns the next y. */
    private int bar(GuiGraphics graphics, int x, int y, String labelKey, float value) {
        graphics.drawString(font, Component.translatable(labelKey), x, y, GRAY);
        int barX = x + 92;
        int barWidth = PANEL_WIDTH - PADDING * 2 - 92;
        graphics.fill(barX, y + 1, barX + barWidth, y + 9, BAR_BG);
        graphics.fill(barX, y + 1, barX + Math.round(barWidth * Math.clamp(value, 0f, 1f)), y + 9, BAR_FILL);
        return y + LINE_HEIGHT;
    }

    private int computeHeight() {
        int lines = 3                                   // name, feeling, opinion
                + (profile.mindKey().isEmpty() ? 0 : 1)
                + 1 + 5 + 1                             // personality header + bars + traits
                + 1 + Math.max(1, profile.memories().size()); // memories header + lines
        return PADDING * 2 + lines * LINE_HEIGHT + 12;
    }

    private static String opinionKey(float o) {
        if (o > 25f) return "adores";
        if (o > 5f) return "likes";
        if (o < -25f) return "hates";
        if (o < -5f) return "dislikes";
        return "neutral";
    }

    private static Component ago(long ticks) {
        long t = Math.max(0L, ticks);
        if (t < 1200) return Component.translatable("census.ago.seconds", t / 20);
        if (t < 24000) return Component.translatable("census.ago.minutes", t / 1200);
        return Component.translatable("census.ago.days", t / 24000);
    }
}
