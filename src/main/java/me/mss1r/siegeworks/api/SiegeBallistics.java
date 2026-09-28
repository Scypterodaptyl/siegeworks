package me.mss1r.siegeworks.api;

import net.minecraft.world.phys.Vec3;

/** A few aiming helpers shared by siege engines and integrations. */
public final class SiegeBallistics {
    private static final double VANILLA_AIR_RETENTION = 0.99D;
    private static final int MAX_FLIGHT_TICKS = 600;

    private SiegeBallistics() {
    }

    /** Returns Minecraft pitch for the lower firing solution, or {@link Float#NaN} if it cannot reach. */
    public static float calculateLowAnglePitch(Vec3 origin, Vec3 target, double speed, double gravity) {
        double dx = target.x - origin.x;
        double dy = target.y - origin.y;
        double dz = target.z - origin.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 1.0E-4D) {
            return (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        }

        double speedSquared = speed * speed;
        double discriminant = speedSquared * speedSquared
                - gravity * (gravity * horizontalDistance * horizontalDistance + 2.0D * dy * speedSquared);
        if (speed <= 0.0D || discriminant < 0.0D) {
            return Float.NaN;
        }
        double angle = gravity <= 0.0D
                ? Math.atan2(dy, horizontalDistance)
                : Math.atan((speedSquared - Math.sqrt(discriminant)) / (gravity * horizontalDistance));

        return (float) -Math.toDegrees(angle);
    }

    /** Returns the power multiplier for a fixed launch slope, without drag, or NaN if unreachable. */
    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, double gravity) {
        double dx = target.x - origin.x;
        double dz = target.z - origin.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double height = target.y - origin.y;
        double angle = Math.atan(launchSlope);
        double denominator = 2.0D * Math.cos(angle) * Math.cos(angle)
                * (horizontalDistance * Math.tan(angle) - height);
        if (baseSpeed <= 0.0D || gravity <= 0.0D || denominator <= 0.0D) {
            return Double.NaN;
        }

        double requiredSpeedSquared = gravity * horizontalDistance * horizontalDistance / denominator;
        return requiredSpeedSquared <= 0.0D ? Double.NaN : Math.sqrt(requiredSpeedSquared) / baseSpeed;
    }

    /** Returns a bounded power multiplier using per-tick drag, or NaN if the range has no solution. */
    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, double gravity, double projectileDrag,
                                                double minPower, double maxPower) {
        if (baseSpeed <= 0.0D || gravity <= 0.0D || minPower <= 0.0D || maxPower < minPower) {
            return Double.NaN;
        }

        double dx = target.x - origin.x;
        double dz = target.z - origin.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double targetHeight = target.y - origin.y;
        if (horizontalDistance < 1.0E-4D) {
            return Double.NaN;
        }

        double lowError = fixedArcHeightError(horizontalDistance, targetHeight, baseSpeed * minPower,
                launchSlope, gravity, projectileDrag);
        double highError = fixedArcHeightError(horizontalDistance, targetHeight, baseSpeed * maxPower,
                launchSlope, gravity, projectileDrag);
        if (!Double.isFinite(highError) || lowError > 0.0D || highError < 0.0D) {
            return Double.NaN;
        }

        double low = minPower;
        double high = maxPower;
        for (int i = 0; i < 32; i++) {
            double middle = (low + high) * 0.5D;
            double error = fixedArcHeightError(horizontalDistance, targetHeight, baseSpeed * middle,
                    launchSlope, gravity, projectileDrag);
            if (!Double.isFinite(error) || error < 0.0D) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) * 0.5D;
    }

    private static double fixedArcHeightError(double horizontalDistance, double targetHeight, double speed,
                                               double launchSlope, double gravity, double projectileDrag) {
        double directionLength = Math.sqrt(1.0D + launchSlope * launchSlope);
        double horizontalVelocity = speed / directionLength;
        double verticalVelocity = horizontalVelocity * launchSlope;
        double horizontalPosition = 0.0D;
        double verticalPosition = 0.0D;
        double customRetention = Math.max(0.0D, 1.0D - projectileDrag);
        double retention = VANILLA_AIR_RETENTION * customRetention;
        double gravityAfterDrag = gravity * customRetention;

        for (int tick = 0; tick < MAX_FLIGHT_TICKS; tick++) {
            double previousHorizontal = horizontalPosition;
            double previousVertical = verticalPosition;
            horizontalPosition += horizontalVelocity;
            verticalPosition += verticalVelocity;
            if (horizontalPosition >= horizontalDistance) {
                double segment = horizontalPosition - previousHorizontal;
                double progress = segment <= 1.0E-8D
                        ? 1.0D
                        : (horizontalDistance - previousHorizontal) / segment;
                double heightAtTarget = previousVertical
                        + (verticalPosition - previousVertical) * progress;
                return heightAtTarget - targetHeight;
            }

            horizontalVelocity *= retention;
            verticalVelocity = verticalVelocity * retention - gravityAfterDrag;
            if (horizontalVelocity <= 1.0E-6D) {
                break;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }
}
