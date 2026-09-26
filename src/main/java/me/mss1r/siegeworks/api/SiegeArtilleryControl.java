package me.mss1r.siegeworks.api;

import net.minecraft.world.phys.Vec3;

public interface SiegeArtilleryControl extends SiegeEngineControl {
    Vec3 getAutomatedAimOrigin();

    float calculateAutomatedAimPitch(Vec3 target);

    default Vec3 resolveAutomatedAimTarget(Vec3 commandedTarget, int shotSequence) {
        return commandedTarget;
    }

    default double getAutomatedTargetSpreadRadius() {
        return 0.0D;
    }

    default void prepareAutomatedShot(Vec3 target) {
    }

    default boolean canReachAutomatedTarget(Vec3 target) {
        return true;
    }

    default boolean isReadyToFire() {
        return hasAmmoLoaded() && getWindingTime() <= 0 && getCooldown() <= 0;
    }
}
