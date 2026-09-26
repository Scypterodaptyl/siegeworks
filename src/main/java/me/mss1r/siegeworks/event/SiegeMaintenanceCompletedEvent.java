package me.mss1r.siegeworks.event;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.server.level.ServerPlayer;

public final class SiegeMaintenanceCompletedEvent {
    public enum Action {
        REPAIR,
        DISMANTLE
    }

    private final ServerPlayer player;
    private final AbstractSiegeEntity siege;
    private final Action action;
    private final int repairedHealth;

    public SiegeMaintenanceCompletedEvent(ServerPlayer player, AbstractSiegeEntity siege, Action action,
                                          int repairedHealth) {
        this.player = player;
        this.siege = siege;
        this.action = action;
        this.repairedHealth = repairedHealth;
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

    public int repairedHealth() {
        return repairedHealth;
    }
}
