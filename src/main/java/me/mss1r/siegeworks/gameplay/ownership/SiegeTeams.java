package me.mss1r.siegeworks.gameplay.ownership;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SiegeTeams {
    private SiegeTeams() {
    }

    @Nullable
    public static String teamOf(Level level, @Nullable UUID playerUuid) {
        Player player = playerUuid == null ? null : level.getPlayerByUUID(playerUuid);
        if (player != null) {
            Team team = player.getTeam();
            return team == null ? null : team.getName();
        }
        return teamOf(level.getServer(), playerUuid);
    }

    /** True if the player or a teammate is online to defend their property. */
    public static boolean sideOnline(Level level, UUID playerUuid) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return false;
        }
        if (level.getPlayerByUUID(playerUuid) != null || server.getPlayerList().getPlayer(playerUuid) != null) {
            return true;
        }
        String teamName = teamOf(level, playerUuid);
        PlayerTeam team = teamName == null ? null : server.getScoreboard().getPlayerTeam(teamName);
        return team != null && team.getPlayers().stream()
                .anyMatch(name -> server.getPlayerList().getPlayerByName(name) != null);
    }

    /** The player's scoreboard team, looked up through the profile cache when they are offline. */
    @Nullable
    public static String teamOf(@Nullable MinecraftServer server, @Nullable UUID playerUuid) {
        if (server == null || playerUuid == null) {
            return null;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(playerUuid);
        if (online != null) {
            Team team = online.getTeam();
            return team == null ? null : team.getName();
        }
        if (server.getProfileCache() == null) {
            return null;
        }
        return server.getProfileCache().get(playerUuid)
                .map(profile -> server.getScoreboard().getPlayersTeam(profile.getName()))
                .map(Team::getName)
                .orElse(null);
    }
}
