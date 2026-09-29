package me.mss1r.siegeworks.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public final class SiegeAccessEvents {
    public static final Event<Check> CHECK = EventFactory.createLoop();

    private SiegeAccessEvents() {
    }

    @FunctionalInterface
    public interface Check {
        void check(SiegeAccessCheckEvent event);
    }
}
