package me.mss1r.siegeworks.gameplay.crew;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SiegePassengerPhysics {
    private SiegePassengerPhysics() {
    }

    public static void updatePassengerState(AbstractSiegeEntity siege, Entity passenger) {
        if (!siege.hasPassenger(passenger)) {
            return;
        }

        boolean controlsRotation = siege.shouldPassengerControlRotation(passenger);
        boolean bodyFollowsSiege = siege.shouldPassengerBodyFollowSiege(passenger);

        if (!controlsRotation) {
            siege.setTrackedPitch(0);
        } else if (passenger instanceof Player && !siege.usesIndependentPitchAim()) {
            siege.setTrackedPitch(passenger.getXRot());
        }
        if (bodyFollowsSiege && passenger instanceof LivingEntity livingPassenger) {
            float passengerYaw = siege.getPassengerVisualYaw(passenger);
            livingPassenger.setYBodyRot(passengerYaw);
            livingPassenger.yBodyRotO = passengerYaw;
        }
        if (passenger instanceof LivingEntity livingPassenger) {
            clampView(siege, passenger, livingPassenger);
        }
        siege.lastRiderPitch = siege.getTrackedPitch();

        if (!(passenger instanceof Player) && passenger.getFirstPassenger() == null) {
            float passengerYaw = siege.getPassengerVisualYaw(passenger);
            passenger.setYRot(passengerYaw);
            passenger.setYHeadRot(passengerYaw);
            passenger.setYBodyRot(passengerYaw);
            passenger.yRotO = passengerYaw;

            if (passenger instanceof LivingEntity livingPassenger) {
                livingPassenger.yHeadRotO = passengerYaw;
                livingPassenger.yBodyRotO = passengerYaw;
            }
        }
    }

    public static void clampView(AbstractSiegeEntity siege, Entity passenger, LivingEntity livingPassenger) {
        float centerYaw = siege.getPassengerVisualYaw(passenger);
        float yawLimit = Mth.clamp(siege.getPassengerViewYawLimit(passenger), 0.0F, 180.0F);
        float relativeYaw = Mth.wrapDegrees(passenger.getYRot() - centerYaw);
        float clampedYaw = centerYaw + Mth.clamp(relativeYaw, -yawLimit, yawLimit);
        if (Math.abs(Mth.wrapDegrees(clampedYaw - passenger.getYRot())) > 1.0E-3F) {
            passenger.setYRot(clampedYaw);
            passenger.yRotO = clampedYaw;
        }

        float relativeHeadYaw = Mth.wrapDegrees(livingPassenger.getYHeadRot() - centerYaw);
        float clampedHeadYaw = centerYaw + Mth.clamp(relativeHeadYaw, -yawLimit, yawLimit);
        if (Math.abs(Mth.wrapDegrees(clampedHeadYaw - livingPassenger.getYHeadRot())) > 1.0E-3F) {
            livingPassenger.setYHeadRot(clampedHeadYaw);
            livingPassenger.yHeadRotO = clampedHeadYaw;
        }

        float pitchLimit = Mth.clamp(siege.getPassengerViewPitchLimit(passenger), 0.0F, 90.0F);
        float clampedPitch = Mth.clamp(passenger.getXRot(), -pitchLimit, pitchLimit);
        if (Math.abs(clampedPitch - passenger.getXRot()) > 1.0E-3F) {
            passenger.setXRot(clampedPitch);
            passenger.xRotO = clampedPitch;
        }
    }

    public static Vec3 rotatedSeatOffset(AbstractSiegeEntity siege, Entity passenger) {
        float yawRadians = (float) Math.toRadians(siege.getPassengerSeatYaw(passenger));
        Vec3 offset = siege.getPassengerOffset(passenger);

        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);

        double offsetX = rightX * offset.x - forwardX * offset.z;
        double offsetZ = rightZ * offset.x - forwardZ * offset.z;
        return new Vec3(offsetX, offset.y, offsetZ);
    }
}
