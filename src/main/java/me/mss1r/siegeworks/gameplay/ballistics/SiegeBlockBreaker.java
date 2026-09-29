package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.api.SiegePlayerAttributionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SiegeBlockBreaker {
    private static final int BLOCK_BREAK_EFFECT = 2001;

    private SiegeBlockBreaker() {
    }

    public static boolean breakBlock(ServerLevel level, BlockPos pos, @Nullable Player breaker) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (breaker == null) {
            return level.destroyBlock(pos, false);
        }

        boolean removed = state.onDestroyedByPlayer(level, pos, breaker, false, level.getFluidState(pos));
        //? if neoforge {
        if (removed) {
            level.levelEvent(BLOCK_BREAK_EFFECT, pos, Block.getId(state));
        }
        //?}
        return removed;
    }

    public static float siegeResistance(Level level, BlockPos pos, BlockState state, Explosion probe) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) {
            return hardness;
        }
        return Math.min(hardness, Math.max(0.0F, state.getExplosionResistance(level, pos, probe)));
    }

    public static Explosion damageProbe(Level level, Vec3 center, @Nullable Entity source) {
        return new Explosion(level, source, center.x, center.y, center.z, 0.0F, false,
                Explosion.BlockInteraction.KEEP);
    }

    @Nullable
    public static Player responsiblePlayer(@Nullable Entity owner) {
        return responsiblePlayer(owner, new HashSet<>());
    }

    @Nullable
    private static Player responsiblePlayer(@Nullable Entity owner, Set<UUID> visited) {
        if (owner == null || !visited.add(owner.getUUID())) {
            return null;
        }
        if (owner instanceof Player player) {
            return player;
        }
        if (owner instanceof AbstractSiegeEntity siege) {
            if (siege.getControllingPassenger() instanceof Player crew) {
                return crew;
            }
            Entity machineOwner = siege.getOperator();
            if (machineOwner != null && machineOwner != siege) {
                Player attributed = responsiblePlayer(machineOwner, visited);
                if (attributed != null) {
                    return attributed;
                }
            }
            UUID engineOwner = siege.getOwnerUuid();
            return engineOwner == null || siege.level().getServer() == null
                    ? null
                    : siege.level().getServer().getPlayerList().getPlayer(engineOwner);
        }
        UUID playerOwner = SiegePlayerAttributionRegistry.playerOwnerOf(owner);
        return playerOwner == null || owner.level().getServer() == null
                ? null
                : owner.level().getServer().getPlayerList().getPlayer(playerOwner);
    }
}
