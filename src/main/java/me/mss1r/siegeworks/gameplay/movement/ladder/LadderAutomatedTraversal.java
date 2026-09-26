package me.mss1r.siegeworks.gameplay.movement.ladder;

import me.mss1r.siegeworks.api.SiegeClimbableControl.ClimbResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LadderAutomatedTraversal {
    private static final double CLIMB_LOCAL_Y_PER_TICK = 0.075D;
    private static final double TRANSITION_DISTANCE_PER_TICK = 0.11D;
    private static final double CLIMBER_GAP = 1.35D;
    private static final double APPROACH_OFFSET = 1.25D;
    private static final double EXIT_OFFSET = 0.9D;
    private static final double LANDING_MIN_DEPTH = 0.2D;
    private static final double LANDING_MAX_DEPTH = 3.75D;
    private static final double LANDING_SAMPLE_STEP = 0.25D;
    private static final double WAITING_SPACING = 0.9D;
    private static final int CLIMBER_TIMEOUT_TICKS = 40;
    private static final double STAGING_ARRIVAL_DISTANCE_SQR = 0.64D;
    private static final double WALK_EXTENSION = 4.0D / 16.0D;
    private static final double GUIDED_BACK_OFFSET = 3.0D / 16.0D;
    private static final double CLIMB_POSE_MIN_VERTICAL_FACTOR = 0.60D;
    private static final double[] SIDE_OFFSETS = {0.0D, -0.9D, 0.9D, -1.8D, 1.8D};

    private final Host host;
    private final Map<UUID, Climber> climbers = new LinkedHashMap<>();
    private final Deque<UUID> queue = new ArrayDeque<>();
    private final Set<UUID> activeClimbers = new LinkedHashSet<>();
    private int sequence;

    public LadderAutomatedTraversal(Host host) {
        this.host = host;
    }

    public int activeClimberCount() {
        return activeClimbers.size();
    }

    public boolean hasClimbers() {
        return !climbers.isEmpty();
    }

    public boolean isActive(UUID entityUuid) {
        return activeClimbers.contains(entityUuid);
    }

    public Vec3 bottomApproach() {
        return host.ladder().position().subtract(host.uphillVector().scale(APPROACH_OFFSET))
                .add(0.0D, 0.05D, 0.0D);
    }

    public Vec3 topExit() {
        double localY = host.ladderLength() + WALK_EXTENSION;
        Vec3 top = host.ladderWorldPoint(localY);
        Vec3 landing = findTopLanding(top, 0, null, false);
        return landing == null
                ? top.add(host.uphillVector().scale(EXIT_OFFSET)).add(0.0D, 0.05D, 0.0D)
                : landing;
    }

    public Vec3 queuePosition(LivingEntity climber, boolean upward) {
        if (!canUse(climber)) {
            return upward ? bottomApproach() : topExit();
        }

        Climber state = getOrCreate(climber, upward);
        return state == null
                ? upward ? bottomApproach() : topExit()
                : waitingPosition(upward, state.stagingIndex);
    }

    public ClimbResult advance(LivingEntity climber, boolean upward) {
        if (!canUse(climber)) {
            cancel(climber);
            return ClimbResult.FAILED;
        }

        UUID uuid = climber.getUUID();
        Climber state = getOrCreate(climber, upward);
        if (state == null) {
            return ClimbResult.FAILED;
        }
        state.ready = state.phase != Phase.WAITING || hasArrived(climber, state);
        state.lastRequestTick = host.gameTick();
        promote();

        if (!activeClimbers.contains(uuid)) {
            moveWaitingClimber(climber, state);
            return ClimbResult.WAITING;
        }
        return advanceActive(climber, state);
    }

    public void cancel(LivingEntity climber) {
        UUID uuid = climber.getUUID();
        climbers.remove(uuid);
        if (queue.remove(uuid)) {
            reindexQueue();
        }
        if (activeClimbers.remove(uuid)) {
            promote();
        }
    }

    public void tick() {
        Iterator<Map.Entry<UUID, Climber>> iterator = climbers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Climber> entry = iterator.next();
            Entity entity = host.level() instanceof ServerLevel serverLevel
                    ? serverLevel.getEntity(entry.getKey()) : null;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || host.gameTick() - entry.getValue().lastRequestTick > CLIMBER_TIMEOUT_TICKS) {
                iterator.remove();
                queue.remove(entry.getKey());
                activeClimbers.remove(entry.getKey());
            }
        }
        reindexQueue();
        promote();
    }

    private boolean canUse(LivingEntity climber) {
        return host.ready() && climber.isAlive() && climber.level() == host.level();
    }

    private Climber getOrCreate(LivingEntity climber, boolean upward) {
        UUID uuid = climber.getUUID();
        Climber state = climbers.get(uuid);
        if (state != null && state.upward != upward) {
            cancel(climber);
            return null;
        }
        if (state == null) {
            state = new Climber(upward, sequence++, queue.size(), host.gameTick());
            climbers.put(uuid, state);
            queue.addLast(uuid);
            reindexQueue();
        }
        state.lastRequestTick = host.gameTick();
        return state;
    }

    private ClimbResult advanceActive(LivingEntity climber, Climber state) {
        double end = host.ladderLength() + WALK_EXTENSION;
        if (state.phase == Phase.ENTERING) {
            Vec3 ladderEntry = ladderPoint(state.upward ? 0.0D : end);
            state.transitionProgress = advanceTransition(
                    state.transitionStart, ladderEntry, state.transitionProgress);
            place(climber, state.transitionStart.lerp(ladderEntry, state.transitionProgress));
            if (state.transitionProgress >= 1.0D) {
                state.phase = Phase.TRAVERSING;
                state.ladderProgress = state.upward ? 0.0D : end;
            }
            return ClimbResult.IN_PROGRESS;
        }

        if (state.phase == Phase.TRAVERSING) {
            state.ladderProgress = Mth.clamp(state.ladderProgress
                            + (state.upward ? CLIMB_LOCAL_Y_PER_TICK : -CLIMB_LOCAL_Y_PER_TICK),
                    0.0D, end);
            Vec3 point = ladderPoint(state.ladderProgress);
            place(climber, point);
            if (state.upward && state.ladderProgress >= end
                    || !state.upward && state.ladderProgress <= 0.0D) {
                state.phase = Phase.EXITING;
                state.transitionStart = point;
                state.transitionProgress = 0.0D;
                state.exit = findExit(climber, state);
            }
            promote();
            return ClimbResult.IN_PROGRESS;
        }

        state.transitionProgress = advanceTransition(
                state.transitionStart, state.exit, state.transitionProgress);
        place(climber, state.transitionStart.lerp(state.exit, state.transitionProgress));
        if (state.transitionProgress < 1.0D) {
            return ClimbResult.IN_PROGRESS;
        }

        finish(climber.getUUID());
        return ClimbResult.COMPLETE;
    }

    private Vec3 ladderPoint(double localY) {
        Vec3 point = host.ladderWorldPoint(localY);
        double angle = host.leanAngleRadians();
        Vec3 backOffset = Math.abs(Math.cos(angle)) >= CLIMB_POSE_MIN_VERTICAL_FACTOR
                ? host.uphillVector().scale(GUIDED_BACK_OFFSET)
                : Vec3.ZERO;
        return new Vec3(point.x - backOffset.x, host.platformY(localY), point.z - backOffset.z);
    }

    private Vec3 findExit(LivingEntity climber, Climber state) {
        if (state.upward) {
            double end = host.ladderLength() + WALK_EXTENSION;
            Vec3 top = host.ladderWorldPoint(end);
            Vec3 landing = findTopLanding(top, state.sequence, climber, true);
            return landing == null ? topExit() : landing;
        }
        return findBottomLanding(state.sequence, climber);
    }

    private Vec3 findTopLanding(Vec3 top, int preferredSlot, LivingEntity climber,
                                boolean requireAvailable) {
        Vec3 uphill = host.uphillVector();
        Vec3 right = new Vec3(-uphill.z, 0.0D, uphill.x);
        for (int offset = 0; offset < SIDE_OFFSETS.length; offset++) {
            double side = SIDE_OFFSETS[Math.floorMod(preferredSlot + offset, SIDE_OFFSETS.length)];
            Vec3 best = null;
            boolean foundSurface = false;
            int missesAfterSurface = 0;
            for (double depth = LANDING_MIN_DEPTH;
                 depth <= LANDING_MAX_DEPTH; depth += LANDING_SAMPLE_STEP) {
                Vec3 safe = findSafeStandingPosition(top.add(uphill.scale(depth)).add(right.scale(side)));
                if (safe == null) {
                    if (foundSurface && ++missesAfterSurface >= 2) {
                        break;
                    }
                    continue;
                }

                foundSurface = true;
                missesAfterSurface = 0;
                if (!requireAvailable || landingAvailable(safe, climber)) {
                    best = safe;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private Vec3 findBottomLanding(int preferredSlot, LivingEntity climber) {
        Vec3 base = bottomApproach();
        Vec3 uphill = host.uphillVector();
        Vec3 right = new Vec3(-uphill.z, 0.0D, uphill.x);
        for (int row = 0; row < 3; row++) {
            for (int offset = 0; offset < SIDE_OFFSETS.length; offset++) {
                double side = SIDE_OFFSETS[Math.floorMod(preferredSlot + offset, SIDE_OFFSETS.length)];
                Vec3 candidate = base.subtract(uphill.scale(row * WAITING_SPACING)).add(right.scale(side));
                Vec3 safe = findSafeStandingPosition(candidate, base.y - 1.5D);
                if (safe != null && landingAvailable(safe, climber)) {
                    return safe;
                }
            }
        }
        return base;
    }

    private Vec3 findSafeStandingPosition(Vec3 candidate) {
        return findSafeStandingPosition(candidate, bottomApproach().y + 1.0D);
    }

    private Vec3 findSafeStandingPosition(Vec3 candidate, double minimumExitY) {
        int centerY = Mth.floor(candidate.y);
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos feet = BlockPos.containing(candidate.x, centerY + dy, candidate.z);
            if (feet.getY() < minimumExitY) {
                continue;
            }
            BlockPos support = feet.below();
            BlockState feetState = host.level().getBlockState(feet);
            BlockState headState = host.level().getBlockState(feet.above());
            BlockState supportState = host.level().getBlockState(support);
            VoxelShape supportShape = supportState.getCollisionShape(host.level(), support);
            if (feetState.getCollisionShape(host.level(), feet).isEmpty()
                    && headState.getCollisionShape(host.level(), feet.above()).isEmpty()
                    && !supportShape.isEmpty()
                    && supportShape.max(Direction.Axis.Y) >= 0.75D) {
                return new Vec3(candidate.x, feet.getY(), candidate.z);
            }
        }
        return null;
    }

    private boolean landingAvailable(Vec3 position, LivingEntity climber) {
        AABB occupancy = new AABB(position.x - 0.45D, position.y, position.z - 0.45D,
                position.x + 0.45D, position.y + 1.9D, position.z + 0.45D);
        return host.level().getEntitiesOfClass(LivingEntity.class, occupancy,
                entity -> entity != host.ladder() && entity != climber && entity.isAlive()).isEmpty();
    }

    private void moveWaitingClimber(LivingEntity climber, Climber state) {
        Vec3 waitingPosition = waitingPosition(state.upward, state.stagingIndex);
        if (!(climber instanceof Mob mob)) {
            return;
        }
        if (climber.distanceToSqr(waitingPosition) <= STAGING_ARRIVAL_DISTANCE_SQR) {
            state.orderedPosition = null;
            mob.getNavigation().stop();
            Vec3 velocity = climber.getDeltaMovement();
            climber.setDeltaMovement(0.0D, velocity.y, 0.0D);
            return;
        }
        if (state.orderedPosition != null && !mob.getNavigation().isDone()
                && state.orderedPosition.distanceToSqr(waitingPosition) <= STAGING_ARRIVAL_DISTANCE_SQR) {
            return;
        }
        state.orderedPosition = waitingPosition;
        mob.getNavigation().moveTo(waitingPosition.x, waitingPosition.y, waitingPosition.z, 1.0D);
    }

    private Vec3 waitingPosition(boolean upward, int queueIndex) {
        Vec3 uphill = host.uphillVector();
        Vec3 right = new Vec3(-uphill.z, 0.0D, uphill.x);
        int lane = queueIndex % SIDE_OFFSETS.length;
        int row = queueIndex / SIDE_OFFSETS.length;
        Vec3 base = upward ? bottomApproach() : topExit();
        Vec3 candidate = base.add(right.scale(SIDE_OFFSETS[lane]));
        candidate = upward
                ? candidate.subtract(uphill.scale(row * WAITING_SPACING))
                : candidate.add(uphill.scale(row * WAITING_SPACING));
        Vec3 safe = findSafeStandingPosition(candidate,
                upward ? base.y - 1.5D : bottomApproach().y + 1.0D);
        return safe == null ? base : safe;
    }

    private void place(LivingEntity climber, Vec3 position) {
        if (climber instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        climber.setPos(position.x, position.y, position.z);
        climber.setDeltaMovement(Vec3.ZERO);
        climber.setOnGround(true);
        climber.fallDistance = 0.0F;
    }

    private void promote() {
        boolean promotedAny = true;
        while (promotedAny) {
            promotedAny = false;
            Iterator<UUID> iterator = queue.iterator();
            while (iterator.hasNext()) {
                UUID uuid = iterator.next();
                Climber state = climbers.get(uuid);
                Entity entity = host.level() instanceof ServerLevel serverLevel
                        ? serverLevel.getEntity(uuid) : null;
                if (state == null || !(entity instanceof LivingEntity living) || !living.isAlive()) {
                    iterator.remove();
                    climbers.remove(uuid);
                    continue;
                }
                if (!state.ready) {
                    continue;
                }
                if (!canEnter(state)) {
                    return;
                }
                iterator.remove();
                activeClimbers.add(uuid);
                state.phase = Phase.ENTERING;
                state.transitionStart = living.position();
                state.transitionProgress = 0.0D;
                promotedAny = true;
                break;
            }
            if (promotedAny) {
                reindexQueue();
            }
        }
    }

    private void reindexQueue() {
        int index = 0;
        for (UUID uuid : queue) {
            Climber state = climbers.get(uuid);
            if (state != null) {
                int nextIndex = index++;
                if (state.phase == Phase.WAITING && state.stagingIndex != nextIndex) {
                    state.ready = false;
                }
                state.stagingIndex = nextIndex;
            }
        }
    }

    private boolean hasArrived(LivingEntity climber, Climber state) {
        return climber.distanceToSqr(waitingPosition(state.upward, state.stagingIndex))
                <= STAGING_ARRIVAL_DISTANCE_SQR;
    }

    private boolean canEnter(Climber candidate) {
        double end = host.ladderLength() + WALK_EXTENSION;
        for (UUID uuid : activeClimbers) {
            Climber active = climbers.get(uuid);
            if (active == null) {
                continue;
            }
            if (active.upward != candidate.upward || active.phase == Phase.ENTERING) {
                return false;
            }
            if (active.phase == Phase.TRAVERSING) {
                double distanceFromEntrance = active.upward
                        ? active.ladderProgress : end - active.ladderProgress;
                if (distanceFromEntrance < CLIMBER_GAP) {
                    return false;
                }
            }
        }
        return true;
    }

    private void finish(UUID uuid) {
        climbers.remove(uuid);
        activeClimbers.remove(uuid);
        promote();
    }

    private static double advanceTransition(Vec3 start, Vec3 end, double progress) {
        double distance = Math.max(0.01D, start.distanceTo(end));
        return Math.min(1.0D, progress + TRANSITION_DISTANCE_PER_TICK / distance);
    }

    public interface Host {
        Entity ladder();

        Level level();

        int gameTick();

        boolean ready();

        double ladderLength();

        double leanAngleRadians();

        Vec3 ladderWorldPoint(double localY);

        double platformY(double localY);

        Vec3 uphillVector();
    }

    private enum Phase {
        WAITING,
        ENTERING,
        TRAVERSING,
        EXITING
    }

    private static final class Climber {
        private final boolean upward;
        private final int sequence;
        private int stagingIndex;
        private Phase phase = Phase.WAITING;
        private int lastRequestTick;
        private double ladderProgress;
        private double transitionProgress;
        private Vec3 transitionStart;
        private Vec3 orderedPosition;
        private Vec3 exit;
        private boolean ready;

        private Climber(boolean upward, int sequence, int stagingIndex, int lastRequestTick) {
            this.upward = upward;
            this.sequence = sequence;
            this.stagingIndex = stagingIndex;
            this.lastRequestTick = lastRequestTick;
        }
    }
}
