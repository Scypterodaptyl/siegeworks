package me.mss1r.siegeworks.api;

import net.minecraft.world.entity.LivingEntity;

/** A siege engine with a deployed and travelling state. */
public interface SiegeDeployableControl extends SiegeEngineControl {
    boolean isDeployed();

    void setDeployed(LivingEntity operator, boolean deployed);

    /** Lets AI skip deployment when the current position is unusable. */
    default boolean worthDeployingHere() {
        return true;
    }

    default double getAutomatedDeploymentDistance() {
        return 3.0D;
    }
}
