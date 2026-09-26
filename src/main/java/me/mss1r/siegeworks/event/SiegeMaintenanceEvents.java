package me.mss1r.siegeworks.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public final class SiegeMaintenanceEvents {
    public static final Event<Check> CHECK = EventFactory.createLoop();
    public static final Event<Completed> COMPLETED = EventFactory.createLoop();

    private SiegeMaintenanceEvents() {
    }

    @FunctionalInterface
    public interface Check {
        void check(SiegeMaintenanceCheckEvent event);
    }

    @FunctionalInterface
    public interface Completed {
        void completed(SiegeMaintenanceCompletedEvent event);
    }
}
