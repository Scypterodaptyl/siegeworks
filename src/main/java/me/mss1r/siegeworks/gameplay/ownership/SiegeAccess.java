package me.mss1r.siegeworks.gameplay.ownership;

import me.mss1r.siegeworks.api.SiegeAllianceRegistry;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.event.SiegeAccessCheckEvent;
import me.mss1r.siegeworks.event.SiegeAccessEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** The single place that decides who may do what with a siege engine. */
public final class SiegeAccess {
    public enum Action {
        USE,
        REPAIR,
        DISMANTLE
    }

    private SiegeAccess() {
    }

    public static boolean allows(@Nullable Entity actor, AbstractSiegeEntity siege, Action action) {
        if (siege.level().isClientSide) {
            return true;
        }
        boolean allowed = !SiegeworksServerConfig.isOwnershipEnforced()
                || siege.getOwnerUuid() == null
                || actor instanceof Player player && player.hasPermissions(2)
                || permits(relationOf(actor, siege), action);
        SiegeAccessCheckEvent event = new SiegeAccessCheckEvent(actor, siege, action, allowed);
        SiegeAccessEvents.CHECK.invoker().check(event);
        return event.allowed();
    }

    private static boolean permits(SiegeRelation relation, Action action) {
        return switch (relation) {
            case OWNER -> true;
            case FRIENDLY -> action != Action.DISMANTLE || SiegeworksServerConfig.teammatesMayDismantle();
            case HOSTILE -> false;
        };
    }

    /** Whether the actor is on the side of an owner whose property is outside an engine, e.g. a carried ladder. */
    public static boolean sameSide(Entity actor, @Nullable UUID ownerUuid) {
        return ownerUuid == null
                || !SiegeworksServerConfig.isOwnershipEnforced()
                || actor instanceof Player player && player.hasPermissions(2)
                || relationOf(actor, actor.level(), ownerUuid) != SiegeRelation.HOSTILE;
    }

    /** Uses the actor's own team, which recruits keep while their commanding player is offline. */
    public static SiegeRelation relationOf(@Nullable Entity actor, AbstractSiegeEntity siege) {
        return relationOf(actor, siege.level(), siege.getOwnerUuid());
    }

    private static SiegeRelation relationOf(@Nullable Entity actor, Level level, @Nullable UUID ownerUuid) {
        Team actorTeam = actor == null ? null : actor.getTeam();
        return relation(level, SiegeOwnership.playerOf(actor),
                actorTeam == null ? null : actorTeam.getName(), ownerUuid);
    }

    public static SiegeRelation relationOf(@Nullable UUID playerUuid, AbstractSiegeEntity siege) {
        return relation(siege.level(), playerUuid, null, siege.getOwnerUuid());
    }

    public static SiegeRelation relation(Level level, @Nullable UUID playerUuid, @Nullable String playerTeamName,
                                         @Nullable UUID ownerUuid) {
        if (playerUuid == null || ownerUuid == null) {
            return SiegeRelation.HOSTILE;
        }
        if (playerUuid.equals(ownerUuid)) {
            return SiegeRelation.OWNER;
        }
        String playerTeam = playerTeamName != null ? playerTeamName : SiegeTeams.teamOf(level, playerUuid);
        String ownerTeam = SiegeTeams.teamOf(level, ownerUuid);
        if (playerTeam == null || ownerTeam == null) {
            return SiegeRelation.HOSTILE;
        }
        return playerTeam.equals(ownerTeam) || SiegeAllianceRegistry.allied(playerTeam, ownerTeam)
                ? SiegeRelation.FRIENDLY
                : SiegeRelation.HOSTILE;
    }
}
