package me.mss1r.siegeworks.gameplay.ownership;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SiegeOperatorReference {
    /** Named before operator and owner were separate; kept so existing saves still load. */
    private static final String TAG_OPERATOR_UUID = "Owner";

    private final AbstractSiegeEntity siege;
    private Entity operator;
    private UUID operatorUuid;

    public SiegeOperatorReference(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    @Nullable
    public Entity get() {
        if (operator != null && !operator.isRemoved()) {
            return operator;
        }
        operator = resolve(operatorUuid);
        return operator;
    }

    @Nullable
    public UUID uuid() {
        return operator != null ? operator.getUUID() : operatorUuid;
    }

    public void set(@Nullable Entity operator) {
        this.operator = operator;
        operatorUuid = operator == null ? null : operator.getUUID();
    }

    public void save(CompoundTag tag) {
        UUID currentOperatorUuid = operator != null ? operator.getUUID() : operatorUuid;
        if (currentOperatorUuid != null) {
            tag.putUUID(TAG_OPERATOR_UUID, currentOperatorUuid);
        }
    }

    public void load(CompoundTag tag) {
        operatorUuid = tag.hasUUID(TAG_OPERATOR_UUID) ? tag.getUUID(TAG_OPERATOR_UUID) : null;
        operator = resolve(operatorUuid);
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
        return operator;
    }
}
