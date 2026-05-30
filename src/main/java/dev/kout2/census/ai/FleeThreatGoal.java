package dev.kout2.census.ai;

import dev.kout2.census.registry.ModAttachments;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A utility-scored flee goal: the mob backs away from a nearby player it has
 * come to fear or hate. This is the first behaviour where Census's inner state
 * (reputation + emotion + personality, via {@link UtilityScorer}) becomes
 * visible action.
 *
 * Injected into villagers' existing {@code goalSelector} (which already runs
 * their trade goals), so it coexists with the brain-based AI.
 */
public final class FleeThreatGoal extends Goal {
    private static final double SCAN_RADIUS = 12.0;
    private static final int FLEE_DISTANCE = 16;
    private static final int FLEE_Y_RANGE = 7;
    private static final double FLEE_SPEED = 0.85;

    /** Only re-scan every this many ticks — keeps per-mob cost low. */
    private static final int SCAN_INTERVAL = 10;

    private final PathfinderMob mob;
    private Player threat;
    private Vec3 fleeTarget;
    private int scanCooldown;

    public FleeThreatGoal(PathfinderMob mob) {
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
        if (!mob.hasData(ModAttachments.PERSONA)) {
            return false;
        }
        Player nearest = mob.level().getNearestPlayer(mob, SCAN_RADIUS);
        if (nearest == null || UtilityScorer.fleeDesire(mob, nearest) < UtilityScorer.FLEE_THRESHOLD) {
            return false;
        }
        Vec3 away = DefaultRandomPos.getPosAway(mob, FLEE_DISTANCE, FLEE_Y_RANGE, nearest.position());
        if (away == null || farther(nearest, away) <= nearest.distanceToSqr(mob)) {
            return false; // no escape route that actually increases distance
        }
        this.threat = nearest;
        this.fleeTarget = away;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return threat != null
                && !mob.getNavigation().isDone()
                && UtilityScorer.fleeDesire(mob, threat) >= UtilityScorer.FLEE_THRESHOLD * 0.5f;
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, FLEE_SPEED);
    }

    @Override
    public void stop() {
        this.threat = null;
        this.fleeTarget = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (threat != null) {
            mob.getLookControl().setLookAt(threat);
            if (mob.getNavigation().isDone()) {
                Vec3 away = DefaultRandomPos.getPosAway(mob, FLEE_DISTANCE, FLEE_Y_RANGE, threat.position());
                if (away != null) {
                    mob.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
                }
            }
        }
    }

    private double farther(Player from, Vec3 pos) {
        return from.distanceToSqr(pos.x, pos.y, pos.z);
    }
}
