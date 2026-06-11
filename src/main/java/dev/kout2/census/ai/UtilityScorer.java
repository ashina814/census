package dev.kout2.census.ai;

import dev.kout2.census.Census;
import dev.kout2.census.ai.utility.ActionDefinition;
import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.persona.BigFive;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.ReputationBook;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Desire formulas for the utility AI — how much a mob wants each behaviour
 * right now, blending reputation, acute emotion and personality. The shape of
 * each formula is code; its coefficients and threshold come from the datapack
 * {@link ActionDefinition} so packs can retune behaviour without a rebuild.
 */
public final class UtilityScorer {
    private UtilityScorer() {}

    /**
     * Desire to keep away from {@code player}, in [0, ~1.2]. Grudge-driven:
     * standing dislike dominates, acute fear is secondary (vanilla already
     * handles panic-on-hit). Timidity (neurotic, introverted) lowers the bar.
     *
     * Params: {@code dislike_weight} (default 1.0), {@code fear_weight} (0.35).
     */
    public static float fleeDesire(LivingEntity mob, Player player, ActionDefinition action) {
        if (!Census.isCensused(mob)) {
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

        return (dislike * action.param("dislike_weight", 1.0f)
                + fear * action.param("fear_weight", 0.35f)) * timidity;
    }

    /**
     * Desire to bring {@code player} a present, in [0, ~1.2]. Zero until the
     * mob's opinion reaches {@code min_opinion}; above that it scales with the
     * opinion, amplified for extraverted and agreeable personalities — the
     * sociable and kind give gladly, the reclusive rarely.
     *
     * Params: {@code min_opinion} (default 50).
     */
    public static float giftDesire(LivingEntity mob, Player player, ActionDefinition action) {
        if (!Census.isCensused(mob)) {
            return 0f;
        }
        float opinion = mob.getData(ModAttachments.REPUTATION).opinionOf(player.getUUID());
        if (opinion < action.param("min_opinion", 50f)) {
            return 0f;
        }
        BigFive b = mob.getData(ModAttachments.PERSONA).personality();
        float warmth = 0.5f + b.extraversion() * 0.35f + b.agreeableness() * 0.35f;
        return (opinion / 100f) * warmth;
    }
}
