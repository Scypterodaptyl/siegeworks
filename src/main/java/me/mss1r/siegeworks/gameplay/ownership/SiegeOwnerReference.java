package me.mss1r.siegeworks.gameplay.ownership;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SiegeOwnerReference {
    private static final String TAG_OWNER_UUID = "Owner";

    private final AbstractSiegeEntity siege;
    private Entity owner;
    private UUID ownerUuid;

    public SiegeOwnerReference(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    @Nullable
    public Entity get() {
        if (owner != null && !owner.isRemoved()) {
            return owner;
        }
        owner = resolve(ownerUuid);
        return owner;
    }

    public void set(@Nullable Entity owner) {
        this.owner = owner;
        ownerUuid = owner == null ? null : owner.getUUID();
    }

    public void save(CompoundTag tag) {
        UUID currentOwnerUuid = owner != null ? owner.getUUID() : ownerUuid;
        if (currentOwnerUuid != null) {
            tag.putUUID(TAG_OWNER_UUID, currentOwnerUuid);
        }
    }

    public void load(CompoundTag tag) {
        ownerUuid = tag.hasUUID(TAG_OWNER_UUID) ? tag.getUUID(TAG_OWNER_UUID) : null;
        owner = resolve(ownerUuid);
    }

    @Nullable
    private Entity resolve(@Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (uuid.equals(siege.getUUID())) {
            return siege;
        }
        if (siege.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(uuid);
        }
        return owner;
    }
}
