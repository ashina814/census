package dev.kout2.census.event;

import dev.kout2.census.Census;
import dev.kout2.census.CensusMod;
import dev.kout2.census.emotion.Emotion;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.registry.ModAttachments;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Makes feelings visible: a censused mob in the grip of a strong emotion puffs
 * the matching particle over its head. Bounded — it only looks at mobs near a
 * player, once a second — so it scales with what's actually on screen.
 */
@EventBusSubscriber(modid = CensusMod.MODID)
public final class EmotionParticleHandler {
    private static final int INTERVAL = 20;          // ~1s
    private static final double RADIUS = 24.0;
    private static final float SHOW_THRESHOLD = 0.45f;
    private static final float CHANCE = 0.5f;

    private EmotionParticleHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        if (now % INTERVAL != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                AABB box = player.getBoundingBox().inflate(RADIUS);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, box, Census::isCensused)) {
                    showEmotion(level, mob, now);
                }
            }
        }
    }

    private static void showEmotion(ServerLevel level, LivingEntity mob, long now) {
        EmotionalState emotion = mob.getData(ModAttachments.EMOTION);
        emotion.decayTo(now);
        Emotion dominant = emotion.dominant();
        if (dominant == null || emotion.intensity(dominant) < SHOW_THRESHOLD) {
            return;
        }
        ParticleOptions particle = particleFor(dominant);
        if (particle == null || mob.getRandom().nextFloat() > CHANCE) {
            return;
        }
        level.sendParticles(particle, mob.getX(), mob.getEyeY() + 0.5, mob.getZ(),
                1, 0.2, 0.1, 0.2, 0.0);
    }

    private static ParticleOptions particleFor(Emotion emotion) {
        return switch (emotion) {
            case ANGER -> ParticleTypes.ANGRY_VILLAGER;
            case JOY, HOPE, PRIDE -> ParticleTypes.HAPPY_VILLAGER;
            case GRATITUDE -> ParticleTypes.HEART;
            case FEAR -> ParticleTypes.SMOKE;
            case DISTRESS, SHAME -> null;
        };
    }
}
