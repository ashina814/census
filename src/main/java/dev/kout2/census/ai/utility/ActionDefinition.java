package dev.kout2.census.ai.utility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * The JSON shape of a utility action — what datapack authors write under
 * {@code data/<namespace>/census_actions/*.json}. Binds a code-side
 * {@link ActionBehavior} to a desire threshold and named tuning parameters.
 *
 * Example ({@code data/census/census_actions/flee.json}):
 * <pre>{@code
 * {
 *   "behavior": "flee",
 *   "threshold": 0.35,
 *   "params": { "dislike_weight": 1.0, "fear_weight": 0.35 }
 * }
 * }</pre>
 *
 * @param behavior  which primitive runs
 * @param threshold desire level at which the behaviour activates
 * @param params    behaviour-specific named coefficients
 * @param item      the item given by GIFT (ignored by other behaviours)
 * @param cooldown  ticks between activations per mob (0 = none)
 */
public record ActionDefinition(
        ActionBehavior behavior,
        float threshold,
        Map<String, Float> params,
        Item item,
        int cooldown
) {
    public static final Codec<ActionDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ActionBehavior.CODEC.fieldOf("behavior").forGetter(ActionDefinition::behavior),
            Codec.FLOAT.fieldOf("threshold").forGetter(ActionDefinition::threshold),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("params", Map.of())
                    .forGetter(ActionDefinition::params),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item", Items.EMERALD)
                    .forGetter(ActionDefinition::item),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(ActionDefinition::cooldown)
    ).apply(inst, ActionDefinition::new));

    public float param(String key, float fallback) {
        return params.getOrDefault(key, fallback);
    }
}
