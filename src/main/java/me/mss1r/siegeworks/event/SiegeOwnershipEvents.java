package me.mss1r.siegeworks.event;

import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.api.event.BlueprintUsedEvent;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;

public final class SiegeOwnershipEvents {
    private SiegeOwnershipEvents() {
    }

    public static void register() {
        BlueprintEvents.USED.register(SiegeOwnershipEvents::onBlueprintUsed);
    }

    private static void onBlueprintUsed(BlueprintUsedEvent event) {
        if (event.placed() instanceof AbstractSiegeEntity siege) {
            siege.claimOwnership(event.player().getUUID());
        }
    }
}
