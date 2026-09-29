package me.mss1r.siegeworks.event;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class SiegeAccessCheckEvent {
    @Nullable
    private final Entity actor;
    private final AbstractSiegeEntity siege;
    private final SiegeAccess.Action action;
    private boolean allowed;

    public SiegeAccessCheckEvent(@Nullable Entity actor, AbstractSiegeEntity siege,
                                 SiegeAccess.Action action, boolean allowed) {
        this.actor = actor;
        this.siege = siege;
        this.action = action;
        this.allowed = allowed;
    }

    @Nullable
    public Entity actor() {
        return actor;
    }

    public AbstractSiegeEntity siege() {
        return siege;
    }

    public SiegeAccess.Action action() {
        return action;
    }

    public boolean allowed() {
        return allowed;
    }

    public void allow() {
        allowed = true;
    }

    public void deny() {
        allowed = false;
    }
}
