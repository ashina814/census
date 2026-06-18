package dev.kout2.census.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration. The first tunables to escape hardcoding; the rest
 * of the simulation constants (bond rates, thresholds, reproduction…) migrate
 * here over the release milestones so server admins can tune Census without
 * recompiling. See {@code scripts/sim.py} to preview the effect of changes.
 */
public final class CensusConfig {
    public static final ModConfigSpec SPEC;

    /** Leave a gravestone where a censused mob dies. */
    public static final ModConfigSpec.BooleanValue GRAVESTONES_ENABLED;
    /** Render villager name tags through walls at all times (off = vanilla hover only). */
    public static final ModConfigSpec.BooleanValue NAME_TAGS_ALWAYS_VISIBLE;
    /** Cap on remembered opinions per mob; least-significant are pruned beyond this. */
    public static final ModConfigSpec.IntValue REPUTATION_MAX_ENTRIES;

    /** Master switch for the social heartbeat (gossip, bonds, courtship, reflection, births). */
    public static final ModConfigSpec.BooleanValue SOCIAL_ENABLED;
    /** Whether couples bear children. */
    public static final ModConfigSpec.BooleanValue REPRODUCTION_ENABLED;
    /** Local villager cap above which couples stop having children (0 disables births). */
    public static final ModConfigSpec.IntValue LOCAL_POPULATION_CAP;
    /** Whether mobs distil memories into lasting reflections. */
    public static final ModConfigSpec.BooleanValue REFLECTION_ENABLED;
    /** Emit particles over mobs feeling a strong emotion. */
    public static final ModConfigSpec.BooleanValue EMOTION_PARTICLES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Census — world & presentation").push("world");
        GRAVESTONES_ENABLED = builder
                .comment("Place a gravestone where a censused mob dies.")
                .define("gravestonesEnabled", true);
        NAME_TAGS_ALWAYS_VISIBLE = builder
                .comment("Always show villager name tags (off = only when looked at; HUD still shows them).")
                .define("nameTagsAlwaysVisible", true);
        EMOTION_PARTICLES = builder
                .comment("Emit particles over mobs feeling a strong emotion.")
                .define("emotionParticles", true);
        builder.pop();

        builder.comment("Census — simulation").push("simulation");
        SOCIAL_ENABLED = builder
                .comment("Master switch for the social systems: gossip, bonds, courtship,",
                        "reflection and births. Off = mobs still remember/feel/flee, but the",
                        "village stops evolving socially (cheapest setting).")
                .define("socialEnabled", true);
        REPRODUCTION_ENABLED = builder
                .comment("Whether bonded couples bear children.")
                .define("reproductionEnabled", true);
        LOCAL_POPULATION_CAP = builder
                .comment("Stop births when this many villagers are nearby (0 disables births).")
                .defineInRange("localPopulationCap", 16, 0, 64);
        REFLECTION_ENABLED = builder
                .comment("Whether mobs distil memories into lasting reflections (grudges, trauma).")
                .define("reflectionEnabled", true);
        builder.pop();

        builder.comment("Census — performance").push("performance");
        REPUTATION_MAX_ENTRIES = builder
                .comment("Max opinions a mob keeps; the weakest are forgotten beyond this.")
                .defineInRange("reputationMaxEntries", 64, 8, 512);
        builder.pop();

        SPEC = builder.build();
    }

    private CensusConfig() {}
}
