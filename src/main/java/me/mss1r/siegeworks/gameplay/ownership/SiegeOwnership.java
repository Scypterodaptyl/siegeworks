package me.mss1r.siegeworks.gameplay.ownership;

import me.mss1r.siegeworks.api.SiegePlayerAttributionRegistry;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** The player an engine belongs to, stored by UUID so it persists while they're offline. */
public final class SiegeOwnership {
    private static final String TAG_OWNER = "SiegeOwner";

    @Nullable
    private UUID ownerUuid;

    @Nullable
    public UUID ownerUuid() {
        return ownerUuid;
    }

    public void set(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public boolean claim(@Nullable UUID claimant) {
        if (ownerUuid != null || claimant == null) {
            return false;
        }
        ownerUuid = claimant;
        return true;
    }

    public boolean isOwnedBy(@Nullable UUID playerUuid) {
        return ownerUuid != null && ownerUuid.equals(playerUuid);
    }

    public void save(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID(TAG_OWNER, ownerUuid);
        }
    }

    /** Returns false for engines saved before ownership existed, which the caller migrates. */
    public boolean load(CompoundTag tag) {
        ownerUuid = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        return tag.contains(TAG_OWNER);
    }

    /** The player behind an entity: the player itself, or whoever commands it. */
    @Nullable
    public static UUID playerOf(@Nullable Entity entity) {
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        return entity == null ? null : SiegePlayerAttributionRegistry.playerOwnerOf(entity);
    }

    /** Like {@link #playerOf}, but automation acting as a fake player never takes an engine. */
    @Nullable
    public static UUID claimantOf(@Nullable Entity entity) {
        return entity instanceof Player player && MinecraftVersionCompat.isFakePlayer(player)
                ? null
                : playerOf(entity);
    }

    public static boolean isKnownPlayer(@Nullable MinecraftServer server, @Nullable UUID uuid) {
        if (server == null || uuid == null) {
            return false;
        }
        return server.getPlayerList().getPlayer(uuid) != null
                || server.getProfileCache() != null && server.getProfileCache().get(uuid).isPresent();
    }
}
