package me.mss1r.siegeworks.gameplay.movement;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.util.Mth;

public final class SiegeTransformInterpolator {
    private final AbstractSiegeEntity siege;
    private int steps;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double targetYaw;
    private double targetPitch;

    public SiegeTransformInterpolator(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    public void queue(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        targetX = x;
        targetY = y;
        targetZ = z;
        targetYaw = yaw;
        targetPitch = pitch;
        steps = Math.max(1, interpolationSteps);
    }

    public void tick(boolean rotationHandledElsewhere) {
        if (!siege.level().isClientSide || steps <= 0) {
            return;
        }

        double x = siege.getX() + (targetX - siege.getX()) / steps;
        double y = siege.getY() + (targetY - siege.getY()) / steps;
        double z = siege.getZ() + (targetZ - siege.getZ()) / steps;
        if (!rotationHandledElsewhere) {
            double yawDelta = Mth.wrapDegrees(targetYaw - siege.getYRot());
            float yaw = (float) (siege.getYRot() + yawDelta / steps);
            float pitch = (float) (siege.getXRot() + (targetPitch - siege.getXRot()) / steps);
            siege.setYRot(yaw);
            siege.setXRot(pitch);
            siege.setYHeadRot(siege.usesIndependentAim() ? siege.getTrackedYaw() : yaw);
            siege.setYBodyRot(yaw);
            siege.lastRiderYaw = siege.usesIndependentAim() ? siege.getTrackedYaw() : yaw;
            siege.lastRiderPitch = pitch;
        }

        steps--;
        siege.setPos(x, y, z);
        if (!rotationHandledElsewhere) {
            siege.commitInterpolatedRotation(siege.getYRot(), siege.getXRot());
        }
    }

    public void reset() {
        steps = 0;
        targetX = siege.getX();
        targetY = siege.getY();
        targetZ = siege.getZ();
        targetYaw = siege.getYRot();
        targetPitch = siege.getXRot();
    }
}
