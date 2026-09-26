package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ExplosionPhysics {
    private static final Map<ServerLevel, DebrisQuota> DEBRIS_QUOTAS = new WeakHashMap<>();

    private ExplosionPhysics() {
    }

    public static void scatterAffectedBlocks(ServerLevel level, Vec3 center, float radius, List<BlockPos> affectedBlocks) {
        if (radius <= 0.0F || affectedBlocks.isEmpty() || !SiegeworksServerConfig.isFlyingBlockDebrisEnabled()) {
            return;
        }

        for (Iterator<BlockPos> iterator = affectedBlocks.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            if (!canBecomeDebris(level, center, pos)) {
                continue;
            }
            if (!claimDebrisSlot(level)) {
                break;
            }

            BlockState state = level.getBlockState(pos);
            spawnDebris(level, center, radius, pos, state);
            iterator.remove();
        }
    }

    public static boolean launchDestroyedBlock(ServerLevel level, BlockPos pos, Vec3 center, float blastPower) {
        BlockState state = level.getBlockState(pos);
        if (!SiegeworksServerConfig.isFlyingBlockDebrisEnabled()
                || !canBecomeDebris(level, pos, state)
                || !claimDebrisSlot(level)) {
            return false;
        }

        spawnDebris(level, center, Math.max(2.0F, blastPower), pos, state);
        return true;
    }

    private static boolean canBecomeDebris(ServerLevel level, Vec3 center, BlockPos pos) {
        if (pos.getY() < center.y - 0.35D) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return canBecomeDebris(level, pos, state);
    }

    private static boolean canBecomeDebris(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        if (state.getBlock() instanceof TntBlock || level.getBlockEntity(pos) != null) {
            return false;
        }

        return !state.getFluidState().isSource();
    }

    private static void spawnDebris(ServerLevel level, Vec3 center, float blastPower, BlockPos pos, BlockState state) {
        FallingBlockEntity debris = FallingBlockEntity.fall(level, pos, state);
        debris.dropItem = false;
        debris.setHurtsEntities(Math.max(1.0F, blastPower * 0.45F), Math.max(3, Mth.ceil(blastPower * 2.0F)));
        debris.setDeltaMovement(calculateMotion(level, center, pos, blastPower));
        debris.hasImpulse = true;
        debris.hurtMarked = true;
    }

    private static Vec3 calculateMotion(ServerLevel level, Vec3 center, BlockPos pos, float radius) {
        Vec3 offset = Vec3.atCenterOf(pos).subtract(center);
        Vec3 direction = offset.lengthSqr() < 1.0E-5D
                ? new Vec3(level.random.nextDouble() - 0.5D, 0.35D, level.random.nextDouble() - 0.5D)
                : offset;
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-5D) {
            horizontal = new Vec3(level.random.nextDouble() - 0.5D, 0.0D, level.random.nextDouble() - 0.5D);
        }

        double distance = Math.max(0.25D, offset.length());
        double falloff = Mth.clamp(1.0D - distance / Math.max(1.0D, radius + 2.0D), 0.35D, 1.0D);
        double outward = Mth.clamp(0.85D + radius * 0.18D, 0.85D, 2.25D) * falloff;
        double upward = Mth.clamp(0.55D + radius * 0.1D + level.random.nextDouble() * 0.35D,
                0.55D, 1.65D) * Math.sqrt(falloff);

        return horizontal.normalize().scale(outward).add(0.0D, upward, 0.0D);
    }

    private static boolean claimDebrisSlot(ServerLevel level) {
        long gameTime = level.getGameTime();
        DebrisQuota quota = DEBRIS_QUOTAS.computeIfAbsent(level, ignored -> new DebrisQuota(gameTime));
        if (quota.gameTime != gameTime) {
            quota.gameTime = gameTime;
            quota.used = 0;
        }

        int maximum = SiegeworksServerConfig.getMaxFlyingBlockDebrisPerTick();
        if (quota.used >= maximum) {
            return false;
        }
        quota.used++;
        return true;
    }

    private static final class DebrisQuota {
        private long gameTime;
        private int used;

        private DebrisQuota(long gameTime) {
            this.gameTime = gameTime;
        }
    }
}
