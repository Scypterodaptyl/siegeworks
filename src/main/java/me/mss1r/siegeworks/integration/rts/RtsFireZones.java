package me.mss1r.siegeworks.integration.rts;

import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.api.MapOrderListener;
import me.mss1r.recruitsrtscommand.api.RecruitsRTSCommandApi;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class RtsFireZones {
    private RtsFireZones() {
    }

    static void register() {
        RecruitsRTSCommandApi.registerOrderListener(new MapOrderListener() {
            @Override
            public void areaFireOrdered(ServerPlayer commander, List<UUID> members,
                                        MapOrderListener.MapFireZone zone) {
                if (members.isEmpty() || zone.center() == null) return;

                Set<UUID> named = new HashSet<>(members);
                RecruitsCompat.applyFireZone(commander, zone.center(),
                        Math.max(1, zone.radiusX()), Math.max(1, zone.radiusZ()),
                        zone.shape() == FireZoneShape.RECTANGLE,
                        recruit -> named.contains(recruit.getUUID())
                                && RecruitsCompat.commandable(commander, recruit));
            }

            @Override
            public void areaFireCleared(ServerPlayer commander, List<UUID> members) {
                if (members.isEmpty()) return;

                Set<UUID> named = new HashSet<>(members);
                RecruitsCompat.clearFireZone(commander,
                        recruit -> named.contains(recruit.getUUID())
                                && RecruitsCompat.commandable(commander, recruit));
            }
        });
    }
}
