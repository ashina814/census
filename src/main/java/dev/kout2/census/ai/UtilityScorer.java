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
    /** Above this, a mob will keep its distance from the player. */
    public static final float FLEE_THRESHOLD = 0.35f;

    private UtilityScorer() {}

    /**
     * How much {@code mob} wants to keep away from {@code player}, in [0, ~1.2].
     *
     * Deliberately <b>grudge-driven</b>: standing dislike (reputation) dominates,
     * with acute fear only a secondary push. Vanilla already makes villagers
     * panic the instant they're hit; Census's distinctive behaviour is the mob
     * that avoids someone it has <i>come to hate</i> even when not under attack.
     * Timidity (neurotic, introverted) lowers the bar; brave mobs hold their
     * ground until the grudge runs deep.
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

        BigFive b = persona.personality();
        float timidity = Math.clamp(
                0.5f + b.neuroticism() * 0.6f - b.extraversion() * 0.2f, 0.2f, 1.1f);

        return (dislike * 1.0f + fear * 0.35f) * timidity;
    }
}
