package me.mss1r.siegeworks.api;

import net.minecraft.world.phys.Vec3;

/** Extra controls needed to aim and fire a siege engine through AI. */
public interface SiegeArtilleryControl extends SiegeEngineControl {
    Vec3 getAutomatedAimOrigin();

    float calculateAutomatedAimPitch(Vec3 target);

    /** Allows weapons such as the hwacha to offset individual shots. */
    default Vec3 resolveAutomatedAimTarget(Vec3 commandedTarget, int shotSequence) {
        return commandedTarget;
    }

    /** Radius advertised to fire-zone targeting code. */
    default double getAutomatedTargetSpreadRadius() {
        return 0.0D;
    }

    /** Updates weapon-specific settings, such as throw power, before aiming. */
    default void prepareAutomatedShot(Vec3 target) {
    }

    default boolean canReachAutomatedTarget(Vec3 target) {
        return true;
    }

    default boolean isReadyToFire() {
        return hasAmmoLoaded() && getWindingTime() <= 0 && getCooldown() <= 0;
    }
}
