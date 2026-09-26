package me.mss1r.siegeworks.gameplay.movement;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import me.mss1r.axiomata.collision.StructureTransform;
import me.mss1r.siegeworks.gameplay.collision.SiegeTerrainCollision;
import net.minecraft.world.phys.Vec3;

public final class SiegeMovementPhysics {
    private SiegeMovementPhysics() {
    }

    private static void turnAboutPivot(AbstractSiegeEntity siege, float yawDelta, boolean clientPredicted) {
        Vec3 before = siege.towPivotWorldOffset();
        float previousYaw = siege.getYRot();
        StructureTransform previousPose = siege.collisionTransform();
        float yaw = previousYaw + yawDelta;
        if (clientPredicted) {
            siege.applyClientPredictedYaw(yaw);
        } else {
            siege.applyYaw(yaw);
        }

        Vec3 shift = before.subtract(siege.towPivotWorldOffset());
        StructureTransform turnedPose = shifted(siege.collisionTransform(), shift);
        if (!SiegeTerrainCollision.canOccupy(siege, previousPose, turnedPose)) {
            if (clientPredicted) {
                siege.applyClientPredictedYaw(previousYaw);
            } else {
                siege.applyYaw(previousYaw);
            }
            return;
        }
        if (shift.lengthSqr() > 1.0E-12D) {
            siege.setPos(siege.getX() + shift.x, siege.getY(), siege.getZ() + shift.z);
        }
    }

    private static StructureTransform shifted(StructureTransform pose, Vec3 shift) {
        return new StructureTransform(
                pose.x() + shift.x, pose.y(), pose.z() + shift.z, pose.yawDegrees());
    }

    public static void updateSiegeVelocity(AbstractSiegeEntity abstractSiegeEntity) {
        Entity passenger = abstractSiegeEntity.getMovementControllerPassenger();
        Entity operator = null;
        Player controller = null;

        if (passenger != null) {
            if (passenger instanceof Player player && abstractSiegeEntity.shouldPassengerControlMovement(passenger)) {
                operator = player;
                controller = player;
            } else if (passenger.getFirstPassenger() != null && passenger.getFirstPassenger() instanceof Player passengerRider
                    && abstractSiegeEntity.shouldPassengerControlMovement(passenger)) {
                operator = passenger;
                controller = passengerRider;
            } else if (passenger instanceof LivingEntity
                    && abstractSiegeEntity.shouldPassengerControlMovement(passenger)) {
                operator = passenger;
            }
        }

        float forwardInput = 0.0F;
        float steeringInput = 0.0F;
        boolean debugAutoDriving = operator == null && abstractSiegeEntity.isDebugAutoDriving();
        if (operator != null) {
            if (controller != null) {
                forwardInput = resolveForwardInput(abstractSiegeEntity, controller);
                steeringInput = resolveSteeringInput(abstractSiegeEntity, controller);
            } else {
                forwardInput = abstractSiegeEntity.getMovementInputForward();
                steeringInput = abstractSiegeEntity.getMovementInputSteering();
            }
        } else if (debugAutoDriving) {
            forwardInput = 1.0F;
        } else {
            abstractSiegeEntity.setMovementInput(0.0F, 0.0F);
        }

        double maximumSpeed = 0.0D;
        if (debugAutoDriving) {
            maximumSpeed = abstractSiegeEntity.getDebugAutoDriveVelocity();
        } else if (operator != null) {
            maximumSpeed = abstractSiegeEntity.getVelocity(operator);
        }
        double targetSpeed = maximumSpeed * forwardInput;
        if (targetSpeed < 0.0D) {
            targetSpeed *= abstractSiegeEntity.getReverseSpeedMultiplier();
        }

        double currentSpeed = abstractSiegeEntity.getCurrentDriveSpeed();
        double speedStep = abstractSiegeEntity.getDriveDeceleration(operator);
        if (Math.abs(forwardInput) > 0.01F) {
            if (debugAutoDriving) {
                speedStep = abstractSiegeEntity.getDebugAutoDriveAcceleration();
            } else if (operator != null) {
                speedStep = abstractSiegeEntity.getDriveAcceleration(operator);
            }
        }
        currentSpeed = approach(currentSpeed, targetSpeed, speedStep);
        if (Math.abs(currentSpeed) < 1.0E-5D) {
            currentSpeed = 0.0D;
        }
        abstractSiegeEntity.setCurrentDriveSpeed(currentSpeed);

        boolean moving = Math.abs(currentSpeed) > 1.0E-5D;
        boolean pivoting = !moving && abstractSiegeEntity.canPivotInPlace(operator);
        float steeringSpeed = 0.0F;
        if (operator != null && moving) {
            steeringSpeed = scaleSteeringForTravel(
                    abstractSiegeEntity.getSteeringSpeedDegrees(operator), currentSpeed,
                    maximumSpeed, abstractSiegeEntity.getReverseSpeedMultiplier());
        } else if (operator != null && pivoting) {
            steeringSpeed = abstractSiegeEntity.getPivotSteeringSpeedDegrees(operator);
        }
        abstractSiegeEntity.setCurrentSteeringSpeed(steeringSpeed);

        if (Math.abs(steeringInput) > 0.01F && (moving || pivoting) && operator != null) {
            float travelDirection = moving && currentSpeed < 0.0D ? -1.0F : 1.0F;
            float yawDelta = -steeringInput * steeringSpeed * travelDirection;
            turnAboutPivot(abstractSiegeEntity, yawDelta, false);
        }

        Vec3 horizontalVelocity = calculateMovementVector(
                currentSpeed, abstractSiegeEntity.getVisualRotationYInDegrees());
        Vec3 velocity = abstractSiegeEntity.getDeltaMovement();

        double verticalVelocity = velocity.y;
        if (verticalVelocity <= 0.0D && hasGroundSupport(abstractSiegeEntity)) {
            verticalVelocity = 0;
            abstractSiegeEntity.setOnGround(true);
        } else {
            verticalVelocity -= 0.08;
        }

        Vec3 newVelocity = new Vec3(horizontalVelocity.x, verticalVelocity, horizontalVelocity.z);
        Vec3 travel = SiegeTerrainCollision.clampMovement(abstractSiegeEntity, newVelocity);

        abstractSiegeEntity.setDeltaMovement(travel);
        abstractSiegeEntity.move(MoverType.SELF, travel);
    }

