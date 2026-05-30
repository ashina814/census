package dev.kout2.census.narrative.tropes;

import dev.kout2.census.Census;
import dev.kout2.census.memory.EventType;
import dev.kout2.census.memory.MemoryEntry;
import dev.kout2.census.narrative.Trope;
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
 * The Avenger: a vengeful mob whose blood relative was killed marks the killer
 * and stops running from them — it confronts and harries them instead (see
 * {@code AvengeGoal}). Fires once, with a public vow.
 */
public final class AvengerTrope implements Trope {
    /** Killing a relative needs only moderate hatred to provoke vengeance. */
    private static final float KIN_HATE_THRESHOLD = -20f;
    /** A purely personal grudge must run much deeper to make a villager fight back. */
    private static final float PERSONAL_HATE_THRESHOLD = -60f;
    private static final double ANNOUNCE_RADIUS_SQR = 48.0 * 48.0;

    @Override
    public String id() {
        return "avenger";
    }

    @Override
    public boolean matches(LivingEntity mob, long now) {
        return findCulprit(mob).isPresent();
    }

    @Override
    public void fire(LivingEntity mob, long now) {
        findCulprit(mob).ifPresent(culprit -> {
            mob.getData(ModAttachments.NARRATIVE).setAvengeTarget(culprit);
            if (mob.level() instanceof ServerLevel level) {
                announce(level, mob);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        mob.getX(), mob.getEyeY() + 0.3, mob.getZ(),
                        14, 0.4, 0.4, 0.4, 0.0);
            }
        });
    }

    /**
     * Who this vengeful mob will hunt, if anyone: the killer of a relative it
     * has come to hate, or — failing that — anyone it hates deeply enough from
     * personal torment.
     */
    private Optional<UUID> findCulprit(LivingEntity mob) {
        if (!Census.isCensused(mob)
                || !mob.getData(ModAttachments.PERSONA).traits().contains(DerivedTrait.VENGEFUL)) {
            return Optional.empty();
        }
        ReputationBook reputation = mob.getData(ModAttachments.REPUTATION);

        // (a) A killed relative — the classic revenge.
        for (MemoryEntry entry : mob.getData(ModAttachments.MEMORY).recent(64)) {
            if (entry.type() == EventType.RELATIVE_KILLED && entry.subject().isPresent()
                    && reputation.opinionOf(entry.subject().get()) <= KIN_HATE_THRESHOLD) {
                return Optional.of(entry.subject().get());
            }
        }

        // (b) A personal tormentor it has come to hate deeply.
        UUID mostHated = null;
        float worst = 0f;
        for (Map.Entry<UUID, Float> opinion : reputation.entries()) {
            if (opinion.getValue() < worst) {
                worst = opinion.getValue();
                mostHated = opinion.getKey();
            }
        }
        return worst <= PERSONAL_HATE_THRESHOLD ? Optional.ofNullable(mostHated) : Optional.empty();
    }

    private void announce(ServerLevel level, LivingEntity mob) {
        Persona persona = mob.getData(ModAttachments.PERSONA);
        Component message = Component.translatable("census.trope.avenger", persona.fullName());
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(mob) <= ANNOUNCE_RADIUS_SQR) {
                player.sendSystemMessage(message);
            }
        }
    }
}
