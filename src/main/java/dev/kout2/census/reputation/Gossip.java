package dev.kout2.census.reputation;

import dev.kout2.census.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Rumour propagation: a speaker shares its strong opinions with a listener.
 *
 * Shared subjects converge (the listener drifts toward the speaker's view);
 * unknown subjects are introduced as damped hearsay (a rumour is believed less
 * than a firsthand experience). Run repeatedly across chance meetings, this
 * spreads a reputation through a village without anyone needing to have
 * witnessed the original act.
 */
public final class Gossip {
    /** Only opinions at least this strong are worth mentioning. */
    private static final float MENTION_THRESHOLD = 8f;
    /** Listener moves this fraction toward the speaker on a shared subject. */
    private static final float CONVERGENCE = 0.10f;
    /** Strength at which a brand-new rumour is adopted. */
    private static final float HEARSAY = 0.30f;

    private Gossip() {}

    /** One-directional: {@code speaker} tells {@code listener} the news. */
    public static void exchange(LivingEntity speaker, LivingEntity listener, long now) {
        ReputationBook speakerBook = speaker.getData(ModAttachments.REPUTATION);
        ReputationBook listenerBook = listener.getData(ModAttachments.REPUTATION);
        UUID listenerId = listener.getUUID();

        for (Map.Entry<UUID, Float> opinion : speakerBook.entries()) {
            UUID subject = opinion.getKey();
            float speakerView = opinion.getValue();
            if (Math.abs(speakerView) < MENTION_THRESHOLD || subject.equals(listenerId)) {
                continue; // not worth gossiping, or it's about the listener themselves
            }
            if (listenerBook.knows(subject)) {
                float listenerView = listenerBook.opinionOf(subject);
                listenerBook.set(subject, listenerView + (speakerView - listenerView) * CONVERGENCE);
            } else {
                listenerBook.set(subject, speakerView * HEARSAY);
            }
        }
    }
}
