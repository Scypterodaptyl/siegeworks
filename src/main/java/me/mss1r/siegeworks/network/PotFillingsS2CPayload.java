package me.mss1r.siegeworks.network;

import com.mojang.serialization.Codec;
import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.data.profile.PotFillingProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if neoforge {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.Map;

/** Server pot fillings, so client tooltips and pot interactions use the same ingredients. */
//? if forge {
/*
public record PotFillingsS2CPayload(Map<ResourceLocation, PotFillingProfile> profiles) {
*///?} else {
public record PotFillingsS2CPayload(Map<ResourceLocation, PotFillingProfile> profiles)
        implements CustomPacketPayload {
    public static final Type<PotFillingsS2CPayload> TYPE = new Type<>(
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "pot_fillings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PotFillingsS2CPayload> STREAM_CODEC =
            StreamCodec.ofMember(PotFillingsS2CPayload::write, PotFillingsS2CPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<PotFillingsS2CPayload> type() { return TYPE; }
    //?}
    private static final Codec<Map<ResourceLocation, PotFillingProfile>> PROFILES =
            Codec.unboundedMap(ResourceLocation.CODEC, PotFillingProfile.CODEC);

    public static PotFillingsS2CPayload current() {
        return new PotFillingsS2CPayload(SiegeProfileCatalogs.POT_FILLINGS.snapshot());
    }

    public static void encode(PotFillingsS2CPayload packet, FriendlyByteBuf buffer) {
        buffer.writeNbt((CompoundTag) PROFILES.encodeStart(NbtOps.INSTANCE, packet.profiles).result()
                .orElseThrow(() -> new IllegalStateException("Pot fillings could not be encoded")));
    }

    public static PotFillingsS2CPayload decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        Map<ResourceLocation, PotFillingProfile> profiles = tag == null
                ? Map.of()
                : PROFILES.parse(NbtOps.INSTANCE, tag).result().orElse(Map.of());
        return new PotFillingsS2CPayload(profiles);
    }

    public static void handle(PotFillingsS2CPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> SiegeProfileCatalogs.POT_FILLINGS.acceptFromServer(packet.profiles));
    }
}