    public static float scaleSteeringForTravel(float maximumTurnDegrees, double currentSpeed,
                                               double maximumForwardSpeed, double reverseMultiplier) {
        double directionalMaximum = Math.abs(maximumForwardSpeed);
        if (currentSpeed < 0.0D) {
            directionalMaximum *= Mth.clamp(reverseMultiplier, 0.0D, 1.0D);
        }
        if (directionalMaximum <= 1.0E-8D) {
            return 0.0F;
        }
        double speedFraction = Mth.clamp(Math.abs(currentSpeed) / directionalMaximum, 0.0D, 1.0D);
        return (float) (Math.max(0.0F, maximumTurnDegrees) * speedFraction);
    }

    public static void updateClientSteering(AbstractSiegeEntity abstractSiegeEntity) {
        float steeringInput = abstractSiegeEntity.getMovementInputSteering();
        double currentSpeed = abstractSiegeEntity.getCurrentDriveSpeed();
        float steeringSpeed = abstractSiegeEntity.getCurrentSteeringSpeed();
        Entity operator = abstractSiegeEntity.getMovementControllerPassenger();

        boolean moving = Math.abs(currentSpeed) > 1.0E-5D;
        boolean pivoting = !moving && abstractSiegeEntity.canPivotInPlace(operator);
        if (Math.abs(steeringInput) > 0.01F && (moving || pivoting) && steeringSpeed > 0.0F) {
            float travelDirection = moving && currentSpeed < 0.0D ? -1.0F : 1.0F;
            float yawDelta = -steeringInput * steeringSpeed * travelDirection;
            turnAboutPivot(abstractSiegeEntity, yawDelta, true);
            return;
        }

        float yawDelta = Mth.wrapDegrees(abstractSiegeEntity.getTrackedYaw() - abstractSiegeEntity.getYRot());
        float correctionStep = Math.max(0.05F, steeringSpeed * 1.5F);
        abstractSiegeEntity.applyClientPredictedYaw(
                abstractSiegeEntity.getYRot() + Mth.clamp(yawDelta, -correctionStep, correctionStep));
    }

    private static float resolveForwardInput(AbstractSiegeEntity abstractSiegeEntity, Player player) {
        float vanillaInput = Mth.clamp(player.zza, -1.0F, 1.0F);
        if (Math.abs(vanillaInput) > 0.01F) {
            return vanillaInput;
        }
        return abstractSiegeEntity.getMovementInputForward();
    }

    private static float resolveSteeringInput(AbstractSiegeEntity abstractSiegeEntity, Player player) {
        float vanillaInput = Mth.clamp(player.xxa, -1.0F, 1.0F);
        if (Math.abs(vanillaInput) > 0.01F) {
            return vanillaInput;
        }
        return abstractSiegeEntity.getMovementInputSteering();
    }

    private static double approach(double current, double target, double step) {
        double safeStep = Math.max(0.0D, step);
        if (current < target) {
            return Math.min(current + safeStep, target);
        }
        return Math.max(current - safeStep, target);
    }

    private static Vec3 calculateMovementVector(double forward, double yaw) {
        double yawRad = Math.toRadians(yaw);
        double x = -Math.sin(yawRad) * forward;
        double z = Math.cos(yawRad) * forward;
        return new Vec3(x, 0, z);
    }

    private static boolean hasGroundSupport(AbstractSiegeEntity abstractSiegeEntity) {
        if (abstractSiegeEntity.onGround()) {
            return true;
        }

        Level level = abstractSiegeEntity.level();
        AABB box = abstractSiegeEntity.getBoundingBox();
        double y = box.minY - 0.06D;
        double inset = Math.min(0.5D, Math.min(box.getXsize(), box.getZsize()) * 0.2D);
        double minX = box.minX + inset;
        double maxX = box.maxX - inset;
        double minZ = box.minZ + inset;
        double maxZ = box.maxZ - inset;
        return hasSolidTop(level, minX, y, minZ)
                || hasSolidTop(level, minX, y, maxZ)
                || hasSolidTop(level, maxX, y, minZ)
                || hasSolidTop(level, maxX, y, maxZ)
                || hasSolidTop(level, abstractSiegeEntity.getX(), y, abstractSiegeEntity.getZ());
    }

    private static boolean hasSolidTop(Level level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    public static void updateWheelRotation(AbstractSiegeEntity abstractSiegeEntity) {
        abstractSiegeEntity.updateDifferentialWheelRotation();
        double directionalDistance = abstractSiegeEntity.level().isClientSide
                ? abstractSiegeEntity.updateDriveAnimationAndGetForwardDistance()
                : abstractSiegeEntity.updateVisualTravelAndGetForwardDistance();
        abstractSiegeEntity.wheelRotation += (float) (-directionalDistance * 72);
        abstractSiegeEntity.wheelRotation %= 360f;

        if (abstractSiegeEntity.wheelRotation < 0) {
            abstractSiegeEntity.wheelRotation += 360f;
        }
    }
}
