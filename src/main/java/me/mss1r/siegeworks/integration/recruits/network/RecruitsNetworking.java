package me.mss1r.siegeworks.integration.recruits.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class RecruitsNetworking {
    private static final ResourceLocation SIEGE_COMMAND = id("recruits_siege_command");
    private static final ResourceLocation TOWER_CREW = id("recruits_tower_crew");
    private static final ResourceLocation FIRE_ZONE = id("recruits_fire_zone");

    private RecruitsNetworking() {
    }

    public static void register() {
        register(SIEGE_COMMAND,
                RecruitsSiegeCommandC2SPayload::decode, RecruitsSiegeCommandC2SPayload::handle);
        register(TOWER_CREW,
                RecruitsTowerCrewC2SPayload::decode, RecruitsTowerCrewC2SPayload::handle);
        register(FIRE_ZONE,
                RecruitsFireZoneC2SPayload::decode, RecruitsFireZoneC2SPayload::handle);
    }

    public static void sendToServer(RecruitsSiegeCommandC2SPayload packet) {
        sendToServer(SIEGE_COMMAND, packet, RecruitsSiegeCommandC2SPayload::encode);
    }

    public static void sendToServer(RecruitsTowerCrewC2SPayload packet) {
        sendToServer(TOWER_CREW, packet, RecruitsTowerCrewC2SPayload::encode);
    }

    public static void sendToServer(RecruitsFireZoneC2SPayload packet) {
        sendToServer(FIRE_ZONE, packet, RecruitsFireZoneC2SPayload::encode);
    }

    private static <T> void register(ResourceLocation id,
                                     Function<FriendlyByteBuf, T> decoder,
                                     BiConsumer<T, NetworkManager.PacketContext> handler) {
        NetworkManager.registerReceiver(NetworkManager.c2s(), id,
                (buffer, context) -> handler.accept(decoder.apply(buffer), context));
    }

    private static <T> void sendToServer(ResourceLocation id, T packet,
                                         BiConsumer<T, FriendlyByteBuf> encoder) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        encoder.accept(packet, buffer);
        NetworkManager.sendToServer(id, buffer);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Siegeworks.MOD_ID, path);
    }
}
