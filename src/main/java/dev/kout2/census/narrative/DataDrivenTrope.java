package dev.kout2.census.narrative;

import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.memory.MemoryEntry;
import dev.kout2.census.persona.DerivedTrait;
import dev.kout2.census.persona.Persona;
import dev.kout2.census.registry.ModAttachments;
import dev.kout2.census.reputation.ReputationBook;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link Trope} interpreted from a datapack {@link TropeDefinition}. This is
 * how all tropes ship now — the Avenger is just the bundled reference JSON —
 * so communities can add stories without touching code.
 */
public final class DataDrivenTrope implements Trope {
    private static final double ANNOUNCE_RADIUS_SQR = 48.0 * 48.0;
    private static final float EMOTION_SURGE = 0.8f;

    private final String id;
    private final TropeDefinition definition;
    private final DerivedTrait requiredTrait;   // null = no gate
    private final Emotion addEmotion;           // null = none

    public DataDrivenTrope(String id, TropeDefinition definition) {
        this.id = id;
        this.definition = definition;
        // Resolve names eagerly so a typo in a datapack fails at load, not mid-game.
        this.requiredTrait = definition.requiredTrait()
                .map(s -> DerivedTrait.valueOf(s.toUpperCase())).orElse(null);
        this.addEmotion = definition.effects().addEmotion()
                .map(s -> Emotion.valueOf(s.toUpperCase())).orElse(null);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean matches(LivingEntity mob, long now) {
        return findCulprit(mob).isPresent();
    }

    @Override
    public void fire(LivingEntity mob, long now) {
        findCulprit(mob).ifPresent(culprit -> apply(mob, culprit, now));
    }

    /** The first trigger that matches names the culprit. */
    private Optional<UUID> findCulprit(LivingEntity mob) {
        if (requiredTrait != null
                && !mob.getData(ModAttachments.PERSONA).traits().contains(requiredTrait)) {
            return Optional.empty();
        }
        ReputationBook reputation = mob.getData(ModAttachments.REPUTATION);
        for (TropeDefinition.Trigger trigger : definition.triggers()) {
            Optional<UUID> culprit = trigger.memoryEvent().isPresent()
                    ? matchMemoryTrigger(mob, reputation, trigger)
                    : matchOpinionTrigger(reputation, trigger);
            if (culprit.isPresent()) {
                return culprit;
            }
        }
        return Optional.empty();
    }

    /** A remembered event of the given type whose subject the mob now despises enough. */
    private Optional<UUID> matchMemoryTrigger(LivingEntity mob, ReputationBook reputation,
                                              TropeDefinition.Trigger trigger) {
        for (MemoryEntry entry : mob.getData(ModAttachments.MEMORY).recent(64)) {
            if (entry.type() == trigger.memoryEvent().get() && entry.subject().isPresent()
                    && reputation.opinionOf(entry.subject().get()) <= trigger.maxOpinion()) {
                return entry.subject();
            }
        }
        return Optional.empty();
    }

    /** The mob's most-hated acquaintance, if hated deeply enough. */
    private Optional<UUID> matchOpinionTrigger(ReputationBook reputation,
                                               TropeDefinition.Trigger trigger) {
        UUID worst = null;
        float worstValue = trigger.maxOpinion();
        for (Map.Entry<UUID, Float> opinion : reputation.entries()) {
            if (opinion.getValue() <= worstValue) {
                worstValue = opinion.getValue();
                worst = opinion.getKey();
            }
        }
        return Optional.ofNullable(worst);
    }

    private void apply(LivingEntity mob, UUID culprit, long now) {
        TropeDefinition.Effects effects = definition.effects();
        if (effects.setAvengeTarget()) {
            mob.getData(ModAttachments.NARRATIVE).setAvengeTarget(culprit);
        }
        if (addEmotion != null) {
            EmotionalState state = mob.getData(ModAttachments.EMOTION);
            state.add(addEmotion, EMOTION_SURGE, now);
            mob.setData(ModAttachments.EMOTION, state);
        }
        if (mob.level() instanceof ServerLevel level) {
            effects.announce().ifPresent(key -> announce(level, mob, key));
            if (effects.angryParticles()) {
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        mob.getX(), mob.getEyeY() + 0.3, mob.getZ(),
                        14, 0.4, 0.4, 0.4, 0.0);
            }
        }
    }

    private static void announce(ServerLevel level, LivingEntity mob, String key) {
        Persona persona = mob.getData(ModAttachments.PERSONA);
        Component message = Component.translatable(key, persona.fullName());
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(mob) <= ANNOUNCE_RADIUS_SQR) {
                player.sendSystemMessage(message);
            }
        }
    }
}
