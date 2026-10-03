package me.mss1r.siegeworks.gameplay.movement.tower;

import me.mss1r.siegeworks.api.SiegeTransportControl.ExitResult;
import me.mss1r.siegeworks.gameplay.movement.SyncedFloatInterpolator;
import me.mss1r.siegeworks.gameplay.crew.tower.TowerCrewRoster.Seat;
import me.mss1r.siegeworks.gameplay.crew.tower.TowerPassengerLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class TowerBridgeController {
    public static final int DEFAULT_PRESET = 2;
    public static final double AUTOMATED_DEPLOYMENT_DISTANCE = 8.0D;

    private static final String TAG_OPEN = "BridgeOpen";
    private static final String TAG_PRESET = "BridgePreset";
    private static final String TAG_PROGRESS = "BridgeProgress";
    private static final double PLATFORM_WIDTH = 120.0D / 16.0D;
    private static final double PLATFORM_START = TowerBridgeTraversal.start();
    private static final float MAX_ANGLE_DEGREES = 120.0F;
    private static final float PROGRESS_STEP = 1.0F / 160.0F;
    private static final float REST_PROBE = PROGRESS_STEP * 2.0F;
    private static final double SAMPLE_STEP = 0.25D;
    private static final double CONTACT_ABOVE = 0.34D;
    private static final double CONTACT_BELOW = 0.75D;
    private static final int REST_PROBE_INTERVAL_TICKS = 20;
    private static final double WALL_HEIGHT_ABOVE_BASE = 2.5D;
    private static final float SETTLED_TOLERANCE = 0.015F;
    private static final float SYNC_EPSILON = 1.0E-5F;
    private static final int SYNC_SILENCE_TICKS = 4;
    private static final int EXIT_INTERVAL_TICKS = 4;
    private static final double WALK_SPEED = 0.14D;
    private static final double STEP_HEIGHT = 1.5D;
    private static final float[] PRESET_PROGRESS = {0.5F, 0.625F, 0.75F, 0.875F, 1.0F};
    private static final String[] PRESET_KEYS = {
            "siege.tower.bridge.angle.high",
            "siege.tower.bridge.angle.raised",
            "siege.tower.bridge.angle.level",
            "siege.tower.bridge.angle.slight_down",
            "siege.tower.bridge.angle.low_drop"
    };

    private final Host host;
    private final TowerBridgeTraversal traversal;
    private final SyncedFloatInterpolator clientProgress = new SyncedFloatInterpolator();
    private final Map<UUID, AutomatedExit> automatedExits = new HashMap<>();
    private final Map<LivingEntity, AutomatedReturn> automatedReturns = new WeakHashMap<>();
    private float previousAngleRadians;
    private boolean restingOnSurface;
    private boolean predicting;
    private float predictedProgress;
    private float lastSyncedProgress;
    private int syncedStillTicks;
    private int nextSurfaceProbeTick;
    private int lastExitTick = Integer.MIN_VALUE / 2;

    public TowerBridgeController(Host host) {
        this.host = host;
        this.traversal = new TowerBridgeTraversal(host);
    }

    public void load(CompoundTag tag) {
        automatedReturns.clear();
        automatedExits.clear();
        setOpen(tag.getBoolean(TAG_OPEN));
        setPreset(tag.contains(TAG_PRESET) ? tag.getInt(TAG_PRESET) : DEFAULT_PRESET);
        setProgress(tag.contains(TAG_PROGRESS) ? tag.getFloat(TAG_PROGRESS) : targetProgress());
        updateTargetProgress();
    }

    public void save(CompoundTag tag) {
        tag.putBoolean(TAG_OPEN, open());
        tag.putInt(TAG_PRESET, preset());
        tag.putFloat(TAG_PROGRESS, progress());
    }

    public void tick() {
        previousAngleRadians = angleRadians();
        if (host.clientSide()) {
            tickClientPrediction();
            clientProgress.tick(progress());
        } else {
            updateTargetProgress();
            tickProgress();
        }
    }

    public boolean open() {
        return host.open();
    }

    public void setOpen(boolean open) {
        if (host.open() != open) {
            resetSurfaceRest();
        }
        host.setOpen(open);
    }

    public int preset() {
        return Mth.clamp(host.preset(), 0, PRESET_PROGRESS.length - 1);
    }

    public void setPreset(int preset) {
        int clamped = Mth.clamp(preset, 0, PRESET_PROGRESS.length - 1);
        if (host.preset() != clamped) {
            resetSurfaceRest();
        }
        host.setPreset(clamped);
    }

    public String cyclePreset() {
        setPreset((preset() + 1) % PRESET_PROGRESS.length);
        return PRESET_KEYS[preset()];
    }

    public float progress() {
        return Mth.clamp(predicting ? predictedProgress : host.progress(), 0.0F, 1.0F);
    }

    private void tickClientPrediction() {
        float synced = Mth.clamp(host.progress(), 0.0F, 1.0F);
        if (!predicting || Math.abs(synced - lastSyncedProgress) > SYNC_EPSILON) {
            lastSyncedProgress = synced;
            predictedProgress = synced;
            syncedStillTicks = 0;
            predicting = true;
            return;
        }

        if (++syncedStillTicks > SYNC_SILENCE_TICKS) {
            predictedProgress = synced;
            return;
        }
        predictedProgress = Mth.approach(predictedProgress, targetProgress(), PROGRESS_STEP);
    }

    public float targetProgress() {
        return Mth.clamp(host.targetProgress(), 0.0F, 1.0F);
    }

    public float renderedProgress(float partialTick) {
        return !host.clientSide() || !clientProgress.isInitialized()
                ? progress()
                : clientProgress.sample(partialTick);
    }

    public float angleRadians() {
        return (float) angleRadians(progress());
    }

    public float previousAngleRadians() {
        return previousAngleRadians;
    }

    public float renderedAngleRadians(float partialTick) {
        return (float) angleRadians(renderedProgress(partialTick));
    }

    public double platformAngleRadians() {
        return platformAngleRadians(progress());
    }

    public boolean facesSomethingToLandOn() {
        float target = PRESET_PROGRESS[preset()];
        double end = platformEnd(target);
        double halfWidth = PLATFORM_WIDTH * 0.5D;
        double[] sides = {0.0D, -halfWidth * 0.88D, -halfWidth * 0.44D,
                halfWidth * 0.44D, halfWidth * 0.88D};
        for (double forward = PLATFORM_START + 1.0D; forward <= end; forward += SAMPLE_STEP) {
            for (double side : sides) {
                Vec3 point = worldPoint(side, forward, platformYAt(target, forward));
                Vec3 floor = host.findSafeAtHeight(point.x, point.y, point.z);
                if (floor != null && floor.y - host.y() >= WALL_HEIGHT_ABOVE_BASE
                        && Math.abs(floor.y - point.y) <= STEP_HEIGHT) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasBridgePassengers() {
        return open() && traversal.hasPassengers(platformAngleRadians());
    }

    public boolean leadsSomewhere() {
        return open() && settled() && findExit(new Seat(TowerPassengerLayout.FLOOR_TWO, 0)) != null;
    }

    public boolean moving() {
        return open() && !settled();
    }

    public boolean canPassengerExit(LivingEntity passenger, Seat seat) {
        return passenger.getVehicle() == host.vehicle()
                && seat != null && seat.floor() == 2 && open()
                && host.tickCount() - lastExitTick >= EXIT_INTERVAL_TICKS
                && settled() && findExit(seat, passenger) != null;
    }

    public boolean disembarkPassenger(LivingEntity passenger, Seat seat) {
        if (!canPassengerExit(passenger, seat)) {
            return false;
        }
        AutomatedExitPlan plan = findExit(seat, passenger);
        if (plan == null) {
            return false;
        }

        double start = Math.min(plan.bridgeEnd(), PLATFORM_START + 0.65D);
        Vec3 bridgeStart = traversal.surfacePoint(passenger, platformAngleRadians(), plan.side(), start);
        passenger.stopRiding();
        automatedExits.put(passenger.getUUID(), new AutomatedExit(
                plan.side(), start, plan.bridgeEnd(), plan.landing()));
        placePassenger(passenger, bridgeStart);
        lastExitTick = host.tickCount();
        return true;
    }

    public ExitResult advanceAutomatedExit(LivingEntity passenger) {
        AutomatedExit exit = automatedExits.get(passenger.getUUID());
        if (exit == null || !passenger.isAlive() || passenger.level() != host.level() || !open()) {
            automatedExits.remove(passenger.getUUID());
            return ExitResult.FAILED;
        }
        if (exit.forward < exit.endForward) {
            exit.forward = Math.min(exit.endForward, exit.forward + WALK_SPEED);
            placePassenger(passenger, traversal.surfacePoint(passenger, platformAngleRadians(), exit.side, exit.forward));
            return ExitResult.IN_PROGRESS;
        }

        Vec3 bridgeEnd = traversal.surfacePoint(passenger, platformAngleRadians(), exit.side, exit.endForward);
        double transitionDistance = Math.max(0.01D, bridgeEnd.distanceTo(exit.landing));
        exit.landingProgress = Math.min(1.0D, exit.landingProgress + WALK_SPEED / transitionDistance);
        placePassenger(passenger, bridgeEnd.lerp(exit.landing, exit.landingProgress));
        if (exit.landingProgress < 1.0D) {
            return ExitResult.IN_PROGRESS;
        }
        automatedExits.remove(passenger.getUUID());
        return ExitResult.COMPLETE;
    }

    public Vec3 automatedReturnApproach(LivingEntity passenger, Seat seat) {
        if (seat == null || passenger.getVehicle() == host.vehicle() || !returnReady()) {
            return null;
        }
        if (traversal.onBridge(passenger, platformAngleRadians())) {
            return passenger.position();
        }
        TowerBridgeTraversal.Entry entry = traversal.findEntry(passenger, platformAngleRadians());
        return entry == null ? null : entry.landing();
    }

    public boolean beginAutomatedReturn(LivingEntity passenger, Seat seat) {
        if (seat == null || passenger.isPassenger() || !returnReady()) {
            return false;
        }
        double angle = platformAngleRadians();
        TowerBridgeTraversal.Entry entry = null;
        if (!traversal.onBridge(passenger, angle)) {
            entry = traversal.findEntry(passenger, angle);
            if (entry == null || !traversal.canEnter(passenger, entry)) {
                return false;
            }
        }
        automatedReturns.put(passenger, new AutomatedReturn(
                entry == null ? traversal.clampedSide(passenger) : entry.side(),
                entry == null ? traversal.local(passenger).z : entry.forward(), entry, angle,
                host.vehicle().position(), host.visualYaw()));
        return true;
    }

    public ExitResult advanceAutomatedReturn(LivingEntity passenger) {
        AutomatedReturn route = automatedReturns.get(passenger);
        if (route == null || !passenger.isAlive() || passenger.isPassenger()
                || passenger.level() != host.level() || !returnReady()
                || Math.abs(platformAngleRadians() - route.angle) > 0.01D
                || host.vehicle().position().distanceToSqr(route.origin) > 0.05D * 0.05D
                || Math.abs(Mth.wrapDegrees(host.visualYaw() - route.yaw)) > 0.5F) {
            automatedReturns.remove(passenger);
            return ExitResult.FAILED;
        }
        if (route.entry != null && route.entryStage < 2) {
            Vec3 target = route.entryStage == 0 ? route.entry.lift() : route.entry.deck();
            Vec3 offset = target.subtract(passenger.position());
            Vec3 next = offset.length() <= WALK_SPEED ? target
                    : passenger.position().add(offset.normalize().scale(WALK_SPEED));
            if (!traversal.clear(passenger, next)) {
                automatedReturns.remove(passenger);
                return ExitResult.FAILED;
            }
            placePassenger(passenger, next);
            if (next.distanceToSqr(target) < 1.0E-6D) {
                route.entryStage++;
            }
            return ExitResult.IN_PROGRESS;
        }
        double nextForward = Math.max(PLATFORM_START, route.forward - WALK_SPEED);
        Vec3 target = traversal.surfacePoint(passenger, route.angle, route.side, nextForward);
        Vec3 offset = target.subtract(passenger.position());
        Vec3 next = offset.length() <= WALK_SPEED ? target
                : passenger.position().add(offset.normalize().scale(WALK_SPEED));
        if (!traversal.clear(passenger, next)) {
            if (target.y > passenger.getY()) {
                next = passenger.position().add(0.0D, Math.min(WALK_SPEED, target.y - passenger.getY()), 0.0D);
            } else {
                Vec3 horizontal = offset.multiply(1.0D, 0.0D, 1.0D);
                next = passenger.position().add(horizontal.length() <= WALK_SPEED
                        ? horizontal : horizontal.normalize().scale(WALK_SPEED));
            }
        }
        if (!traversal.clear(passenger, next)
                || (next.distanceToSqr(passenger.position()) < 1.0E-10D
                && target.distanceToSqr(passenger.position()) > 1.0E-6D)) {
            automatedReturns.remove(passenger);
            return ExitResult.FAILED;
        }
        placePassenger(passenger, next);
        if (next.distanceToSqr(target) < 1.0E-6D) {
            route.forward = nextForward;
        }
        if (route.forward > PLATFORM_START) {
            return ExitResult.IN_PROGRESS;
        }
        automatedReturns.remove(passenger);
        return ExitResult.COMPLETE;
    }

    public void cancelAutomatedReturn(LivingEntity passenger) {
        automatedReturns.remove(passenger);
    }

    private boolean returnReady() {
        return open() && Math.abs(progress() - targetProgress()) < 0.0005F;
    }

    public void cancelAutomatedExit(LivingEntity passenger) {
        automatedExits.remove(passenger.getUUID());
    }

    private boolean settled() {
        return Math.abs(progress() - targetProgress()) <= SETTLED_TOLERANCE;
    }

    private void tickProgress() {
        float current = progress();
        float target = targetProgress();
        if (Math.abs(current - target) < 0.0005F) {
            if (current != target) {
                setProgress(target);
            }
            return;
        }
        float next = Mth.approach(current, target, PROGRESS_STEP);
        if (next > current && wouldRestOnSurface(next)) {
            setTargetProgress(current);
            restingOnSurface = true;
            nextSurfaceProbeTick = host.tickCount() + REST_PROBE_INTERVAL_TICKS;
            return;
        }
        setProgress(next);
    }

    private void updateTargetProgress() {
        if (host.clientSide()) {
            return;
        }
        if (!open()) {
            restingOnSurface = false;
            if (targetProgress() != 0.0F) {
                setTargetProgress(0.0F);
            }
            return;
        }
        float desired = PRESET_PROGRESS[preset()];
        float current = progress();
        if (!restingOnSurface || desired < current) {
            setTargetProgress(desired);
            return;
        }
        if (desired <= current || host.tickCount() < nextSurfaceProbeTick) {
            return;
        }
        float probe = Math.min(desired, current + REST_PROBE);
        nextSurfaceProbeTick = host.tickCount() + REST_PROBE_INTERVAL_TICKS;
        if (!wouldRestOnSurface(probe)) {
            restingOnSurface = false;
            setTargetProgress(desired);
        }
    }

    private boolean wouldRestOnSurface(float progress) {
        double sensorEnd = platformEnd(progress);
        double halfWidth = PLATFORM_WIDTH * 0.5D;
        double[] sides = {0.0D, -halfWidth * 0.88D, -halfWidth * 0.44D,
                halfWidth * 0.44D, halfWidth * 0.88D};
        for (double forward = PLATFORM_START + 1.0D; forward <= sensorEnd; forward += SAMPLE_STEP) {
            double y = platformYAt(progress, forward);
            for (double side : sides) {
                Vec3 world = worldPoint(side, forward, y);
                if (surfaceContact(world.x, world.y, world.z)) {
                    return true;
                }
            }
        }
        double y = platformYAt(progress, sensorEnd);
        for (double side : sides) {
            Vec3 world = worldPoint(side, sensorEnd, y);
            if (surfaceContact(world.x, world.y, world.z)) {
                return true;
            }
        }
        return false;
    }

    private boolean surfaceContact(double x, double platformY, double z) {
        BlockPos base = BlockPos.containing(x, platformY, z);
        Level level = host.level();
        for (int dy = -2; dy <= 1; dy++) {
            BlockPos pos = base.offset(0, dy, 0);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            double top = pos.getY() + shape.max(Direction.Axis.Y);
            if (platformY <= top + CONTACT_ABOVE && platformY >= top - CONTACT_BELOW) {
                return true;
            }
        }
        return false;
    }

    private AutomatedExitPlan findExit(Seat seat) {
        return findExit(seat, null);
    }

    private AutomatedExitPlan findExit(Seat seat, LivingEntity passenger) {
        double end = platformEnd(progress());
        double preferredSide = Mth.clamp((seat.slot() % 5 - 2) * 1.15D,
                -PLATFORM_WIDTH * 0.35D, PLATFORM_WIDTH * 0.35D);
        double[] sides = {preferredSide, 0.0D, preferredSide * 0.5D, -preferredSide * 0.5D};
        for (double distance = PLATFORM_START + 0.75D; distance <= end + 0.75D; distance += SAMPLE_STEP) {
            double bridgeDistance = Math.min(distance, end);
            double y = platformYAt(bridgeDistance);
            for (double side : sides) {
                Vec3 candidate = worldPoint(side, distance, y);
                Vec3 safe = host.findSafeAtHeight(candidate.x, candidate.y, candidate.z);
                if (safe != null && Math.abs(safe.y - y) <= STEP_HEIGHT) {
                    Vec3 landing = findLanding(passenger, side, distance, safe);
                    if (landing == null) {
                        continue;
                    }
                    double bridgeEnd = Mth.clamp(distance - 0.2D, PLATFORM_START + 0.65D, end);
                    return new AutomatedExitPlan(side, bridgeEnd, landing);
                }
            }
        }
        return null;
    }

    private Vec3 findLanding(LivingEntity passenger, double side, double contactDistance, Vec3 contact) {
        Vec3 best = null;
        int misses = 0;
        for (double distance = contactDistance + SAMPLE_STEP;
             distance <= Math.min(contactDistance + 2.0D, platformEnd(progress()) - 0.5D);
             distance += SAMPLE_STEP) {
            Vec3 sample = worldPoint(side, distance, contact.y);
            Vec3 safe = host.findSafeAtHeight(sample.x, contact.y, sample.z);
            if (safe == null || Math.abs(safe.y - contact.y) > STEP_HEIGHT) {
                if (++misses >= 2) {
                    break;
                }
                continue;
            }
            misses = 0;
            Vec3 surface = traversal.surfacePoint(passenger, platformAngleRadians(), side, distance);
            Vec3 landing = new Vec3(safe.x, Math.max(safe.y, surface.y), safe.z);
            if (traversal.clear(passenger, landing)) {
                best = landing;
            }
        }
        return best;
    }

    private double platformYAt(double forward) {
        return platformYAt(progress(), forward);
    }

    private double platformYAt(float progress, double forward) {
        double sample = Mth.clamp(forward, PLATFORM_START, Math.max(PLATFORM_START, platformEnd(progress)));
        double angle = platformAngleRadians(progress);
        return host.y() + TowerBridgeTraversal.surfaceY(angle, sample);
    }

    private double platformEnd(float progress) {
        return TowerBridgeTraversal.end(platformAngleRadians(progress));
    }

    private double platformAngleRadians(float progress) {
        return angleRadians(Math.max(0.01F, progress));
    }

    private static double angleRadians(float progress) {
        return Math.toRadians(MAX_ANGLE_DEGREES * Mth.clamp(progress, 0.0F, 1.0F));
    }

    private Vec3 worldPoint(double side, double forward, double y) {
        float yaw = (float) Math.toRadians(host.visualYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        return new Vec3(host.x() + rightX * side + forwardX * forward, y,
                host.z() + rightZ * side + forwardZ * forward);
    }

    private void setProgress(float progress) {
        host.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
    }

    private void setTargetProgress(float progress) {
        host.setTargetProgress(Mth.clamp(progress, 0.0F, 1.0F));
    }

    private void resetSurfaceRest() {
        restingOnSurface = false;
        nextSurfaceProbeTick = 0;
    }

    private static void placePassenger(LivingEntity passenger, Vec3 position) {
        passenger.setPos(position.x, position.y, position.z);
        passenger.setDeltaMovement(Vec3.ZERO);
        passenger.setOnGround(true);
        passenger.fallDistance = 0.0F;
        passenger.hurtMarked = true;
    }

    public interface Host {
        LivingEntity vehicle();
        Level level();
        boolean clientSide();
        int tickCount();
        double x();
        double y();
        double z();
        float visualYaw();
        boolean open();
        void setOpen(boolean open);
        int preset();
        void setPreset(int preset);
        float progress();
        void setProgress(float progress);
        float targetProgress();
        void setTargetProgress(float progress);
        Vec3 findSafeAtHeight(double x, double y, double z);
    }

    private record AutomatedExitPlan(double side, double bridgeEnd, Vec3 landing) {
    }

    private static final class AutomatedExit {
        private final double side;
        private final double endForward;
        private final Vec3 landing;
        private double forward;
        private double landingProgress;

        private AutomatedExit(double side, double startForward, double endForward, Vec3 landing) {
            this.side = side;
            this.forward = startForward;
            this.endForward = endForward;
            this.landing = landing;
        }
    }

    private static final class AutomatedReturn {
        private final double side;
        private final TowerBridgeTraversal.Entry entry;
        private final double angle;
        private final Vec3 origin;
        private final float yaw;
        private double forward;
        private int entryStage;

        private AutomatedReturn(double side, double forward, TowerBridgeTraversal.Entry entry, double angle,
                                Vec3 origin, float yaw) {
            this.side = side;
            this.forward = forward;
            this.entry = entry;
            this.angle = angle;
            this.origin = origin;
            this.yaw = yaw;
        }
    }
}
