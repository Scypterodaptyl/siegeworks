package me.mss1r.siegeworks.api;

import net.minecraft.world.entity.LivingEntity;

/** Passenger exit hooks for siege transports such as the siege tower. */
public interface SiegeTransportControl extends SiegeDeployableControl {
    /** Starts or validates the exit sequence before dismounting. */
    boolean preparePassengerForAutomatedExit(LivingEntity passenger);

    boolean canPassengerExit(LivingEntity passenger);

    boolean disembarkPassenger(LivingEntity passenger);

    default boolean disembarkPassengerToGround(LivingEntity passenger, boolean force) {
        return false;
    }

    /** Continues an exit that has already started. */
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
