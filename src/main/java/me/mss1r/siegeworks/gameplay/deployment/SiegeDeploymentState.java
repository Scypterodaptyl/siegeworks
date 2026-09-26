package me.mss1r.siegeworks.gameplay.deployment;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SiegeDeploymentState {
    private static final String TAG_OWNER = "DeploymentOwner";
    private static final String TAG_GROUP = "DeploymentGroup";

    private final AbstractSiegeEntity siege;
    private UUID ownerUuid;
    private String groupKey = "";
    private boolean registered;

    public SiegeDeploymentState(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    public void setIdentity(@Nullable UUID ownerUuid, @Nullable String groupKey) {
        this.ownerUuid = ownerUuid;
        this.groupKey = groupKey == null ? "" : groupKey;
        registered = false;
    }

    @Nullable
    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String groupKey() {
        return groupKey;
    }

    public void save(CompoundTag tag) {
        if (ownerUuid != null && !groupKey.isBlank()) {
            tag.putUUID(TAG_OWNER, ownerUuid);
            tag.putString(TAG_GROUP, groupKey);
        }
    }

    public void load(CompoundTag tag) {
        if (tag.hasUUID(TAG_OWNER)) {
            ownerUuid = tag.getUUID(TAG_OWNER);
            groupKey = tag.getString(TAG_GROUP);
        } else {
            ownerUuid = null;
            groupKey = "";
        }
        registered = false;
    }

    public void tick() {
        if (!registered && ownerUuid != null && !groupKey.isBlank()
                && siege.level() instanceof ServerLevel level) {
            SiegeDeploymentLimits.register(level, siege.getUUID(), siege.getType(), groupKey);
            registered = true;
        }
    }

    public void onRemoved(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && siege.level() instanceof ServerLevel level) {
            SiegeDeploymentLimits.unregister(level, siege.getUUID());
            registered = false;
        }
    }
}
