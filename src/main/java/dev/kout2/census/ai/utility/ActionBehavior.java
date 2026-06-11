package dev.kout2.census.ai.utility;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The behaviour primitives Census knows how to execute. Datapack actions
 * (see {@link ActionDefinition}) bind one of these to a desire formula and a
 * threshold — the primitive is code, everything about when it runs is data.
 */
public enum ActionBehavior implements StringRepresentable {
    /** Keep away from a player the mob has come to fear or hate. */
    FLEE("flee"),
    /** Bring a present to a player the mob adores. */
    GIFT("gift");

    public static final Codec<ActionBehavior> CODEC = StringRepresentable.fromEnum(ActionBehavior::values);

    private final String name;

    ActionBehavior(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
