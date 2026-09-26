package me.mss1r.siegeworks.api;

import net.minecraft.world.entity.LivingEntity;

public interface SiegeDeployableControl extends SiegeEngineControl {
    boolean isDeployed();

    void setDeployed(LivingEntity operator, boolean deployed);

    default boolean worthDeployingHere() {
        return true;
    }

    default double getAutomatedDeploymentDistance() {
        return 3.0D;
    }
}
