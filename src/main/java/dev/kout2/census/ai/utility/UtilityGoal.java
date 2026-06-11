package dev.kout2.census.ai.utility;

import dev.kout2.census.ai.UtilityScorer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * The utility-AI selector: one goal per mob that, on a throttled scan, scores
 * every datapack-loaded {@link ActionDefinition} against the nearest player
 * and runs the most-desired one above its threshold. Replaces the old
 * hardcoded flee goal — which behaviours exist and when they trigger is now
 * data ({@code census_actions/*.json}), live-reloadable.
 */
public final class UtilityGoal extends Goal {
    private static final double SCAN_RADIUS = 12.0;
    private static final int SCAN_INTERVAL = 10;

    // Flee tuning (movement itself, not the desire — that's in the JSON).
    private static final int FLEE_DISTANCE = 16;
    private static final int FLEE_Y_RANGE = 7;
    private static final double FLEE_SPEED = 0.85;

    // Gift movement/delivery.
    private static final double GIFT_SPEED = 0.9;
    private static final double GIFT_REACH_SQR = 2.5 * 2.5;
    private static final double GIFT_GIVE_UP_SQR = 32.0 * 32.0;

    private final PathfinderMob mob;
    private final Map<ActionBehavior, Long> lastRun = new EnumMap<>(ActionBehavior.class);
    private int scanCooldown;

    private ActionDefinition active;
    private Player target;
    private Vec3 fleeTarget;
    private boolean delivered;

    public UtilityGoal(PathfinderMob mob) {
        this.mob = mob;
        this.scanCooldown = mob.getRandom().nextInt(SCAN_INTERVAL); // stagger across mobs
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (scanCooldown-- > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL;
        Player player = mob.level().getNearestPlayer(mob, SCAN_RADIUS);
        if (player == null) {
            return false;
        }
        long now = mob.level().getGameTime();

        ActionDefinition best = null;
        float bestDesire = 0f;
        for (ActionDefinition action : ActionLoader.actions()) {
            if (now - lastRun.getOrDefault(action.behavior(), Long.MIN_VALUE) < action.cooldown()) {
                continue;
            }
            float desire = desire(action, player);
            if (desire >= action.threshold() && desire > bestDesire) {
                bestDesire = desire;
                best = action;
            }
        }
        if (best == null) {
            return false;
        }
        return prepare(best, player);
    }

    private float desire(ActionDefinition action, Player player) {
        return switch (action.behavior()) {
            case FLEE -> UtilityScorer.fleeDesire(mob, player, action);
            case GIFT -> UtilityScorer.giftDesire(mob, player, action);
        };
    }

    /** Behaviour-specific feasibility (e.g. an actual escape route exists). */
    private boolean prepare(ActionDefinition action, Player player) {
        if (action.behavior() == ActionBehavior.FLEE) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, FLEE_DISTANCE, FLEE_Y_RANGE, player.position());
            if (away == null
                    || player.distanceToSqr(away.x, away.y, away.z) <= player.distanceToSqr(mob)) {
                return false; // no escape route that actually increases distance
            }
            this.fleeTarget = away;
        }
        this.active = action;
        this.target = player;
        this.delivered = false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (active == null || target == null || !target.isAlive()) {
            return false;
        }
        return switch (active.behavior()) {
            case FLEE -> !mob.getNavigation().isDone()
                    && UtilityScorer.fleeDesire(mob, target, active) >= active.threshold() * 0.5f;
            case GIFT -> !delivered && target.distanceToSqr(mob) <= GIFT_GIVE_UP_SQR;
        };
    }

    @Override
    public void start() {
        switch (active.behavior()) {
            case FLEE -> mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, FLEE_SPEED);
            case GIFT -> mob.getNavigation().moveTo(target, GIFT_SPEED);
        }
    }

    @Override
    public void stop() {
        this.active = null;
        this.target = null;
        this.fleeTarget = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (active == null || target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target);
        switch (active.behavior()) {
            case FLEE -> {
                if (mob.getNavigation().isDone()) {
                    Vec3 away = DefaultRandomPos.getPosAway(mob, FLEE_DISTANCE, FLEE_Y_RANGE, target.position());
                    if (away != null) {
                        mob.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
                    }
                }
            }
            case GIFT -> {
                if (mob.distanceToSqr(target) <= GIFT_REACH_SQR) {
                    deliverGift();
                } else if (mob.getNavigation().isDone()) {
                    mob.getNavigation().moveTo(target, GIFT_SPEED);
                }
            }
        }
    }

    /** Tosses the configured item toward the player, with a little flourish. */
    private void deliverGift() {
        mob.swing(mob.getUsedItemHand());
        Vec3 toward = target.position().subtract(mob.position()).normalize().scale(0.25);
        ItemEntity present = new ItemEntity(mob.level(),
                mob.getX(), mob.getEyeY() - 0.2, mob.getZ(),
                new ItemStack(active.item()));
        present.setDeltaMovement(toward.x, 0.2, toward.z);
        mob.level().addFreshEntity(present);
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART,
                    mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
        }
        lastRun.put(ActionBehavior.GIFT, mob.level().getGameTime());
        delivered = true;
    }
}
