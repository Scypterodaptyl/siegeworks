package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Flies on shots that have gone past the simulation distance, where the server stops moving entities, through
 * the chunks still loaded beyond it, so a shot at a wall that far off gets there instead of hanging in the air.
 * A shot that leaves the loaded world is gone: nobody is there to see it land.
 */
public final class DistantFlight {
    private static final Set<SiegeProjectile> SHOTS = Collections.newSetFromMap(new WeakHashMap<>());

    private DistantFlight() {
    }

    public static void track(SiegeProjectile shot) {
        SHOTS.add(shot);
    }

    public static void tick(MinecraftServer server) {
        if (SHOTS.isEmpty()) {
            return;
        }
        for (SiegeProjectile shot : List.copyOf(SHOTS)) {
            if (!shot.isInFlight() || !(shot.level() instanceof ServerLevel level) || level.getServer() != server) {
                SHOTS.remove(shot);
                continue;
            }
            long now = level.getGameTime();
            if (shot.flewOn(now)) {
                continue;
            }
            if (!shot.flewOn(now - 1L)) {
                SHOTS.remove(shot);
                continue;
            }
            if (level.hasChunkAt(shot.blockPosition())) {
                level.guardEntityTick(level::tickNonPassenger, shot);
            }
            if (!shot.isRemoved() && !level.hasChunkAt(shot.blockPosition())) {
                shot.discard();
            }
        }
    }
}
