package me.mss1r.siegeworks.api;

import net.minecraft.world.entity.LivingEntity;

public interface SiegeTransportControl extends SiegeDeployableControl {
    boolean preparePassengerForAutomatedExit(LivingEntity passenger);

    boolean canPassengerExit(LivingEntity passenger);

    boolean disembarkPassenger(LivingEntity passenger);

    default boolean disembarkPassengerToGround(LivingEntity passenger, boolean force) {
        return false;
    }

    default ExitResult advanceAutomatedExit(LivingEntity passenger) {
        return ExitResult.COMPLETE;
    }

    default void cancelAutomatedExit(LivingEntity passenger) {
    }

    enum ExitResult {
        IN_PROGRESS,
        COMPLETE,
        FAILED
    }
}
