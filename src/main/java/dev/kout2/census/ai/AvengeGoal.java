package dev.kout2.census.ai;

import dev.kout2.census.registry.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * The behavioural half of the Avenger trope: the mob pursues the player it has
 * sworn vengeance on (stored in {@code NarrativeState}) and harries them in
 * melee. Registered at higher priority than {@link FleeThreatGoal}, so a sworn
 * avenger advances on its target where it would otherwise flee.
 */
public final class AvengeGoal extends Goal {
    private static final double ACTIVATE_RADIUS = 24.0;
    private static final double GIVE_UP_RADIUS = 32.0;
    private static final double ATTACK_REACH_SQR = 2.2 * 2.2;
    private static final double SPEED = 1.05;
    private static final int ATTACK_COOLDOWN = 20;
    private static final int SCAN_INTERVAL = 10;

    private final PathfinderMob mob;
    private Player target;
    private int attackCooldown;
    private int scanCooldown;

    public AvengeGoal(PathfinderMob mob) {
        this.mob = mob;
        this.scanCooldown = mob.getRandom().nextInt(SCAN_INTERVAL);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (scanCooldown-- > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL;
        Optional<UUID> targetId = mob.getData(ModAttachments.NARRATIVE).avengeTarget();
        if (targetId.isEmpty()) {
            return false;
        }
        Player player = mob.level().getPlayerByUUID(targetId.get());
        if (player == null || !player.isAlive()
                || player.distanceToSqr(mob) > ACTIVATE_RADIUS * ACTIVATE_RADIUS) {
            return false;
        }
        this.target = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive()
                && target.distanceToSqr(mob) <= GIVE_UP_RADIUS * GIVE_UP_RADIUS
                && mob.getData(ModAttachments.NARRATIVE).avengeTarget()
                        .filter(id -> id.equals(target.getUUID())).isPresent();
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(target, SPEED);
    }

    @Override
    public void stop() {
        this.target = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target);
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (mob.distanceToSqr(target) > ATTACK_REACH_SQR) {
            if (mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(target, SPEED);
            }
        } else if (attackCooldown <= 0) {
            strike();
            attackCooldown = ATTACK_COOLDOWN;
        }
    }

    private void strike() {
        mob.swing(mob.getUsedItemHand());
        target.hurt(mob.damageSources().mobAttack(mob), 2.0f);
        // Shove the target away from the avenger.
        target.knockback(0.4, mob.getX() - target.getX(), mob.getZ() - target.getZ());
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    mob.getX(), mob.getEyeY() + 0.3, mob.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        }
    }
}
