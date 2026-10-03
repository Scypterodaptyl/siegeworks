package me.mss1r.siegeworks.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.GameInstance;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Keeps every client's projectile profiles equal to the server's: on joining and after each reload. */
public final class ProjectileProfileSync {
    private ProjectileProfileSync() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(player ->
                SiegeworksNetworking.sendToPlayer(player, ProjectileProfilesS2CPayload.current()));
        SiegeProfileCatalogs.PROJECTILES.onPublish(ProjectileProfileSync::sendToEveryone);
    }

    private static void sendToEveryone() {
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }
        ProjectileProfilesS2CPayload payload = ProjectileProfilesS2CPayload.current();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SiegeworksNetworking.sendToPlayer(player, payload);
        }
    }
}
