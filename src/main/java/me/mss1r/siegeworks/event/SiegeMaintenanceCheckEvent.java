package me.mss1r.siegeworks.event;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.server.level.ServerPlayer;

public final class SiegeMaintenanceCheckEvent {
    public enum Action {
        REPAIR,
        START_DISMANTLE,
        CANCEL_DISMANTLE,
        DISMANTLE_HIT
    }

    private final ServerPlayer player;
    private final AbstractSiegeEntity siege;
    private final Action action;
    private boolean allowed = true;

    public SiegeMaintenanceCheckEvent(ServerPlayer player, AbstractSiegeEntity siege, Action action) {
        this.player = player;
        this.siege = siege;
        this.action = action;
    }

    public ServerPlayer player() {
        return player;
    }

    public AbstractSiegeEntity siege() {
        return siege;
    }

    public Action action() {
        return action;
    }

    public boolean allowed() {
        return allowed;
    }

    public void deny() {
        allowed = false;
    }
}
