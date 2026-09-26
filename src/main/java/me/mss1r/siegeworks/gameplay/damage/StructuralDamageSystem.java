package me.mss1r.siegeworks.gameplay.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class StructuralDamageSystem {
    private static final int OVERLAY_REFRESH_TICKS = 12;
    private static final int DAMAGE_TTL_TICKS = 20 * 35;
    private static final Map<MinecraftServer, ServerDamageState> SERVERS = new WeakHashMap<>();

    private StructuralDamageSystem() {
    }

    public static ImpactResult applyImpact(ServerLevel level, BlockPos pos,
                                           float impact, float hardness) {
        BlockState blockState = level.getBlockState(pos);
        if (impact <= 0.0F || !Float.isFinite(impact) || hardness < 0.0F || blockState.isAir()) {
            return ImpactResult.IGNORED;
        }

        Map<BlockPos, DamageEntry> entries = stateFor(level).entries(level.dimension());
        BlockPos key = pos.immutable();
        DamageEntry previous = entries.get(key);
        float previousProgress = previous != null && previous.matches(blockState)
                ? previous.progress()
                : 0.0F;
        float progress = hardness == 0.0F
                ? 1.0F
                : previousProgress + impact / hardness;

        if (progress >= 1.0F) {
            entries.remove(key);
            BlockCrackOverlay.clear(level, key);
            return ImpactResult.BREAK_BLOCK;
        }

        entries.put(key, new DamageEntry(blockState, progress, 0, 0));
        BlockCrackOverlay.show(level, key, progress);
        return ImpactResult.ACCUMULATED;
    }

    public static void tick(ServerLevel level) {
        ServerDamageState serverState = SERVERS.get(level.getServer());
        if (serverState == null) {
            return;
        }

        Map<BlockPos, DamageEntry> entries = serverState.forDimension(level.dimension());
        if (entries == null) {
            return;
        }

        Iterator<Map.Entry<BlockPos, DamageEntry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, DamageEntry> tracked = iterator.next();
            BlockPos pos = tracked.getKey();
            DamageEntry current = tracked.getValue();
            BlockState blockState = level.getBlockState(pos);

            if (!current.matches(blockState) || current.ageTicks() >= DAMAGE_TTL_TICKS) {
                BlockCrackOverlay.clear(level, pos);
                iterator.remove();
                continue;
            }

            DamageEntry aged = current.age();
            if (aged.overlayRefreshTicks() >= OVERLAY_REFRESH_TICKS) {
                BlockCrackOverlay.show(level, pos, aged.progress());
                aged = aged.afterOverlayRefresh();
            }
            tracked.setValue(aged);
        }

        if (entries.isEmpty()) {
            serverState.remove(level.dimension());
            if (serverState.isEmpty()) {
                SERVERS.remove(level.getServer());
            }
        }
    }

    private static ServerDamageState stateFor(ServerLevel level) {
        return SERVERS.computeIfAbsent(level.getServer(), ignored -> new ServerDamageState());
    }

    public enum ImpactResult {
        IGNORED,
        ACCUMULATED,
        BREAK_BLOCK
    }

    private record DamageEntry(BlockState blockState, float progress,
                               int ageTicks, int overlayRefreshTicks) {
        boolean matches(BlockState current) {
            return !current.isAir() && blockState.equals(current);
        }

        DamageEntry age() {
            return new DamageEntry(blockState, progress, ageTicks + 1, overlayRefreshTicks + 1);
        }

        DamageEntry afterOverlayRefresh() {
            return new DamageEntry(blockState, progress, ageTicks, 0);
        }
    }

    private static final class ServerDamageState {
        private final Map<ResourceKey<Level>, Map<BlockPos, DamageEntry>> dimensions = new HashMap<>();

        Map<BlockPos, DamageEntry> entries(ResourceKey<Level> dimension) {
            return dimensions.computeIfAbsent(dimension, ignored -> new HashMap<>());
        }

        Map<BlockPos, DamageEntry> forDimension(ResourceKey<Level> dimension) {
            return dimensions.get(dimension);
        }

        void remove(ResourceKey<Level> dimension) {
            dimensions.remove(dimension);
        }

        boolean isEmpty() {
            return dimensions.isEmpty();
        }
    }
}
