package me.mss1r.siegeworks.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.utils.GameInstance;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** Syncs projectile profiles and pot fillings to clients on join and after each reload. */
public final class ProfileSync {
    private ProfileSync() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            SiegeworksNetworking.sendToPlayer(player, ProjectileProfilesS2CPayload.current());
            SiegeworksNetworking.sendToPlayer(player, PotFillingsS2CPayload.current());
        });
        SiegeProfileCatalogs.PROJECTILES.onPublish(() -> sendToEveryone(ProjectileProfilesS2CPayload::current));
        SiegeProfileCatalogs.POT_FILLINGS.onPublish(() -> sendToEveryone(PotFillingsS2CPayload::current));
    }

    private static void sendToEveryone(Supplier<?> payload) {
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }
        Object packet = payload.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SiegeworksNetworking.sendToPlayer(player, packet);
        }
    }
}
