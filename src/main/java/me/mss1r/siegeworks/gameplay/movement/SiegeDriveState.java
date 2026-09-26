package me.mss1r.siegeworks.gameplay.movement;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.util.Mth;

public final class SiegeDriveState {
    private float forwardInput;
    private float steeringInput;
    private boolean debugAutoDriving;
    private boolean visualTravelInitialized;
    private double lastVisualTravelX;
    private double lastVisualTravelZ;
    private double visualHorizontalTravelSpeed;
    private double measuredHorizontalTravelSpeed;
    private boolean differentialWheelYawInitialized;
    private float lastDifferentialWheelYaw;
    private float differentialWheelRotation;

    public void setInput(float forward, float steering) {
        setForwardInput(forward);
        steeringInput = Mth.clamp(steering, -1.0F, 1.0F);
    }

    public void setForwardInput(float forward) {
        forwardInput = Mth.clamp(forward, -1.0F, 1.0F);
    }

    public float forwardInput() {
        return forwardInput;
    }

    public float steeringInput() {
        return steeringInput;
    }

    public boolean toggleDebugAutoDriving() {
        debugAutoDriving = !debugAutoDriving;
        if (!debugAutoDriving) {
            setInput(0.0F, 0.0F);
        }
        return debugAutoDriving;
    }

    public void setDebugAutoDriving(boolean enabled) {
        debugAutoDriving = enabled;
        if (!enabled) {
            forwardInput = 0.0F;
            steeringInput = 0.0F;
        }
    }

    public boolean debugAutoDriving() {
        return debugAutoDriving;
    }

    public float leftWheelRotation(float wheelRotation) {
        return normalizeWheelRotation(wheelRotation + differentialWheelRotation);
    }

    public float rightWheelRotation(float wheelRotation) {
        return normalizeWheelRotation(wheelRotation - differentialWheelRotation);
    }

    public void updateDifferentialWheelRotation(AbstractSiegeEntity siege) {
        if (!siege.level().isClientSide || !siege.hasDifferentialDrive()) {
            return;
        }

        float currentYaw = siege.getVisualRotationYInDegrees();
        if (!differentialWheelYawInitialized) {
            differentialWheelYawInitialized = true;
            lastDifferentialWheelYaw = currentYaw;
            differentialWheelRotation = 0.0F;
            return;
        }

        float yawDelta = Mth.wrapDegrees(currentYaw - lastDifferentialWheelYaw);
        lastDifferentialWheelYaw = currentYaw;
        if (Math.abs(yawDelta) > 45.0F) {
            return;
        }

        double halfTrackWidth = siege.getBbWidth() * 0.5D;
        double wheelTravel = Math.toRadians(yawDelta) * halfTrackWidth;
        differentialWheelRotation = normalizeWheelRotation(
                differentialWheelRotation - (float) (wheelTravel * 72.0D));
    }

    public double updateVisualTravelAndGetForwardDistance(AbstractSiegeEntity siege) {
        double forwardDistance = measureForwardTravel(siege);
        visualHorizontalTravelSpeed = measuredHorizontalTravelSpeed;
        return forwardDistance;
    }

    private double measureForwardTravel(AbstractSiegeEntity siege) {
        if (!visualTravelInitialized) {
            visualTravelInitialized = true;
            lastVisualTravelX = siege.getX();
            lastVisualTravelZ = siege.getZ();
            measuredHorizontalTravelSpeed = 0.0D;
            return 0.0D;
        }

        double dx = siege.getX() - lastVisualTravelX;
        double dz = siege.getZ() - lastVisualTravelZ;
        lastVisualTravelX = siege.getX();
        lastVisualTravelZ = siege.getZ();

        double distanceSqr = dx * dx + dz * dz;
        if (distanceSqr <= 1.0E-8D || distanceSqr >= 1.0D) {
            measuredHorizontalTravelSpeed = 0.0D;
            return 0.0D;
        }

        measuredHorizontalTravelSpeed = Math.sqrt(distanceSqr);
        float yawRadians = (float) Math.toRadians(siege.getVisualRotationYInDegrees());
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        return dx * forwardX + dz * forwardZ;
    }

    public double updateDriveAnimationAndGetForwardDistance(AbstractSiegeEntity siege) {
        double measuredForward = measureForwardTravel(siege);

        double targetSpeed = siege.getCurrentDriveSpeed();
        double targetMagnitude = Math.abs(targetSpeed);
        boolean driveSilent = targetMagnitude < 1.0E-5D;
        if (driveSilent && measuredHorizontalTravelSpeed > 1.0E-5D) {
            targetMagnitude = measuredHorizontalTravelSpeed;
        }

        visualHorizontalTravelSpeed += (targetMagnitude - visualHorizontalTravelSpeed) * 0.35D;
        if (visualHorizontalTravelSpeed < 1.0E-5D) {
            visualHorizontalTravelSpeed = 0.0D;
        }

        if (driveSilent) {
            return measuredForward < 0.0D ? -visualHorizontalTravelSpeed : visualHorizontalTravelSpeed;
        }
        if (targetSpeed < 0.0D || targetSpeed == 0.0D && forwardInput < 0.0F) {
            return -visualHorizontalTravelSpeed;
        }
        return visualHorizontalTravelSpeed;
    }

    public double visualHorizontalTravelSpeed() {
        return visualHorizontalTravelSpeed;
    }

    private static float normalizeWheelRotation(float rotation) {
        float normalized = rotation % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }
}
