package dev.kout2.census.ai;

import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.persona.BigFive;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.ReputationBook;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Utility AI scoring — turns a mob's inner state into a number per candidate
 * action. Phase 6 ships one action (fleeing a threatening player); the same
 * pattern extends to approach/avoid/seek behaviours later.
 *
 * Utility AI is chosen over GOAP here: there is no multi-step planning to do,
 * just a continuous "how much do I want to do X right now" that blends emotion,
 * personality and reputation — exactly what a weighted score expresses.
 */
public final class UtilityScorer {
    /** Above this, a mob will break off and flee the player. */
    public static final float FLEE_THRESHOLD = 0.30f;

    private UtilityScorer() {}

    /**
     * How much {@code mob} wants to flee {@code player} right now, in [0, ~1.5].
     * Driven by standing dislike, acute fear/anger, and timidity (neurotic and
     * introverted mobs spook more easily; brave ones stand their ground).
     */
    public static float fleeDesire(LivingEntity mob, Player player) {
        if (!mob.hasData(ModAttachments.PERSONA)) {
            return 0f;
        }
        Persona persona = mob.getData(ModAttachments.PERSONA);
        ReputationBook reputation = mob.getData(ModAttachments.REPUTATION);
        EmotionalState emotion = mob.getData(ModAttachments.EMOTION);
        emotion.decayTo(mob.level().getGameTime());

        float opinion = reputation.opinionOf(player.getUUID());   // -100..100
        float dislike = Math.max(0f, -opinion) / 100f;            // 0..1
        float fear = emotion.intensity(Emotion.FEAR);
        float anger = emotion.intensity(Emotion.ANGER);

        BigFive b = persona.personality();
        float timidity = Math.max(0.2f,
                0.5f + b.neuroticism() * 0.6f - b.extraversion() * 0.2f);

        return (dislike * 0.7f + fear * 0.7f + anger * 0.2f) * timidity;
    }
}
