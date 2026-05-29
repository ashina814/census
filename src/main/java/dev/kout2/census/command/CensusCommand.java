package dev.kout2.census.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.kout2.census.CensusMod;
import dev.kout2.census.persona.BigFive;
import dev.kout2.census.persona.DerivedTrait;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.StringJoiner;

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
                        .executes(ctx -> whoNearest(ctx.getSource()))
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> who(ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target"))))));
    }

    private static int whoNearest(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 origin = source.getPosition();
        AABB box = AABB.ofSize(origin, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2);

        Villager nearest = level.getEntitiesOfClass(Villager.class, box,
                        v -> v.hasData(ModAttachments.PERSONA)).stream()
                .min(Comparator.comparingDouble(v -> v.distanceToSqr(origin)))
                .orElse(null);

        if (nearest == null) {
            source.sendFailure(Component.literal(
                    "No censused villager within " + (int) SEARCH_RADIUS + " blocks."));
            return 0;
        }
        return who(source, nearest);
    }

    private static int who(CommandSourceStack source, Entity entity) {
        if (!entity.hasData(ModAttachments.PERSONA)) {
            source.sendFailure(Component.literal(
                    entity.getName().getString() + " has no persona."));
            return 0;
        }
        Persona p = entity.getData(ModAttachments.PERSONA);
        long now = source.getLevel().getGameTime();
        source.sendSuccess(() -> describe(p, now), false);
        return 1;
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
}
