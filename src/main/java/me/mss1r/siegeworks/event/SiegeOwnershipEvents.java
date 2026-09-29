package me.mss1r.siegeworks.event;

import dev.architectury.event.events.common.PlayerEvent;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.api.event.BlueprintUsedEvent;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeOwnerActivity;

public final class SiegeOwnershipEvents {
    private SiegeOwnershipEvents() {
    }

    public static void register() {
        BlueprintEvents.USED.register(SiegeOwnershipEvents::onBlueprintUsed);
        PlayerEvent.PLAYER_JOIN.register(player -> SiegeOwnerActivity.record(player, System.currentTimeMillis()));
        PlayerEvent.PLAYER_QUIT.register(player -> SiegeOwnerActivity.record(player, System.currentTimeMillis()));
    }

    private static void onBlueprintUsed(BlueprintUsedEvent event) {
        if (event.placed() instanceof AbstractSiegeEntity siege) {
            siege.claimOwnership(event.player().getUUID());
        }
    }
}
