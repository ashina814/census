package dev.kout2.census.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.kout2.census.CensusMod;
import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.emotion.PADMood;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.memory.MemoryEntry;
import dev.kout2.census.memory.MemoryStream;
import dev.kout2.census.reputation.ReputationBook;
import dev.kout2.census.persona.BigFive;
import dev.kout2.census.persona.DerivedTrait;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * {@code /census who [target]} — inspect a mob's persona.
 *
 * With no argument it picks the nearest censused villager to the command
 * source; with an entity selector it inspects that entity. The dev-facing way
 * to verify Phase 1 generation.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class CensusCommand {
    private static final double SEARCH_RADIUS = 32.0;

    private CensusCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("census")
                .then(Commands.literal("who")
                        .executes(ctx -> who(ctx.getSource(), nearest(ctx.getSource())))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> who(ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target")))))
                .then(Commands.literal("memory")
                        .executes(ctx -> memory(ctx.getSource(), nearest(ctx.getSource())))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> memory(ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target")))))
                .then(Commands.literal("emotion")
                        .executes(ctx -> emotion(ctx.getSource(), nearest(ctx.getSource())))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> emotion(ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target")))))
                .then(Commands.literal("family")
                        .executes(ctx -> family(ctx.getSource(), nearest(ctx.getSource())))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> family(ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target")))))
                .then(Commands.literal("reputation")
                        .executes(ctx -> reputation(ctx.getSource(), nearest(ctx.getSource())))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> reputation(ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target"))))));
    }

    /** Nearest censused villager to the source, or {@code null} if none in range. */
    private static Entity nearest(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();
        AABB box = AABB.ofSize(origin, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2);
        return level.getEntitiesOfClass(Villager.class, box,
                        v -> v.hasData(ModAttachments.PERSONA)).stream()
                .min(Comparator.comparingDouble(v -> v.distanceToSqr(origin)))
                .orElse(null);
    }

    private static int who(CommandSourceStack source, Entity entity) {
        if (!validate(source, entity)) {
            return 0;
        }
        Persona p = entity.getData(ModAttachments.PERSONA);
        long now = source.getLevel().getGameTime();
        source.sendSuccess(() -> describe(p, now), false);
        return 1;
    }

    private static int memory(CommandSourceStack source, Entity entity) {
        if (!validate(source, entity)) {
            return 0;
        }
        Persona p = entity.getData(ModAttachments.PERSONA);
        MemoryStream stream = entity.getData(ModAttachments.MEMORY);
        long now = source.getLevel().getGameTime();
        source.sendSuccess(() -> describeMemory(p, stream, now), false);
        return 1;
    }

    private static int emotion(CommandSourceStack source, Entity entity) {
        if (!validate(source, entity)) {
            return 0;
        }
        Persona p = entity.getData(ModAttachments.PERSONA);
        EmotionalState state = entity.getData(ModAttachments.EMOTION);
        long now = source.getLevel().getGameTime();
        state.decayTo(now); // show current values, not last-touched
        source.sendSuccess(() -> describeEmotion(p, state), false);
        return 1;
    }

    private static int family(CommandSourceStack source, Entity entity) {
        if (!validate(source, entity)) {
            return 0;
        }
        Persona p = entity.getData(ModAttachments.PERSONA);
        Lineage lineage = entity.getData(ModAttachments.LINEAGE);
        source.sendSuccess(() -> describeFamily(p, lineage), false);
        return 1;
    }

    private static int reputation(CommandSourceStack source, Entity entity) {
        if (!validate(source, entity)) {
            return 0;
        }
        Persona p = entity.getData(ModAttachments.PERSONA);
        ReputationBook book = entity.getData(ModAttachments.REPUTATION);
        UUID viewer = source.getEntity() != null ? source.getEntity().getUUID() : null;
        source.sendSuccess(() -> describeReputation(p, book, viewer), false);
        return 1;
    }

    /** Shared null/persona guard for both subcommands. */
    private static boolean validate(CommandSourceStack source, Entity entity) {
        if (entity == null) {
            source.sendFailure(Component.literal(
                    "No censused villager within " + (int) SEARCH_RADIUS + " blocks."));
            return false;
        }
        if (!entity.hasData(ModAttachments.PERSONA)) {
            source.sendFailure(Component.literal(
                    entity.getName().getString() + " has no persona."));
            return false;
        }
        return true;
    }

    private static Component describe(Persona p, long now) {
        BigFive b = p.personality();
        return Component.literal("")
                .append(line(ChatFormatting.GOLD, "── " + p.fullName() + " ──"))
                .append(line(ChatFormatting.GRAY, "  " + p.gender().getSerializedName()
                        + ", age " + p.ageInDays(now) + "d"))
                .append(line(ChatFormatting.AQUA, "  Openness          " + bar(b.openness())))
                .append(line(ChatFormatting.AQUA, "  Conscientiousness " + bar(b.conscientiousness())))
                .append(line(ChatFormatting.AQUA, "  Extraversion      " + bar(b.extraversion())))
                .append(line(ChatFormatting.AQUA, "  Agreeableness     " + bar(b.agreeableness())))
                .append(line(ChatFormatting.AQUA, "  Neuroticism       " + bar(b.neuroticism())))
                .append(line(ChatFormatting.GREEN, "  traits: " + traits(p)));
    }

    private static Component line(ChatFormatting color, String text) {
        return Component.literal(text + "\n").withStyle(color);
    }

    /** Renders a 0..1 value as a 10-cell bar plus the numeric value. */
    private static String bar(float value) {
        int filled = Math.clamp(Math.round(value * 10f), 0, 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? '█' : '░');
        }
        return sb.append(' ').append(String.format("%.2f", value)).toString();
    }

    private static String traits(Persona p) {
        StringJoiner joiner = new StringJoiner(", ");
        for (DerivedTrait trait : p.traits()) {
            joiner.add(trait.name().toLowerCase());
        }
        return joiner.length() == 0 ? "(balanced)" : joiner.toString();
    }

    private static final int MEMORY_LINES = 15;

    private static Component describeMemory(Persona p, MemoryStream stream, long now) {
        MutableComponent body = Component.literal("")
                .append(line(ChatFormatting.GOLD, "── " + p.fullName() + " — memories ("
                        + stream.size() + ") ──"));
        if (stream.isEmpty()) {
            body.append(line(ChatFormatting.GRAY, "  (nothing memorable yet)"));
            return body;
        }
        for (MemoryEntry e : stream.recent(MEMORY_LINES)) {
            body.append(line(valenceColor(e.valence()), "  " + formatEntry(e, now)));
        }
        return body;
    }

    private static String formatEntry(MemoryEntry e, long now) {
        long agoTicks = Math.max(0L, now - e.tick());
        String ago = agoTicks < 1200 ? (agoTicks / 20) + "s ago"
                : agoTicks < 24000 ? (agoTicks / 1200) + "m ago"
                : (agoTicks / 24000) + "d ago";
        return String.format("%-16s imp %.1f  val %+.1f  (%s)",
                e.type().getSerializedName(), e.importance(), e.valence(), ago);
    }

    private static ChatFormatting valenceColor(float valence) {
        if (valence > 0.1f) return ChatFormatting.GREEN;
        if (valence < -0.1f) return ChatFormatting.RED;
        return ChatFormatting.GRAY;
    }

    private static Component describeEmotion(Persona p, EmotionalState state) {
        Emotion dominant = state.dominant();
        PADMood mood = state.mood();
        MutableComponent body = Component.literal("")
                .append(line(ChatFormatting.GOLD, "── " + p.fullName() + " — emotion ──"))
                .append(line(ChatFormatting.YELLOW, "  feeling: "
                        + (dominant == null ? "calm" : dominant.lowerName())));
        for (Emotion e : Emotion.values()) {
            float v = state.intensity(e);
            ChatFormatting color = v > 0.05f ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY;
            body.append(line(color, String.format("  %-10s %s", e.lowerName(), bar(v))));
        }
        body.append(line(ChatFormatting.LIGHT_PURPLE, String.format(
                "  mood: %s  (P%+.2f A%+.2f D%+.2f)",
                mood.label(), mood.pleasure(), mood.arousal(), mood.dominance())));
        return body;
    }

    private static Component describeFamily(Persona p, Lineage lineage) {
        MutableComponent body = Component.literal("")
                .append(line(ChatFormatting.GOLD, "── " + p.fullName() + " — family ──"))
                .append(line(ChatFormatting.GRAY, "  generation: " + lineage.generation()
                        + (lineage.hasParents() ? "" : " (founder)")));
        lineage.parentA().ifPresent(id ->
                body.append(line(ChatFormatting.GRAY, "  parent A: " + shortId(id))));
        lineage.parentB().ifPresent(id ->
                body.append(line(ChatFormatting.GRAY, "  parent B: " + shortId(id))));
        return body;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static final int REPUTATION_LINES = 10;

    private static Component describeReputation(Persona p, ReputationBook book, UUID viewer) {
        MutableComponent body = Component.literal("")
                .append(line(ChatFormatting.GOLD, "── " + p.fullName() + " — opinions ("
                        + book.size() + ") ──"));
        if (book.size() == 0) {
            body.append(line(ChatFormatting.GRAY, "  (knows no one yet)"));
            return body;
        }
        book.entries().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Float> e) -> Math.abs(e.getValue())).reversed())
                .limit(REPUTATION_LINES)
                .forEach(e -> {
                    boolean isViewer = e.getKey().equals(viewer);
                    String who = (isViewer ? "you" : shortId(e.getKey()));
                    body.append(line(opinionColor(e.getValue()),
                            String.format("  %-10s %+6.1f", who, e.getValue())));
                });
        return body;
    }

    private static ChatFormatting opinionColor(float score) {
        if (score > 5f) return ChatFormatting.GREEN;
        if (score < -5f) return ChatFormatting.RED;
        return ChatFormatting.GRAY;
    }
}
