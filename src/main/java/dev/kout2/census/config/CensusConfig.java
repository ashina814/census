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

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Census — world & presentation").push("world");
        GRAVESTONES_ENABLED = builder
                .comment("Place a gravestone where a censused mob dies.")
                .define("gravestonesEnabled", true);
        NAME_TAGS_ALWAYS_VISIBLE = builder
                .comment("Always show villager name tags (off = only when looked at; HUD still shows them).")
                .define("nameTagsAlwaysVisible", true);
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
