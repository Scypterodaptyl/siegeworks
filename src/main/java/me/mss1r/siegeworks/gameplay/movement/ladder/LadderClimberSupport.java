package me.mss1r.siegeworks.gameplay.movement.ladder;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import me.mss1r.axiomata.collision.system.VirtualPlatformSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class LadderClimberSupport {
    private static final double LADDER_HALF_WIDTH = 7.5D / 16.0D;
    private static final double WALK_EXTENSION = 4.0D / 16.0D;
    private static final double PLATFORM_Y_TOLERANCE = 1.15D;
    private static final double STEEP_UPWARD_SPEED_FACTOR = 0.12D;
    private static final double STEEP_DOWNWARD_SPEED_FACTOR = 0.65D;
    private static final double STEEP_SIDE_SPEED_FACTOR = 0.55D;
    private static final double STEEP_MAX_UP_LOCAL_Y_PER_TICK = 0.085D;
    private static final double FLAT_MAX_UP_LOCAL_Y_PER_TICK = 0.16D;
    private static final double STEP_SOUND_MIN_LOCAL_Y_DELTA = 0.006D;
    private static final double CLIMB_POSE_MIN_VERTICAL_FACTOR = 0.60D;
    private static final double GUIDED_BACK_OFFSET = 3.0D / 16.0D;
    private static final int STEP_SOUND_INTERVAL_TICKS = 12;
    private static final int CLIMB_STATE_TTL_TICKS = 20;

    private final Host host;
    private final Map<Integer, ClimbState> climbStates = new HashMap<>();

    public LadderClimberSupport(Host host) {
        this.host = host;
    }

    public void tick() {
        cleanupClimbStates();
        if (Math.abs(host.leanProgress()) < 0.08F) {
            return;
        }

        Entity ladder = host.ladder();
        double length = host.ladderLength() + WALK_EXTENSION + 1.0D;
        AABB searchBox = ladder.getBoundingBox().inflate(length, host.ladderLength() + 1.0D, length);
        host.level().getEntities(ladder, searchBox, this::canStandOnLadder).forEach(this::supportOnLadder);
    }

    private boolean canStandOnLadder(Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof AbstractSiegeEntity)
                && entity.isAlive()
                && !entity.isPassenger()
                && !(entity instanceof Player player && player.isSpectator());
    }

    private void supportOnLadder(Entity entity) {
        LocalPosition local = host.localPosition(entity.position());
        double angle = host.leanAngleRadians();
        double sin = Math.sin(angle);
        if (Math.abs(sin) < 0.08D) {
            return;
        }

        boolean guidedClimb = Math.abs(Math.cos(angle)) >= CLIMB_POSE_MIN_VERTICAL_FACTOR;
        ClimbState previous = climbStates.get(entity.getId());
        boolean keptAtGuidedOffset = guidedClimb
                && (host.automatedClimberActive(entity.getUUID())
                || previous != null && previous.guidedClimb() && host.gameTick() - previous.tick() <= 2);
        double forwardOffset = keptAtGuidedOffset
                ? uphillDirection(angle) * GUIDED_BACK_OFFSET
                : 0.0D;
        double halfWidth = LADDER_HALF_WIDTH + entity.getBbWidth() * 0.5D;
        double localY = (local.forward() + forwardOffset) / sin;
        double end = host.ladderLength() + WALK_EXTENSION + entity.getBbWidth() * 0.5D;
        if (Math.abs(local.side()) > halfWidth || localY < 0.0D || localY > end) {
            return;
        }

        ClimbStep climbStep = limitClimbStep(entity, local.side(), localY, angle, guidedClimb);
        localY = climbStep.localY();
        double platformY = host.platformY(localY);
        Vec3 velocity = entity.getDeltaMovement();
        AABB entityBox = entity.getBoundingBox();
        boolean closeToTop = entity.getY() >= platformY - PLATFORM_Y_TOLERANCE
                && entity.getY() <= platformY + 0.45D;
        boolean intersectsPlatform = entityBox.minY <= platformY + 0.25D
                && entityBox.maxY >= platformY - 0.10D;
        if ((!closeToTop && !intersectsPlatform) || velocity.y > 0.35D) {
            return;
        }

        entity.setPos(climbStep.position().x, platformY, climbStep.position().z);
        entity.setDeltaMovement(slowMovement(velocity, angle, guidedClimb));
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
        VirtualPlatformSupport.markSupported(entity);
        double handsY = entity.getY() + entity.getBbHeight() * HAND_HEIGHT_FRACTION;
        if (guidedClimb && handsY <= host.platformY(host.ladderLength())) {
            StructureClimbingSystem.markClimbPoseAttachment(
                    entity, host.ladder(), (float) angle, host.uphillYawDegrees(angle));
        }
        playStepSound(entity, climbStep.climbedLocalY());
    }

    private ClimbStep limitClimbStep(Entity entity, double side, double localY, double angle,
                                     boolean guidedClimb) {
        ClimbState previous = climbStates.get(entity.getId());
        double adjustedLocalY = localY;
        double climbedLocalY = 0.0D;
        if (previous != null && host.gameTick() - previous.tick() <= 2) {
            double delta = localY - previous.localY();
            if (delta > 0.0D) {
                double maxDelta = maxClimbLocalYDelta(angle);
                adjustedLocalY = previous.localY() + Math.min(delta, maxDelta);
                climbedLocalY = adjustedLocalY - previous.localY();
            }
        }

        Vec3 position = entity.position();
        double offsetLocalY = adjustedLocalY - localY;
        if (Math.abs(offsetLocalY) > 1.0E-5D) {
            Vec3 forward = host.forwardVector();
            double offsetForward = offsetLocalY * Math.sin(angle);
            position = new Vec3(
                    entity.getX() + forward.x * offsetForward,
                    entity.getY(),
                    entity.getZ() + forward.z * offsetForward);
        }
        if (guidedClimb) {
            Vec3 right = host.rightVector();
            position = position.subtract(right.scale(side));
            Vec3 forward = host.forwardVector();
            double desiredForward = adjustedLocalY * Math.sin(angle)
                    - uphillDirection(angle) * GUIDED_BACK_OFFSET;
            double currentForward = host.localPosition(position).forward();
            position = position.add(forward.scale(desiredForward - currentForward));
            side = 0.0D;
        }

        climbStates.put(entity.getId(), new ClimbState(adjustedLocalY, host.gameTick(), guidedClimb));
        return new ClimbStep(adjustedLocalY,
                Mth.clamp(side, -LADDER_HALF_WIDTH, LADDER_HALF_WIDTH), position, climbedLocalY);
    }

    private static double maxClimbLocalYDelta(double angle) {
        return Mth.lerp(flatRelief(angle),
                STEEP_MAX_UP_LOCAL_Y_PER_TICK,
                FLAT_MAX_UP_LOCAL_Y_PER_TICK);
    }

    private Vec3 slowMovement(Vec3 velocity, double angle, boolean guidedClimb) {
        Vec3 forward = host.forwardVector();
        double forwardSpeed = velocity.x * forward.x + velocity.z * forward.z;
        double sideX = velocity.x - forward.x * forwardSpeed;
        double sideZ = velocity.z - forward.z * forwardSpeed;
        double uphillSpeed = forwardSpeed * uphillDirection(angle);
        double flatRelief = flatRelief(angle);
        double uphillFactor = Mth.lerp(flatRelief, STEEP_UPWARD_SPEED_FACTOR, 1.0D);
        double downhillFactor = Mth.lerp(flatRelief, STEEP_DOWNWARD_SPEED_FACTOR, 1.0D);
        double sideFactor = guidedClimb
                ? 0.0D
                : Mth.lerp(flatRelief, STEEP_SIDE_SPEED_FACTOR, 1.0D);
        double forwardFactor = uphillSpeed > 0.0D ? uphillFactor : downhillFactor;

        return new Vec3(
                forward.x * forwardSpeed * forwardFactor + sideX * sideFactor,
                0.0D,
                forward.z * forwardSpeed * forwardFactor + sideZ * sideFactor);
    }

    private static double flatRelief(double angle) {
        double flatness = Mth.clamp(Math.abs(Math.sin(angle)), 0.0D, 1.0D);
        double squaredFlatness = flatness * flatness;
        return squaredFlatness * squaredFlatness;
    }

    private static double uphillDirection(double angle) {
        double direction = Math.signum(Math.sin(angle));
        return direction == 0.0D ? 1.0D : direction;
    }

    private void playStepSound(Entity entity, double climbedLocalY) {
        if (!(host.level() instanceof ServerLevel serverLevel)
                || entity.tickCount % STEP_SOUND_INTERVAL_TICKS != 0
                || climbedLocalY <= STEP_SOUND_MIN_LOCAL_Y_DELTA) {
            return;
        }

        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.LADDER_STEP, SoundSource.PLAYERS, 0.65F, 0.9F + host.randomFloat() * 0.2F);
    }

    private void cleanupClimbStates() {
        if (host.gameTick() % CLIMB_STATE_TTL_TICKS != 0) {
            return;
        }

        Iterator<Map.Entry<Integer, ClimbState>> iterator = climbStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ClimbState> entry = iterator.next();
            if (host.gameTick() - entry.getValue().tick() > CLIMB_STATE_TTL_TICKS) {
                iterator.remove();
            }
        }
    }

    public record LocalPosition(double side, double forward) {
    }

    private record ClimbState(double localY, int tick, boolean guidedClimb) {
    }

    private static final double HAND_HEIGHT_FRACTION = 0.72D;

    private record ClimbStep(double localY, double side, Vec3 position, double climbedLocalY) {
    }

    public interface Host {
        AbstractSiegeEntity ladder();

        Level level();

        int gameTick();

        float leanProgress();

        double leanAngleRadians();

        double ladderLength();

        LocalPosition localPosition(Vec3 worldPosition);

        Vec3 forwardVector();

        Vec3 rightVector();

        double platformY(double localY);

        float uphillYawDegrees(double angle);

        boolean automatedClimberActive(UUID entityUuid);

        float randomFloat();
    }
}
