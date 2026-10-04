package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.entity.siege.MangonelEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

//? if forge {
/*public record MangonelFireC2SPayload() {
*///?} else {
public record MangonelFireC2SPayload() implements CustomPacketPayload {
    public static final Type<MangonelFireC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "mangonel_fire"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MangonelFireC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(MangonelFireC2SPayload::write, MangonelFireC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<MangonelFireC2SPayload> type() { return TYPE; }
    //?}
    public static final MangonelFireC2SPayload INSTANCE = new MangonelFireC2SPayload();

    public static void encode(MangonelFireC2SPayload packet, FriendlyByteBuf buffer) {
    }

    public static MangonelFireC2SPayload decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(MangonelFireC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null && player.getVehicle() instanceof MangonelEntity mangonel) {
                mangonel.requestRiderFire(player);
            }
        });
    }
}
